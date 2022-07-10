package commons

object collections {

  def permutationOf2[A](set: Set[A]): Set[(A, A)] =
    for {
      n  <- set
      n1 <- set - n
    } yield (n, n1)
}
