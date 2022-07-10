package forex
package app
package stream

import programs.rates.Algebra
import core.rates.domains.Pair
import core.rates.domains.Rate
import core.rates.domains.Currency

import io.chrisdavenport.log4cats.Logger
import cats.effect.concurrent.Ref
import cats.effect.Timer
import cats.Functor

package object updater {

  import scala.concurrent.duration._

  def stream[F[_]: Functor: Timer: Logger](
      updateEvery: FiniteDuration,
      clientRateAlg: Algebra[F],
      mapRef: Ref[F, Map[Pair, Rate]]
  ): fs2.Stream[F, Unit] =
    fs2.Stream.eval(Logger[F].info(s"Starting the cache updater. Updating every $updateEvery")) ++
      fs2.Stream
        .every[F](updateEvery)
        .evalMap(_ => Logger[F].info(s"Updating the cache"))
        .flatMap(_ => clientRateAlg.allRates)
        .chunkN(Currency.allCombinationsLength)
        .evalMap { rates =>
          val newMap = rates.map(r => r.pair -> r).toList.toMap
          mapRef.set(newMap)
        }
        .flatMap(_ => fs2.Stream.sleep(updateEvery))
}
