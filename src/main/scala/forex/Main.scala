package forex

import boot.config._
import boot.Resources

import org.http4s.server.blaze.BlazeServerBuilder
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import cats.effect._

object Main extends IOApp {

  implicit val logger = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    load[IO]("").flatMap { cfg =>
      Resources.create.use { res =>
        for {
          _      <- Logger[IO].info(s"Loaded config $cfg")
          module <- boot.Module[IO](cfg, res.client, res.rateRef)

          fib <- app.stream.updater
                   .stream(
                     module.programCacheAlg.updateEvery(cfg.oneFrame.maxInvocations),
                     module.programClientAlg,
                     res.rateRef
                   )
                   .compile
                   .drain
                   .start
          _   <- BlazeServerBuilder[IO]
                   .bindHttp(cfg.http.port, cfg.http.host)
                   .withHttpApp(module.httpApp)
                   .serve
                   .compile
                   .drain
          _   <- fib.cancel
        } yield ExitCode.Success
      }
    }
}
