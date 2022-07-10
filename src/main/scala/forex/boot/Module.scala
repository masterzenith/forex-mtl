package forex
package boot

import config.AppConfig
import app.http.rates.RatesRoutes
import interps.inmemories.InmemoryRateAlg
import interps.http.PaidyOneFrameRateClientAlg
import core.rates.domains.Pair
import core.rates.domains.Rate

import org.http4s.HttpApp
import org.http4s.client.Client
import io.chrisdavenport.log4cats.Logger
import cats.effect._
import cats.effect.concurrent.Ref

/**
 * @param config configuration loaded from `application.conf`.
 * @param programClientAlg [[app.rates.Algebra[F]]] implementation which has capabilities to query `oneFrame` server.
 * @param programCacheAlg [[app.rates.Algebra[F]]] implementation which only get rates from memory.
 * @param httpApp all the routes and middlewares of our proxy http app.
**/
case class Module[F[_]: Sync: Concurrent: Timer: Logger](
    config: AppConfig,
    programClientAlg: app.programs.rates.Algebra[F],
    programCacheAlg: app.programs.rates.Algebra[F],
    httpApp: HttpApp[F]
)

object Module {

  import cats.implicits._

  def apply[F[_]: ContextShift: ConcurrentEffect: Timer: Logger](
      config: AppConfig,
      client: Client[F],
      mapRef: Ref[F, Map[Pair, Rate]]
  ): F[Module[F]] =
    for {
      clientAlg <- PaidyOneFrameRateClientAlg[F](config.oneFrame.uri, client, config.oneFrame.token)
      clientProg = app.programs.rates.Algebra[F](clientAlg)
      inmemAlg  <- InmemoryRateAlg(mapRef)
      inmemProg  = app.programs.rates.Algebra[F](inmemAlg)
      routes     = new RatesRoutes[F](inmemProg).routes
      httpApp    = commons.http.setupMiddleware(routes, config.http.timeout)
    } yield new Module(config, clientProg, inmemProg, httpApp)
}
