name := "penstock"

organization := "io.woodenmill"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.12.6"

libraryDependencies += "org.apache.kafka" % "kafka-clients" % "1.1.1" % Provided
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.14"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.14" % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test
libraryDependencies += "net.manub" %% "scalatest-embedded-kafka" % "1.1.1" % Test

scalacOptions += "-deprecation"
