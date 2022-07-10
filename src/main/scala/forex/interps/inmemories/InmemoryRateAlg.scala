package forex.interps.inmemories

import forex.core.rates
import rates.domains._
import rates.errors

import cats.data.NonEmptyList
import cats.effect.concurrent.Ref
import cats.implicits._
import cats.effect.Sync

/**
 * Implementation of [[core.rates.Algebra]] using in-memory lookup data.
 * Easily mis-judged as one of dummy implementation, if paired with another process
 * which updates the cache regularly, this class might be quite handy.
 * @param mapRef atomic reference which contains our cached rates keyed by pair.
**/
class InmemoryRateAlg[F[_]: Sync](mapRef: Ref[F, Map[Pair, Rate]]) extends rates.Algebra[F] {

  override def get(pair: Pair): F[errors.Error Either Rate] =
    getAll(NonEmptyList.one(pair)).map {
      case Right(rates) => rates.head.asRight
      case Left(err)    => err.asLeft
    }

  override def getAll(pairs: NonEmptyList[Pair]): F[errors.Error Either NonEmptyList[Rate]] = {
    if (pairs.forall(p => p.from != p.to)) lookup(pairs)
    else F.pure(errors.Error.DoublePair.asLeft)
  }

  private def lookup(pairs: NonEmptyList[Pair]): F[errors.Error Either NonEmptyList[Rate]] =
    for {
      rateMap <- mapRef.get
      rateList = pairs.toList.flatMap(p => rateMap.get(p))
      ratesOpt = NonEmptyList.fromList(rateList)
      result   = Either.fromOption(
                   ratesOpt,
                   errors.Error.RateLookupFailed("Currency not found in cache")
                 )
    } yield result
}

object InmemoryRateAlg {

  /**
   * @param mapRef a map of pair and its rate wrapped inside a pure atomic mutable reference [[Ref]]
   * mapRef can be shared with other components which might update the state.
   * @return new instance of [[InmemoryRateAlg]] wrapped in a generic effect [[F]].
  **/
  def apply[F[_]: Sync](mapRef: Ref[F, Map[Pair, Rate]]): F[InmemoryRateAlg[F]] =
    F.delay(new InmemoryRateAlg(mapRef))
}
