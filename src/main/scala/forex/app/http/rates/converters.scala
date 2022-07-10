package forex
package app.http.rates

import protocols.RateResponse
import core.rates.domains.Rate

object converters {

  implicit class RateResponseOps(val rate: Rate) extends AnyVal {
    def toResponse: RateResponse =
      RateResponse(
        from = rate.pair.from,
        to = rate.pair.to,
        price = rate.price,
        timestamp = rate.timestamp
      )
  }
}
