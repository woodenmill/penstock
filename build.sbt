name := "reservoir"

version := "0.1"

scalaVersion := "2.12.6"

libraryDependencies += "com.softwaremill.sttp" %% "core" % "1.2.3"
libraryDependencies += "org.json4s" %% "json4s-native" % "3.6.0"


libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5"
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.14.0"
libraryDependencies += "org.apache.kafka" % "kafka-clients" % "1.1.1" % Provided


