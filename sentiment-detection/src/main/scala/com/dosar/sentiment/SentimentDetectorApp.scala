package com.dosar.sentiment

import Helper._
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor
import org.deeplearning4j.text.tokenization.tokenizerfactory.{DefaultTokenizerFactory, NGramTokenizerFactory}

object SentimentDetectorApp extends App {

  shardData()
  prepareData()

  def loadForDebug(iter: Long) = meter("loading vector model") {
    val vec = word2VecHelper.loadWord2Vec()
    word2VecHelper.debug(vec)
  }

  def shardData() = {
    new DataPreparer().putDataAccordingToDate(
      fileFrom = "/Users/nightmarepipetz/work/files/query-hive-38246.csv",
      dirTo = "/Users/nightmarepipetz/work/files/user-documents/")
  }

  def prepareData() = {
    new DataPreparer().prepareInputForWord2Vec(
      dirFrom = "/Users/nightmarepipetz/work/files/user-documents/",
      fileTo = "/Users/nightmarepipetz/work/files/word-vec-input-2017-8_10.txt"
    )
  }

  def prepareDataForOctave() = {
    new DataPreparer().prepareForOctave(word2VecHelper.vectorsFile,
      cellArrayFile = "/Users/nightmarepipetz/work/files/labels2d.txt",
      matrixFile = "/Users/nightmarepipetz/work/files/2dpoints.txt"
    )
  }

  def clusterize() = {
    val vec = word2VecHelper.loadWord2Vec()
//    word2VecHelper.reduceDimensionsAndClusterize(vec, 2)
    word2VecHelper.clusterByCommittee(vec)
  }

  def startSkipGram() = {

    def defaultFactory = {
      val result = new DefaultTokenizerFactory()
      result.setTokenPreProcessor(new CommonPreprocessor())
      result
    }

    def ngramFactory(min: Int, max: Int) = {
      val result = new NGramTokenizerFactory(new DefaultTokenizerFactory(), min, max)
      result.setTokenPreProcessor(new CommonPreprocessor())
      result
    }

    meter ("word 2 vec training"){
      word2VecHelper.trainAndStore("/Users/nightmarepipetz/work/files/word-vec-input-2017-8_10.txt", ngramFactory(1, 2))
    }
  }

//  lazy val word2VecHelper = Word2VecHelper(iter = 2, dimensions = 2) // done specifically for 2 dimensions reduction
  lazy val word2VecHelper = Word2VecHelper(iter = 2, dimensions = 2)
}