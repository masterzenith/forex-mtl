package forex.app
package http.rates

import programs.rates.protocol.GetRatesRequest
import programs.rates.errors
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.HttpRoutes
import io.chrisdavenport.log4cats.Logger
import cats.effect.Sync

class RatesRoutes[F[_]: Sync: Logger](rateAlg: programs.rates.Algebra[F]) extends Http4sDsl[F] {

  import protocols._
  import converters._
  import commons.http.codecs._
  import errors.Error
  import cats.implicits._

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    // GET /rates/(from, to)
    case GET -> Root :? FromParam(fromEith) +& ToParam(toEith) =>
      val result = for {
        from      <- F.fromEither(fromEith)
        to        <- F.fromEither(toEith)
        rateOrErr <- rateAlg.get(GetRatesRequest(from, to))
        rate      <- F.fromEither(rateOrErr)
        res       <- Ok(rate.toResponse)
      } yield res

      result.handleErrorWith {
        case Error.CurrencyNotSupported(curr) =>
          BadRequest(s"Currency ${curr.getOrElse("")} is not supported.")
        case Error.DoublePair =>
          BadRequest(s"Pair must have different currencies")
        case Error.RateLookupFailed(msg) =>
          Logger[F].error(s"Internal error: $msg") >> InternalServerError(s"Rate lookup failed: $msg")
        case err =>
          Logger[F].error("Internal error: ${err.getMessage}") >> InternalServerError()
      }

    // GET /rates - get all supported combinations of rates
    case GET -> Root => Ok(rateAlg.allRates.compile.toList.map(r => r.map(_.toResponse)))
  }

  val routes = Router("/rates" -> httpRoutes)
}
