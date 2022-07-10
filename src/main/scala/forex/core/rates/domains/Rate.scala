package forex.core.rates.domains

import forex.core.rates.errors
import java.time.OffsetDateTime

case class Rate(pair: Pair, price: Price, timestamp: Timestamp)

object Rate {

  def create(
      from: Currency,
      to: Currency,
      price: BigDecimal,
      timeStamp: OffsetDateTime
  ): errors.Error Either Rate =
    Pair.create(from, to).map { pair =>
      Rate(pair, Price(price), Timestamp(timeStamp))
    }
}
