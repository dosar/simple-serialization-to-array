package com.dosar.sentiment

import java.util.Properties

import edu.ucla.sspace.clustering.{Assignments, CKVWSpectralClustering03, ClusteringByCommittee, SpectralClustering}
import edu.ucla.sspace.matrix.{ArrayMatrix, AtomicGrowingSparseMatrix}
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer
import org.deeplearning4j.models.word2vec.Word2Vec
import org.deeplearning4j.plot.BarnesHutTsne
import org.deeplearning4j.text.sentenceiterator.{SentenceIterator, SentencePreProcessor}
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory
import io.circe.parser.decode
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.StringCleaning
import Helper._

import scala.collection.mutable

object Word2VecHelper {
  def apply(dimensions: Int, fileGenerator: FileGenerator) = new Word2VecHelper(dimensions, fileGenerator)
}

class Word2VecHelper private[Word2VecHelper](resultDimensions: Int, fileGenerator: FileGenerator) {

  def trainAndStore(tokenizerFactory: TokenizerFactory, sentenceIterator: SentenceIterator) = {
    val vecTrainee: Word2Vec = new Word2Vec.Builder()
      .minWordFrequency(5)
      .iterations(2)
      .epochs(1)
      .layerSize(resultDimensions)
      .seed(42)
      .windowSize(10)
      .iterate(sentenceIterator)
      .tokenizerFactory(tokenizerFactory)
      .build()
    vecTrainee.fit()
    WordVectorSerializer.writeWordVectors(vecTrainee.lookupTable(), fileGenerator.createVectorsFile())
  }

  def loadWord2Vec() =
    WordVectorSerializer.readWord2VecModel(fileGenerator.vectorsFile)

  def debug(vec: Word2Vec) = {
    println(vec.vocab().hasToken("B64:aQ=="))
//    WordVectorSerializer.decodeB64("token") -> word
//    vec.lookupTable().vector("i") -> vector

  }

  def reduceDimensionsAndClusterize(vec: Word2Vec, targetDimensions: Int) = {
    val tsne = new BarnesHutTsne.Builder()
      .setMaxIter(20)
      .stopLyingIteration(250)
      .useAdaGrad(false)
      .learningRate(500)
      .theta(2)
      .normalize(true)
      .setMomentum(0.5)
      .numDimension(targetDimensions)
      .build()
    vec.lookupTable().plotVocab(tsne, vec.vocab().numWords(), fileGenerator.createTsneFile(targetDimensions))
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
        if(vector.length() != resultDimensions) println("vector length is " + vector.length())
        for(j <- 0 until vector.length())
          m.set(i, j, vector.getDouble(j))
        wordByInd(i) = word
      }
    }
    val committee = new ClusteringByCommittee()
    val properties = new Properties()
//    properties.setProperty(ClusteringByCommittee.COMMITTEE_SIMILARITY_THRESHOLD_PROPERTY, "0.99")
//    properties.setProperty(ClusteringByCommittee.RESIDUE_SIMILARITY_THRESHOLD_PROPERTY, "0.7")
    properties.setProperty(ClusteringByCommittee.HARD_CLUSTERING_PROPERTY, "false")

    val result = committee.cluster(m, properties)
    println("number of clusters is " + result.numClusters())
    for(i <- 0 until result.numClusters()){
      println(s"$i cluster has ${result.get(i).length()} elements")
    }
  }

  def spectralCluster() = {
    val matrix = meter("building matrix") {
      val matrix = Helper.withCloseable(scala.io.Source.fromFile(fileGenerator.vectorsFile)) { source =>
        val it = source.getLines().drop(1)
        val buffer = mutable.ArrayBuffer[Array[Double]]()
        while(it.hasNext)
          buffer.append(it.next().split(' ').dropAndTransform(1, _.toDouble))
        buffer
      }
      new ArrayMatrix(matrix.toArray)
    }
    val result = meter("clusterizing") { new CKVWSpectralClustering03().cluster(matrix, null) }
    println("number of clusters is " + result.numClusters())
    for(i <- 0 until result.numClusters()){
      println(s"$i cluster has ${result.get(i).length()} elements")
    }
  }

  def printClusters(file: String, clusters: Assignments) = {
    clusters.getCentroids
  }
}

object UserDocumentSentenceIterator {
  private [UserDocumentSentenceIterator] def init(sentenceFile: String, vocab: String => Boolean) = {
    val source = scala.io.Source.fromFile(sentenceFile, "UTF-8")
    val iterator = Helper.iterate(source.getLines())(decode[Array[String]]).flatten.filter { sentence =>
      val words = sentence.split(' ').map(StringCleaning.stripPunct)
      val engWords = words.count(vocab)
      (engWords / words.length.toDouble) >= 0.9
    }
    (source, iterator)
  }
}

class UserDocumentSentenceIterator(sentenceFile: String, vocab: String => Boolean) extends SentenceIterator { self =>
  private var (source, iterator) = UserDocumentSentenceIterator.init(sentenceFile, vocab)
  private var preProcessor: SentencePreProcessor = _
  override def setPreProcessor(preProcessor: SentencePreProcessor) = this.preProcessor = preProcessor
  override def getPreProcessor = preProcessor
  override def hasNext() = iterator.hasNext
  override def nextSentence() = {
    val s = iterator.next()
    if(preProcessor != null) preProcessor.preProcess(s)
    else s
  }

  override def reset() = {
    source.close()
    val (s, i) = UserDocumentSentenceIterator.init(sentenceFile, vocab)
    source = s
    iterator = i
  }

  override def finish() = {
    source.close
  }

  def close() = finish()

  def toScalaIterator = new Iterator[String] {
    override def hasNext() = self.hasNext()
    override def next() = self.nextSentence()
  }
}