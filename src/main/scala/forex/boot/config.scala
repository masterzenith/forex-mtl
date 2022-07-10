package forex
package boot

import org.http4s.Uri
import pureconfig.ConfigSource
import cats.effect.Sync
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

object config {

  import commons.configs._
  import pureconfig.generic.auto._

  case class AppConfig(
      http: HttpConfig,
      oneFrame: OneFrameConfig
  )

  case class HttpConfig(host: String, port: Int, timeout: FiniteDuration)

  case class OneFrameConfig(
      uri: Uri,            // uri for one frame server
      maxInvocations: Int, // maximum invocation oneframe server can handle in a day
      token: String        // token key for calling oneframe API
  )

  /**
   * @param path the property path inside the default configuration
  **/
  def load[F[_]: Sync](path: String): F[AppConfig] =
    F.fromTry(Try(ConfigSource.default.at(path).loadOrThrow[AppConfig]))
}
