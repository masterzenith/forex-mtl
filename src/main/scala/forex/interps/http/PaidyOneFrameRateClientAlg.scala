package forex
package interps.http

import core.rates.domains.{Pair, Rate}
import core.rates.errors

import org.http4s._
import org.http4s.headers.Accept
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import io.chrisdavenport.log4cats.Logger
import cats.effect.Sync
import cats.data.NonEmptyList

class PaidyOneFrameRateClientAlg[F[_]: Sync: Logger](
    oneFrameUri: Uri,
    client: Client[F],
    token: String
) extends core.rates.Algebra[F]
    with Http4sClientDsl[F] {

  import cats.implicits._
  import protocols._
  import org.http4s.circe._

  override def get(pair: Pair): F[errors.Error Either Rate] =
    for {
      either <- getAll(NonEmptyList.one(pair))
      res     = either.map(_.head)
    } yield res

  override def getAll(pairs: NonEmptyList[Pair]): F[errors.Error Either NonEmptyList[Rate]] =
    getRaw(pairs).map {
      case Right(rateRespList) => rateRespList.map(_.toDomain).sequence
      case Left(err)           => err.toDomain.asLeft
    }

  private def mkRequest(pairs: NonEmptyList[Pair]): Request[F] = {
    val pairStrings = pairs.map { pair => s"${pair.from}${pair.to}" }.toList
    val query       = Query.fromMap(Map("pair" -> pairStrings))
    val uri         = (oneFrameUri / "rates").copy(query = query)
    Request[F](
      uri = uri,
      headers = Headers
        .of(Accept(MediaType.application.json), Header("token", token))
    )
  }

  private def getRaw(
      pairs: NonEmptyList[Pair]
  ): F[ErrorResponse Either NonEmptyList[RateResponse]] = {
    val request = mkRequest(pairs)
    Logger[F].info(s"Invoking to Paidy one frame $request") >>
      client.fetch[ErrorResponse Either NonEmptyList[RateResponse]](request) { response =>
        if (response.status == Status.Ok) {
          response.asJsonDecode[List[RateResponse]].attempt.flatMap {
            case Right(Nil)   => ErrorResponse("Double Pair").asLeft.pure[F]
            case Right(rates) => NonEmptyList.fromListUnsafe(rates).asRight.pure[F]
            case Left(_)      => response.asJsonDecode[ErrorResponse].map(Left(_))
          }
        } else
          Logger[F].error(s"Unexpected error.\nRequest:$request\nResponse:$response") >>
            ErrorResponse("Unexpected error. ").asLeft.pure[F]
      }
  }
}

object PaidyOneFrameRateClientAlg {

  def apply[F[_]: Sync: Logger](
      oneFrameUri: Uri,
      client: Client[F],
      token: String
  ): F[PaidyOneFrameRateClientAlg[F]] =
    F.delay(new PaidyOneFrameRateClientAlg[F](oneFrameUri, client, token))
}
