package forex.app.programs.rates

import forex.core.rates.domains.Currency

object protocol {

  final case class GetRatesRequest(from: Currency, to: Currency)
}
