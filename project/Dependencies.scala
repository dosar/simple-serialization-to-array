import sbt._

object Dependencies {

  val shapeless = "com.chuusai" %% "shapeless" % "2.3.2"
  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1" % Test
  val akka = "com.typesafe.akka" %% "akka-actor" % "2.5.4"
  val scalaCsv = "com.github.tototoshi" %% "scala-csv" % "1.3.5"
  val ejml = "org.ejml" % "ejml-all" % "0.32"
  val circeAll = Seq("circe-core", "circe-generic", "circe-parser").map("io.circe" %% _ % "0.8.0")
  val nd4j = "org.nd4j" % "nd4j-native-platform" % Version.nd4j
  val dl4jCore = "org.deeplearning4j" % "deeplearning4j-core" % Version.nd4j
  val dl4jNlp = "org.deeplearning4j" % "deeplearning4j-nlp" % Version.nd4j
  val dl4jZoo = "org.deeplearning4j" % "deeplearning4j-zoo" % Version.nd4j
//  val dl4jUi = "org.deeplearning4j" %% "deeplearning4j-ui" % Version.nd4j
  val dl4jParallelWrapper = "org.deeplearning4j" %% "deeplearning4j-parallel-wrapper" % Version.nd4j
  val dl4jAll = Seq(nd4j, dl4jCore, dl4jNlp, dl4jZoo/*, dl4jParallelWrapper, dl4jUi*/)
  val logback = "ch.qos.logback" % "logback-classic" % "1.1.8"
  val trove = "net.sf.trove4j" % "trove4j" % "3.0.3"
}

object Version {
  val nd4j = "0.9.1"
}
