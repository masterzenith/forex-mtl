package forex.core.rates.domains

case class Price(value: BigDecimal) extends AnyVal

object Price {
  def ofInt(value: Integer): Price = Price(BigDecimal(value))
}
