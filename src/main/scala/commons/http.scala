package commons

import org.http4s._
import org.http4s.server.middleware._
import io.circe.{Decoder, Encoder}
import cats.effect.Sync
import cats.Applicative
import scala.concurrent.duration.FiniteDuration
import cats.effect.Concurrent
import cats.effect.Timer

package object http {

  import org.http4s.circe._
  import org.http4s.implicits._

  object codecs {
    implicit def deriveEntityEncoder[F[_]: Applicative, A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]
    implicit def deriveEntityDecoder[F[_]: Sync, A: Decoder]: EntityDecoder[F, A] = jsonOf[F, A]
  }

  def setupMiddleware[F[_]: Sync: Concurrent: Timer](routes: HttpRoutes[F], timeout: FiniteDuration) = {
    val middleware: HttpRoutes[F] => HttpRoutes[F] =
      { http: HttpRoutes[F] => AutoSlash(http) } andThen
      { http: HttpRoutes[F] => CORS(http, CORS.DefaultCORSConfig) } andThen
      { http: HttpRoutes[F] => Timeout(timeout)(http) }

    val loggers: HttpApp[F] => HttpApp[F] =
      { http: HttpApp[F] => RequestLogger.httpApp(true, true)(http) } andThen
      { http: HttpApp[F] => ResponseLogger.httpApp(true, true)(http) }

    loggers(middleware(routes).orNotFound)
  }
}
