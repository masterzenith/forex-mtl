package forex.core.rates

import domains.{Pair, Rate}
import cats.data.NonEmptyList

trait Algebra[F[_]] {

  /**
    * @param pair pair of currencies (from, to) which rate to be calculated
    * @return a [[Rate]], which includes currency pairs, rate, and timestamp
    */
  def get(pair: Pair): F[errors.Error Either Rate]

  /**
    * Get batch of rates for currencies requested by the input.
    * Protip: some of the implementations of currency rate calculator might limit the rate requests
    * it serves in a given time window (e.g. 1000 requests per day). Using this interface
    * will only counted as one request.
    * @param pairs list of [[Pair]] which currencies to be calculated
    * @return list of [[Rate]]
    */
  def getAll(pairs: NonEmptyList[Pair]): F[errors.Error Either NonEmptyList[Rate]]

  /**
    * Get all rates for every combinations of supported currencies.
    * Even though this operation is more aggressive than its get pairs counterparts (by returning all [[Currency]]),
    * this operation also might counted as 1 request.
    * The implementation is a non-optimized version and only relies on another get(pairs) method.
    * The implementator of this algebra are advised to override this method.
    * @return stream of all possible combinations of [[Rate]]
    */
  def allRates: fs2.Stream[F, Rate] =
    fs2.Stream
      .eval(getAll(Pair.allCurrencyPairs))
      .filter(_.isRight)
      .map(
        // get is safe here, since it's guaranted right by previous filter operation
        _.right.get.toList
      )
      .flatMap(fs2.Stream.apply)
}
