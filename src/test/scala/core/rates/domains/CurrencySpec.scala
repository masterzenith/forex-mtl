package core.rates.domains

import forex.core.rates.domains.Currency

import weaver._
import weaver.scalacheck.IOCheckers
import org.scalacheck.Arbitrary
import org.scalacheck.Gen

object CurrencySpec extends SimpleIOSuite with IOCheckers {

  import cats.implicits._

  pureTest("Currency 'allCombinationsLength' is correct") {
    // 14! / (14 - 2)! == 14 * 13 == 182
    val combinationLength = 182
    expect(Currency.allCombinationsLength == combinationLength)
  }

  simpleTest("Currency permutations are correct") {

    val currencies = Currency.allCombinations.toList.toSet
    implicit val arbiter = Arbitrary(Gen.oneOf(currencies))

    forall { (pair: (Currency, Currency)) =>
      val (c1, c2) = pair
      val c12 = (c1, c2)
      val c21 = (c2, c1)
      val c11 = (c1, c1)
      val c22 = (c2, c2)

      import currencies._
      expect(contains(c12) && contains(c21) && !contains(c11) && !contains(c22))
    }
  }
}
