import sbt._

object Dependencies {

  val shapeless = "com.chuusai" %% "shapeless" % "2.3.2"
  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1" % Test
  val akka = "com.typesafe.akka" %% "akka-actor" % "2.5.4"
  val scalaCsv = "com.github.tototoshi" %% "scala-csv" % "1.3.5"
  val ejml = "org.ejml" % "ejml-all" % "0.32"
  val circeAll = Seq("circe-core", "circe-generic", "circe-parser").map("io.circe" %% _ % "0.8.0")
  val nd4j = "org.nd4j" % "nd4j-native-platform" % Version.nd4j
  val nd4jCpu = "org.nd4j" % "nd4j-native" % Version.nd4j classifier "" classifier "macosx-x86_64"
  val nd4jGpu = "org.nd4j" % "nd4j-cuda-8.0" % Version.nd4j/* classifier "" classifier "macosx-x86_64"*/
  val dl4jCore = "org.deeplearning4j" % "deeplearning4j-core" % Version.dl4j
  val dl4jNlp = "org.deeplearning4j" % "deeplearning4j-nlp" % Version.dl4j
  val dl4jNlpUima = "org.deeplearning4j" % "deeplearning4j-nlp-uima" % Version.dl4j
  val dl4jZoo = "org.deeplearning4j" % "deeplearning4j-zoo" % Version.dl4j
//  val dl4jUi = "org.deeplearning4j" %% "deeplearning4j-ui" % Version.nd4j
  val dl4jParallelWrapper = "org.deeplearning4j" %% "deeplearning4j-parallel-wrapper" % Version.dl4j
  val dl4jCpu = Seq(nd4j, dl4jCore, dl4jNlp, dl4jNlpUima, dl4jZoo/*, dl4jParallelWrapper, dl4jUi*/)
  val dl4jGpu = Seq(nd4jGpu, dl4jCore, dl4jNlp, dl4jNlpUima, dl4jZoo/*, dl4jParallelWrapper, dl4jUi*/)
  val logback = "ch.qos.logback" % "logback-classic" % "1.1.8"
  val trove = "net.sf.trove4j" % "trove4j" % "3.0.3"
  val langDetect = "org.apache.tika" % "tika-langdetect" % "1.16"
}

object Version {
  val dl4j = "0.9.1"
  val nd4j = "0.9.1"
}
