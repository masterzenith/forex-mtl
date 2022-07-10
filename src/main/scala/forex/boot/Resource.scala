package forex
package boot

import core.rates.domains.Pair
import core.rates.domains.Rate

import cats.effect.ConcurrentEffect
import cats.effect.ContextShift
import cats.effect.Resource
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import cats.effect.concurrent.Ref

case class Resources[F[_]](client: Client[F], rateRef: Ref[F, Map[Pair, Rate]])

object Resources {

  import scala.concurrent.ExecutionContext
  import cats.implicits._

  def create[F[_]: ContextShift: ConcurrentEffect]: Resource[F, Resources[F]] = {

    def mkClient: Resource[F, Client[F]] =
      BlazeClientBuilder[F](ExecutionContext.global).resource

    def mkRef: Resource[F, Ref[F, Map[Pair, Rate]]] = Resource.liftF(Ref.of(Map.empty[Pair, Rate]))

    (mkClient, mkRef).mapN(Resources.apply)
  }
}
