package forex
package app.http.rates

import core.rates.domains._
import app.programs.rates.errors

import org.http4s.QueryParamDecoder
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import io.circe._

object protocols {

  import io.circe.generic.semiauto._
  import cats.syntax.show._

  final case class RateRequest(from: Currency, to: Currency)
  final case class RateResponse(from: Currency, to: Currency, price: Price, timestamp: Timestamp)

  implicit val currencyEncoder: Encoder[Currency] = Encoder.instance[Currency] { curr =>
    Json.fromString(curr.show)
  }

  implicit val pairEncoder: Encoder[Pair]                    = deriveEncoder[Pair]
  implicit val priceEncoder: Encoder[Price]                  = deriveEncoder[Price]
  implicit val rateEncoder: Encoder[Rate]                    = deriveEncoder[Rate]
  implicit val timeStampEncoder: Encoder[Timestamp]          = deriveEncoder[Timestamp]
  implicit val responseEncoder: Encoder[RateResponse]        = deriveEncoder[RateResponse]
  implicit val responsesEncoder: Encoder[List[RateResponse]] = deriveEncoder[List[RateResponse]]

  private[http] implicit val currencyParam = QueryParamDecoder[String].map { currName =>
    import cats.implicits._
    Currency.fromString(currName).leftMap(errors.toProgramsError)
  }

  object FromParam extends QueryParamDecoderMatcher[errors.Error Either Currency]("from")
  object ToParam   extends QueryParamDecoderMatcher[errors.Error Either Currency]("to")
}
