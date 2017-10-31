package com.dosar.sentiment

import java.util.Properties

import edu.ucla.sspace.clustering.ClusteringByCommittee
import edu.ucla.sspace.matrix.{AtomicGrowingSparseMatrix, SparseHashMatrix, SparseMatrix}
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer
import org.deeplearning4j.models.word2vec.Word2Vec
import org.deeplearning4j.plot.BarnesHutTsne
import org.deeplearning4j.text.sentenceiterator.{SentenceIterator, SentencePreProcessor}
import org.deeplearning4j.text.tokenization.tokenizerfactory.{DefaultTokenizerFactory, TokenizerFactory}
import io.circe.parser.decode

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object Word2VecHelper {
  def apply(iter: Int, dimensions: Int) = new Word2VecHelper(Some(iter), dimensions)
}

class Word2VecHelper private[Word2VecHelper](iter: Option[Long] = None, resultDimensions: Int) {

  def trainAndStore(sentencesFile: String, tokenizerFactory: TokenizerFactory) = {
    val sentenceIterator = new UserDocumentSentenceIterator(sentencesFile)
    val vecTrainee: Word2Vec = new Word2Vec.Builder()
      .minWordFrequency(20)
      .iterations(5)
      .epochs(5)
      .layerSize(resultDimensions)
      .negativeSample(1.0)
      .seed(42)
      .windowSize(5)
      .iterate(sentenceIterator)
      .tokenizerFactory(tokenizerFactory)
      .build()
    vecTrainee.fit()
    WordVectorSerializer.writeWordVectors(vecTrainee.lookupTable(), file(vectorsFile))
  }

  def loadWord2Vec() =
    WordVectorSerializer.readWord2VecModel(vectorsFile)

  def debug(vec: Word2Vec) = {
    println(vec.vocab().hasToken("B64:aQ=="))
//    WordVectorSerializer.decodeB64("token") -> word
//    vec.lookupTable().vector("i") -> vector

  }

  def reduceDimensionsAndClusterize(vec: Word2Vec, dimensionsNum: Int) = {
    val tsne = new BarnesHutTsne.Builder()
      .setMaxIter(20)
      .stopLyingIteration(250)
      .useAdaGrad(false)
      .learningRate(500)
      .theta(2)
      .normalize(true)
      .setMomentum(0.5)
      .numDimension(dimensionsNum)
//      .perplexity(100)
      .build()
    vec.lookupTable().plotVocab(tsne, vec.vocab().numWords(), file(vocabToPlotFile))
//    vec.lookupTable().plotVocab(tsne, 10000, file)
  }

  def clusterByCommittee(vec: Word2Vec) = {
    val m = new AtomicGrowingSparseMatrix()
    val lookupTable = vec.lookupTable()
    val vocabCache = lookupTable.getVocabCache
    val wordByInd = mutable.Map[Int, String]()
    Helper.meter("matrix fill") {
      for(i <- 0 until vocabCache.numWords()) {
        val word = vocabCache.elementAtIndex(i).getLabel
        val vector = lookupTable.vector(word)
        if(vector.length() != 300) println("vector length is " + vector.length())
        for(j <- 0 until vector.length())
          m.set(i, j, vector.getDouble(j))
        wordByInd(i) = word
      }
    }
    val committee = new ClusteringByCommittee()
    val properties = new Properties()
    properties.setProperty(ClusteringByCommittee.COMMITTEE_SIMILARITY_THRESHOLD_PROPERTY, "0.99")
    properties.setProperty(ClusteringByCommittee.RESIDUE_SIMILARITY_THRESHOLD_PROPERTY, "0.7")

    val result = committee.cluster(m, properties)
    println("number of clusters is " + result.numClusters())
    for(i <- 0 until result.numClusters()){
      println(s"$i cluster has ${result.get(i).length()} elements")
    }
  }

  def file(path: String) = FileUtils.recreateFile(path).jfile

  final val vectorsFile = s"/Users/nightmarepipetz/work/files/vectors${iter.getOrElse("")}.txt"
  final val vocabToPlotFile = s"/Users/nightmarepipetz/work/files/tsne2d${iter.getOrElse("")}.txt"
}

object UserDocumentSentenceIterator {
  private [UserDocumentSentenceIterator] def init(file: String) = {
    val source = scala.io.Source.fromFile(file)
    (source, Helper.iterate(source.getLines())(decode[Array[String]]))
  }
}

class UserDocumentSentenceIterator(file: String) extends SentenceIterator {
  private var (source, iterator) = UserDocumentSentenceIterator.init(file)
  private var preProcessor: SentencePreProcessor = _
  override def setPreProcessor(preProcessor: SentencePreProcessor) = this.preProcessor = preProcessor
  override def getPreProcessor = preProcessor
  override def hasNext = iterator.hasNext
  override def nextSentence() = {
    iterator.next().mkString(" ")
  }

  override def reset() = {
    source.close()
    val (s, i) = UserDocumentSentenceIterator.init(file)
    source = s
    iterator = i
  }

  override def finish() = {
    source.close
  }
}