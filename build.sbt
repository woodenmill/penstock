name := "penstock"

organization := "io.woodenmill"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.12.6"

lazy val IntegrationTest = config("it") extend Test

lazy val root = project.in(file("."))
    .configs( IntegrationTest )
    .settings( Defaults.itSettings : _*)

libraryDependencies += "org.apache.kafka" % "kafka-clients" % "1.1.1" % Provided
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.14"
libraryDependencies += "com.softwaremill.sttp" %% "core" % "1.2.3"
libraryDependencies += "com.softwaremill.sttp" %% "async-http-client-backend-future" % "1.2.3"
libraryDependencies += "org.json4s" %% "json4s-native" % "3.6.0"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.14" % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test
libraryDependencies += "net.manub" %% "scalatest-embedded-kafka" % "1.1.1" % Test
libraryDependencies += "com.github.tomakehurst" % "wiremock" % "2.18.0" % Test
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3" % Test


scalacOptions ++= Seq(
  "-deprecation",
  "-explaintypes",
  "-Xfatal-warnings",
  "-Xlint:private-shadow",
  "-Ywarn-dead-code",
  "-Ywarn-infer-any",
  "-Ywarn-unused:imports",
  "-Ywarn-unused:implicits"
)

logBuffered in Test := false
parallelExecution in Test := false
