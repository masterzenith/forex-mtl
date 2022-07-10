package forex
package it

import core.rates.domains.Currency
import core.rates.domains.Pair
import core.rates.domains.Rate

import weaver._
import weaver.scalacheck.IOCheckers
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.Status
import org.http4s.client.Client
import org.http4s.Response
import cats.effect.Resource
import cats.effect.IO

object ForexTest extends IOSuite with IOCheckers {

  import cats.implicits._

  test("GET /rates?(valid pair) : get single rate returns Ok") { client =>
    import forex.it.utils.arbiters.currencies.currencyPairArb
    forall { pair: (Currency, Currency) =>
      val (c1, c2) = pair
      client
        .get(singleRateUrl(c1, c2))(getStatus)
        .map(code => expect(code == Status.Ok))
    }
  }

  test("GET /rates : get all rates returns Ok") { client =>
    client
      .get("http://localhost:9090/rates")(getStatus)
      .map(code => expect(code == Status.Ok))
  }

  test("GET /rates?(invalid pair) : get single rate with invalid pair returns BadRequest") {
    client =>
      import forex.it.utils.arbiters.currencies.currencyArb
      forall { c: Currency =>
        client
          .get(singleRateUrl(c, c))(getStatus)
          .map(code => expect(code == Status.BadRequest))
      }
  }

  override type Res = Client[IO]
  override def sharedResource: Resource[IO, Res] = BlazeClientBuilder[IO](ec).resource

  def getStatus(resp: Response[IO]) = IO.pure(resp.status)

  def singleRateUrl(from: Currency, to: Currency) = s"http://localhost:9090/rates?from=$from&to=$to"
}

