package io.woodenmill.penstock.testutils

import java.net.URL

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.stubbing.Scenario
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Suite}

trait PrometheusIntegratedSpec extends BeforeAndAfterAll with BeforeAndAfter { this: Suite =>
  val prometheusPort: Int = Ports.nextAvailablePort()
  val prometheusUri: URL = new URL(s"http://localhost:$prometheusPort")
  val wireMockServer = new WireMockServer(WireMockConfiguration.options().port(prometheusPort))


  before {
    wireMockServer.resetAll()
  }

  override protected def beforeAll(): Unit = {
    wireMockServer.start()
    configureFor("localhost", prometheusPort)
  }

  override protected def afterAll(): Unit = wireMockServer.stop()

  def configurePromStub(query: String, promResponse: String, status: Int = 200): Unit = {
    stubFor(get(urlPathEqualTo("/api/v1/query")).withQueryParam("query", equalTo(query))
      .willReturn(aResponse.withStatus(status).withBody(promResponse))
    )
  }

  def configurePromStub(query: String, firstResponse: String, nextResponses: String*): Unit = {
    stubFor(get(urlPathEqualTo("/api/v1/query")).withQueryParam("query", equalTo(query))
      .inScenario(query).whenScenarioStateIs(Scenario.STARTED).willSetStateTo("0")
      .willReturn(aResponse.withStatus(200).withBody(firstResponse))
    )

    nextResponses.zipWithIndex.foreach { case (response, stepId) => {
      stubFor(get(urlPathEqualTo("/api/v1/query")).withQueryParam("query", equalTo(query))
        .inScenario(query).whenScenarioStateIs(stepId.toString).willSetStateTo((stepId+1).toString)
        .willReturn(aResponse.withStatus(200).withBody(response))
      )
    }}

  }

}
