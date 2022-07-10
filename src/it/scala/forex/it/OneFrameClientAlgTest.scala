package forex
package it

import interps.http.PaidyOneFrameRateClientAlg
import core.rates.domains.Pair
import core.rates.domains.Rate

import weaver._
import weaver.scalacheck.IOCheckers
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.Client
import org.http4s.Uri
import cats.effect.{IO, Resource}
import cats.data.NonEmptyList

object OneFrameClientAlgTest extends IOSuite with IOCheckers {

  import cats.implicits._
  import utils.logger._

  val uri = Uri.fromString("http://localhost:8080").right.get

  test("get(pair) should obtain right result") { algebra =>
    import forex.it.utils.arbiters.currencies.pairArb
    forall { pair: Pair =>
      algebra
        .get(pair)
        .map(either => expect(either.isRight))
    }
  }

  test("getAll(pair) should obtain right result") { algebra =>
    import forex.it.utils.arbiters.currencies.pairListArb
    forall { pairs: List[Pair] =>
      algebra
        .getAll(NonEmptyList.fromListUnsafe(pairs))
        .map(either => expect(either.isRight))
    }
  }

  test("allRates should obtain 182 rates") { algebra =>
    algebra.allRates.compile.toList.map(rates => expect(rates.length == 182))
  }

  override type Res = PaidyOneFrameRateClientAlg[IO]
  override def sharedResource: Resource[IO, Res] =
    BlazeClientBuilder[IO](ec).resource.evalMap { client =>
      PaidyOneFrameRateClientAlg(uri, client, "10dc303535874aeccc86a8251e6992f5")
    }
}

