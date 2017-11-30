import sbt._
import Dependencies._

lazy val basicSettings = Seq(
  organization := "com.dosar",
  description := "simple serializer to array",
  startYear := Some(2017),
  shellPrompt := { s => s"${Project.extract(s).currentProject.id} > " },
  version := "0.1",
  scalaVersion := "2.12.3",
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  logLevel := Level.Info
)

lazy val root = (Project("simple-serialization-to-array", file("."))
  settings (moduleName := "simple-serialization-to-array")
  settings basicSettings
  aggregate (serializer, akkaProtocolSerializer))

lazy val serializer = (Project("shapeless-serializer", file("shapeless-serializer"))
  settings (moduleName := "shapeless-serializer")
  settings basicSettings
  settings (libraryDependencies ++= Seq(shapeless, scalaTest)))

lazy val akkaProtocolSerializer = (Project("akka-protocol-serializer", file("akka-protocol-serializer"))
  settings (moduleName := "akka-protocol-serializer")
  settings basicSettings
  settings (libraryDependencies ++= Seq(akka, scalaTest))
  dependsOn serializer)