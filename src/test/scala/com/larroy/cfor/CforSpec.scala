package com.larroy.cfor

import org.specs2.mutable.Specification


class CforSpec extends Specification {
  "Cfor" >> {
    var sum = 0
    val N = 100
    cfor(0)(i ⇒ i < N, i ⇒ i + 1) { i ⇒
      sum += i
    }
    sum should equalTo((N-1)*(N)/2)
  }
}
