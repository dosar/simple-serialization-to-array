package com.dosar.sentiment

class FileGenerator(dir: String, dimensions: Int, run: Option[Int] = None) {

  def createVectorsFile() = FileUtils.recreateFile(vectorsFile).jfile

  def createTsneFile(targetDimensions: Int) = FileUtils.recreateFile(tsneFile(targetDimensions)).jfile

  private def tsneFile(targetDimensions: Int): String = s"$dir/tsne${targetDimensions}d$runPart.txt"

  private def runPart = run.map("_" + _).getOrElse("")

  final val vectorsFile = s"$dir/vectors$dimensions$runPart.txt"
}
