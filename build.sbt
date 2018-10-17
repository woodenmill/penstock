name := "penstock"
version := "0.0.2"
organization := "io.woodenmill"
bintrayOrganization := Some("woodenmill")
bintrayRepository := "oss-maven"
bintrayReleaseOnPublish in ThisBuild := false
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
scalaVersion := "2.12.6"

lazy val IntegrationTest = config("it") extend Test

lazy val root = project.in(file("."))
    .configs( IntegrationTest )
    .settings( Defaults.itSettings : _*)

resolvers += Resolver.bintrayRepo("ovotech", "maven")

val circeVersion = "0.9.3"
val sttpVersion = "1.3.0"

libraryDependencies += "org.apache.kafka" % "kafka-clients" % "1.1.1" % Provided
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.14"
libraryDependencies += "com.softwaremill.sttp" %% "core" % sttpVersion
libraryDependencies += "com.softwaremill.sttp" %% "async-http-client-backend-cats" % sttpVersion
libraryDependencies += "io.circe" %% "circe-core" % circeVersion
libraryDependencies += "io.circe" %% "circe-generic" % circeVersion
libraryDependencies += "io.circe" %% "circe-parser" % circeVersion
libraryDependencies += "org.typelevel" %% "cats-effect" % "0.10.1"
libraryDependencies += "de.vandermeer" % "asciitable" % "0.3.2"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.14" % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test
libraryDependencies += "net.manub" %% "scalatest-embedded-kafka" % "1.1.1" % Test
libraryDependencies += "com.github.tomakehurst" % "wiremock" % "2.18.0" % Test
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3" % Test
libraryDependencies += "com.ovoenergy" %% "kafka-serialization-core" % "0.3.11" % Test
libraryDependencies += "com.ovoenergy" %% "kafka-serialization-circe" % "0.3.11" % Test


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
cancelable in Global := true
