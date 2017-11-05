package com.dosar.sentiment

import Helper._
import com.typesafe.config.ConfigFactory
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.{CommonPreprocessor, StringCleaning}
import org.deeplearning4j.text.tokenization.tokenizerfactory.{DefaultTokenizerFactory, NGramTokenizerFactory}

import scala.collection.mutable

object SentimentDetectorApp extends App {

  word2vec.startSkipGram()

  def loadForDebug() = meter("loading vector model") {
    val vec = word2vec.helper.loadWord2Vec()
    word2vec.helper.debug(vec)
  }

  object data {

    val trainingInput = conf.filesDir + "/word-vec-input-2017-8_10.txt"
    val google10000EnWords = conf.filesDir + "/google-10000-english.txt"
    val shardInput = conf.filesDir + "/query-hive-38246.csv"
    val shardOutput = conf.filesDir + "/user-documens/"

    def shard() =
      new DataPreparer().putDataAccordingToDate(fileFrom = shardInput, dirTo = shardOutput)

    def prepareForTraining() =
      new DataPreparer().prepareInputForWord2Vec(dirFrom = shardOutput, fileTo = trainingInput)

    final val engVocab: Vocab = new Vocab {
      override final val lang: Option[String] = Some("en")
      override def apply(word: String): Boolean = vocab(word)

      private final val vocab = withCloseable(scala.io.Source.fromFile(data.google10000EnWords, "UTF-8")) { source =>
        source.getLines().foldLeft(mutable.HashSet[String]()){ (set, word) => set.add(word); set }
      }
    }

    def allVocab(): Vocab = new Vocab {
      override final val lang: Option[String] = None
      override def apply(word: String): Boolean = true
    }
  }

  object forOctave {

    def prepareUnigrams() = {
      new DataPreparer().prepareNgramsForOctave(word2vec.fileGenerator.vectorsFile,
        cellArrayFile = labelsFile,
        matrixFile = matrixFile,
        N = 1
      )
    }

    def extractNgrams() =
      new DataPreparer().extractNgrams(word2vec.fileGenerator.vectorsFile, ngramsFile)


    val labelsFile = conf.filesDir + "/labels2d.txt"
    val matrixFile = conf.filesDir + "/2dpoints.txt"
    val ngramsFile = conf.filesDir + "/ngrams300.txt"
  }

  object debug {
    def countSentences() = {
      val it = new UserDocumentSentenceIterator(data.trainingInput, data.engVocab)
      var sentences = 0
      val words = new mutable.HashSet[String]()
      while (it.hasNext()) {
        val sentence = it.nextSentence()
        sentence.split(' ').foreach(w => words.add(StringCleaning.stripPunct(w)))
        sentences += 1
      }
      println(s"amount of sentences is $sentences")
      println(s"amount of unique words is ${words.size}")
    }

    def showWordsInSentenceDistribution() = {
      type WordsCount = Int; type SentCount = Int
      withCloseable(new UserDocumentSentenceIterator(data.trainingInput, data.engVocab)) { it =>
        val result = it.toScalaIterator.foldLeft(mutable.Map[WordsCount, SentCount]() withDefaultValue 0) { case (map, sentence) =>
          val wordsCount = sentence.split(' ').length
          map(wordsCount) += 1
          map
        }
        val all = result.values.sum
        result.toSeq.sortBy(_._1).foldLeft(0){ case (acc, (wordsCount, sentCount)) =>
          val newAcc = acc + sentCount
          println(s"words: $wordsCount, sentences: $sentCount, percentile: ${format(sentCount.toDouble / all * 100)}, " +
            s"withLessWords: $newAcc, percentile: ${format(newAcc.toDouble / all * 100)}")
            newAcc
        }
      }
    }
  }

  object word2vec {
    final val dimensions = 700
    final val vocab = data.engVocab
    final val fileGenerator = new FileGenerator(conf.filesDir, dimensions, postfix = vocab.lang, run = None)
    final val helper = Word2VecHelper(dimensions, fileGenerator)

    def startSkipGram() = {
      meter ("word 2 vec training"){
        val sentenceIterator = new UserDocumentSentenceIterator(data.trainingInput, vocab)
        helper.trainAndStore(ngramFactory(1, 2), sentenceIterator)
      }
    }

    def clusterize() = {
      val vec = meter("model loading"){ helper.loadWord2Vec() }
      helper.clusterByCommittee(vec)
    }

    private def defaultFactory = using(new DefaultTokenizerFactory())(_.setTokenPreProcessor(new CommonPreprocessor()))

    private def ngramFactory(min: Int, max: Int) =
      using(new NGramTokenizerFactory(new DefaultTokenizerFactory(), min, max))(_.setTokenPreProcessor(new CommonPreprocessor()))
  }

  lazy val tsConfig = ConfigFactory.load().getConfig("sentiment-detection")
  lazy val conf = new {
    final val filesDir = tsConfig.getString("files-dir")
  }
}