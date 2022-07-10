package forex.core.rates
package domains

import forex.core.rates.errors.Error.CurrencyNotSupported
import cats.data.NonEmptyList
import enumeratum._

sealed trait Currency extends EnumEntry

object Currency extends Enum[Currency] with CatsEnum[Currency] {

  val values = findValues

  case object AUD extends Currency
  case object CAD extends Currency
  case object CHF extends Currency
  case object EUR extends Currency
  case object GBP extends Currency
  case object MYR extends Currency
  case object NZD extends Currency
  case object IDR extends Currency
  case object KRW extends Currency
  case object JPY extends Currency
  case object SGD extends Currency
  case object THB extends Currency
  case object USD extends Currency
  case object VUV extends Currency

  import cats.implicits._

  def fromString(currencyName: String): errors.Error Either Currency =
    Currency.withNameInsensitiveEither(currencyName).leftMap {
      case _ => CurrencyNotSupported(currencyName.some)
    }

  lazy val allCombinationsLength = allCombinations.length

  lazy val allCombinations: NonEmptyList[(Currency, Currency)] = {
    val pairs = commons.collections.permutationOf2(values.toSet).toList
    // fromListUnsafe is safe here because pairs is guaranteed non empty.
    // We can guarantee pairs unemptiness since findValues result
    // is hardcoded as enum members.
    NonEmptyList.fromListUnsafe(pairs)
  }
}
