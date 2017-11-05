package com.dosar.sentiment

class FileGenerator(dir: String, dimensions: Int, postfix: Option[String] = None, run: Option[Int] = None) {

  def createVectorsFile() = FileUtils.recreateFile(vectorsFile).jfile

  def createTsneFile(targetDimensions: Int) = FileUtils.recreateFile(tsneFile(targetDimensions)).jfile

  private def tsneFile(targetDimensions: Int): String = s"$dir/tsne${targetDimensions}d$runPart.txt"
  final val vectorsFile = s"$dir/vectors$dimensions$runPart.txt"

  private def runPart = {
    val seq = Seq(postfix, run).flatten
    if(seq.nonEmpty) "_" + seq.mkString
    else ""
  }
}
