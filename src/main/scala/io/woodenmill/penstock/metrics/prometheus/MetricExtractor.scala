package io.woodenmill.penstock.metrics.prometheus

import io.woodenmill.penstock.Metric
import org.json4s.JValue
import org.json4s.JsonAST.{JArray, JString}
import org.json4s.native.JsonMethods._

import scala.util.{Failure, Success, Try}

object MetricExtractor {

  def extract[B <: Metric[_]](promResponse: String)(implicit g: String => Try[B]): Try[B] = {
    parseOpt(promResponse).map(Success(_)).getOrElse(Failure(new IllegalArgumentException("Not a valid JSON")))
      .map(extractResult)
      .flatMap(extractMetricValue)
      .flatMap {
        case JString(value) => Try(value)
        case value => Failure(new IllegalArgumentException(s"Invalid response from Prometheus. Unparsable metric value: $value"))
      }
      .flatMap(g)
  }

  private val extractResult: JValue => JValue = bodyJson => bodyJson \ "data" \ "result"

  private val extractMetricValue: JValue => Try[JValue] = {
    case JArray(value :: Nil) => Try((value \ "value") (1))
    case JArray(Nil) => Failure(new IllegalArgumentException("Metric does not exist"))
    case JArray(h :: tail) => Failure(new IllegalArgumentException("Prom Query returned more than one metric. Correct your query so it fetch only one metric"))
  }

}
