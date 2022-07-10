package forex.core.rates
package domains

import cats.Show
import cats.data.NonEmptyList

final case class Pair private (from: Currency, to: Currency)

object Pair {

  import cats.implicits._

  implicit val pairShow = Show.show[Pair](p => s"${p.from}${p.to}")

  def create(from: Currency, to: Currency): errors.Error Either Pair =
    if (from == to) errors.Error.DoublePair.asLeft
    else Pair(from, to).asRight

  def allCurrencyPairs: NonEmptyList[Pair] =
    Currency.allCombinations.map { case (c1, c2) => Pair(c1, c2) }
}
