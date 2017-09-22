name := "simple-serialization-to-array"

version := "0.1"

scalaVersion := "2.12.3"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.2"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"