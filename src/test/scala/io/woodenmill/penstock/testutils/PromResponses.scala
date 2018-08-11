package io.woodenmill.penstock.testutils


  object PromResponses {
    val noDataPoint: String =
      """
        |{
        | "status": "success",
        | "data": {
        |   "resultType": "vector",
        |   "result": []
        | }
        |}
      """.stripMargin

    def responseWithNegativeValue: String =
    """
      |{
      | "status": "success",
      | "data": {
      |   "resultType": "vector",
      |   "result": [
      |     {
      |       "metric": {
      |         "__name__": "jvm_memory_bytes_max",
      |         "area": "nonheap",
      |         "instance": "kafka:7071",
      |         "job": "kafka"
      |     },
      |   "value": [
      |     1533934927.769,
      |     "-1"
      |   ]
      |  }
      | ]
      | }
      |}
    """.stripMargin

    def valid(metricValue: String): String =
      s"""
         |{
         | "status": "success",
         | "data": {
         |   "resultType": "vector",
         |   "result": [
         |     {
         |       "metric": {
         |         "instance": "kafka:7071",
         |         "job": "kafka",
         |         "topic": "test"
         |       },
         |       "value": [
         |         1533765030.363,
         |         "$metricValue"
         |       ]
         |     }
         |   ]
         | }
         |}
    """.stripMargin


    val invalidValue: String =
      s"""
         |{
         | "status": "success",
         | "data": {
         |   "resultType": "vector",
         |   "result": [
         |     {
         |       "metric": {
         |         "instance": "kafka:7071",
         |         "job": "kafka",
         |         "topic": "test"
         |       },
         |       "value": [
         |         1533765030.363,
         |         3
         |       ]
         |     }
         |   ]
         | }
         |}
    """.stripMargin

    def multipleMetricsResponse(): String =
      s"""
         |{
         | "status": "success",
         | "data": {
         |   "resultType": "vector",
         |   "result": [
         |     {
         |       "metric": {
         |         "instance": "kafka:7071",
         |         "job": "kafka",
         |         "topic": "test"
         |       },
         |       "value": [
         |         1533765030.363,
         |         "1"
         |       ]
         |     },
         |     {
         |       "metric": {
         |         "instance": "kafka:7071",
         |         "job": "kafka",
         |         "topic": "test"
         |       },
         |       "value": [
         |         1533765030.363,
         |         "2"
         |       ]
         |     }
         |   ]
         | }
         |}
    """.stripMargin

  }