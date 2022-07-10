package forex
package app.programs.rates

import core.rates.domains.Rate
import core.rates.domains.Pair
import errors.Error

import cats.effect.Sync
import scala.concurrent.duration.FiniteDuration

trait Algebra[F[_]] {

  import scala.concurrent.duration._

  def get(request: protocol.GetRatesRequest): F[Error Either Rate]
  def allRates: fs2.Stream[F, Rate]

  /**
    * How much time to wait between invocation to one frame server given max invocations
    * they preserve?
    * @param maxInvocations maximum served requests allowed for OneFrame server.
    * @return duration to wait for every update
    */
  def updateEvery(maxInvocations: Int): FiniteDuration =
    (60 * 60 * 24.0 / maxInvocations).seconds
}

object Algebra {

  import cats.implicits._

  def apply[F[_]: Sync](rateAlg: core.rates.Algebra[F]): Algebra[F] =
    new Algebra[F] {

      override def get(request: protocol.GetRatesRequest): F[Error Either Rate] =
        for {
          eithPair <- Pair.create(request.from, request.to).pure[F]
          pair     <- Sync[F].fromEither(eithPair.leftMap(errors.toProgramsError))
          rateRes  <- rateAlg.get(pair)
          appRes    = rateRes.leftMap(errors.toProgramsError)
        } yield appRes

      override def allRates: fs2.Stream[F, Rate] = rateAlg.allRates
    }
}
