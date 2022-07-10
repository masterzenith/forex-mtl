package forex.core.rates.domains

import cats.effect.Timer
import cats.Applicative
import java.time._

case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {

  import cats.implicits._
  import scala.concurrent.duration._

  def now[F[_]: Timer: Applicative](zoneId: ZoneId): F[Timestamp] =
    for {
      epoch   <- F.clock.monotonic(MILLISECONDS)
      instant = Instant.ofEpochMilli(epoch)
    } yield Timestamp(OffsetDateTime.ofInstant(instant, zoneId))
}
