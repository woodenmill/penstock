package io.woodenmill.penstock.report

import io.woodenmill.penstock.Metrics.{Counter, Gauge}
import io.woodenmill.penstock.testutils.Spec

class AsciiTableFormatterSpec extends Spec {

  "Table Formatter" should "convert list of metrics into formatted ASCII table" in {
    val metrics = List(Counter(42, "counter", 0L), Gauge(2.3, "gauge", 0L))

    val formatted = AsciiTableFormatter.format(metrics)

    val expected =
      """╒═══════════════════════════════════════╤══════════════════════════════════════╕
        |│Metric Name                            │Metric Value                          │
        |╞═══════════════════════════════════════╪══════════════════════════════════════╡
        |│counter                                │42                                    │
        |│gauge                                  │2.3                                   │
        |╘═══════════════════════════════════════╧══════════════════════════════════════╛""".stripMargin
    formatted shouldBe expected
  }

}
