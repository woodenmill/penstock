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

    def responseWithNegativeValue(time: Long): String =
    s"""
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
      |     $time,
      |     "-1"
      |   ]
      |  }
      | ]
      | }
      |}
    """.stripMargin

    def valid(time: Long, metricValue: String): String =
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
         |         $time,
         |         "$metricValue"
         |       ]
         |     }
         |   ]
         | }
         |}
    """.stripMargin

    val resultIsNotAnArray: String =
      s"""
         |{
         | "status": "success",
         | "data": {
         |   "resultType": "vector",
         |   "result": "what the hell Prometheus?"
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