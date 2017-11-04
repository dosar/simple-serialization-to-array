package com.dosar.sentiment

import java.io.BufferedWriter
import java.time.{Instant, LocalDate, ZoneId}

import scala.io.{BufferedSource, Codec, Source}
import Helper._
import com.github.tototoshi.csv.CSVReader
import com.github.tototoshi.csv._
import io.circe.generic.auto._
import cats.syntax.either._
import com.dosar.sentiment.FileUtils._
import com.dosar.sentiment.JavaTimeUtils._
import io.circe.Printer
import io.circe.parser._
import io.circe.syntax.EncoderOps
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, DurationLong}
import scala.concurrent.ExecutionContext.Implicits.global

class DataPreparer {

  final val printer: Printer = Printer.noSpaces.copy(dropNullKeys = true)

  type WordsCount = Int; type Ngram = String; type Coordinate = String

  object WordCoordinates {

    def unapply(line: String): Option[(WordsCount, Ngram, List[Coordinate])] = {
      val wordB64 :: points = line.split(' ').toList
      val word = WordVectorSerializer.decodeB64(wordB64)
      Some(word.split(' ').length, word, points)
    }
  }

  def prepareNgramsForOctave(file: String, cellArrayFile: String, matrixFile: String, N: Int) = {
    val size = withCloseable(Source.fromFile(file)){ _.getLines().drop(1).collect{case WordCoordinates(N, _, _) => }.size}
    withCloseable(recreateFile(cellArrayFile).bufferedWriter()){ cellOutput =>
      cellOutput.appendLine("# Created by Octave 4.2.1, Sun Oct 29 03:48:48 2017 EET <nightmarepipetz@Andreys-MacBook-Pro-2.local>")
      cellOutput.appendLine("# name: csa")
      cellOutput.appendLine("# type: cell")
      cellOutput.appendLine(s"# rows: $size")
      cellOutput.appendLine("# columns: 1")
      withCloseable(recreateFile(matrixFile).bufferedWriter()) { matrixOutput =>
        withCloseable(Source.fromFile(file)) { source =>
          for(WordCoordinates(n, ngram, coordinates) <- source.getLines().drop(1) if n == N) {
            matrixOutput.appendLine(coordinates.mkString(","))
            cellOutput.appendLine("# name: <cell-element>")
            cellOutput.appendLine("# type: sq_string")
            cellOutput.appendLine("# elements: 1")
            cellOutput.appendLine(s"# length: ${ngram.getBytes().length}")
            cellOutput.appendLine(ngram)
            cellOutput.append("\n\n")
          }
        }
      }
    }
  }

  def extractNgrams(file: String, ngramFile: String) = {
    withCloseable(recreateFile(ngramFile).bufferedWriter()) { ngramOutput =>
      withCloseable(Source.fromFile(file)) { source =>
        for(line <- source.getLines().drop(1)) {
          val wordB64 :: points = line.split(' ').toList
          val word = WordVectorSerializer.decodeB64(wordB64)
          if(word.split(' ').length > 1)
            ngramOutput.appendLine(word)
        }
      }
    }
  }

  type Month = Int; type Day = Int
  def putDataAccordingToDate(fileFrom: String, dirTo: String) = {

    val outputs: Map[(Month, Day), BufferedWriter] = (8 to 10)
      .flatMap(m => (1 to 31).map(d => (m, d) -> recreateFile(dayFile(dirTo, m, d)).bufferedWriter()))
      .toMap

    withCloseable(CSVReader.open(fileFrom, "UTF-8")) { source =>
      iterate(source.iterator)(line => SentimentDetector.toChatMsgInfo(line(9))).foreach { chatMessageInfo =>
        val date = chatMessageInfo.timems.toLocalDate
        val serialized = printer.pretty(chatMessageInfo.asJson)
        outputs(date.month -> date.day).appendLine(serialized)
      }
    }
    outputs.values.foreach(_.close)
  }

  def prepareInputForWord2Vec(dirFrom: String, fileTo: String) = {
    withCloseable(FileUtils.recreateFile(fileTo).bufferedWriter()) { output =>
      type Period = Iterable[(Month, Day)]
      val monthDayPeriods: Iterator[Period] = (8 to 10).flatMap(m => (1 to 31).map(m -> _)).grouped(8)
      val resultFutures = for(period <- monthDayPeriods) yield {
        val periodFutures = period.map{case (m, d) => Future(prepareDayData(dirFrom, m, d))}
        for (periodMessages <- Future.sequence(periodFutures))
          yield periodMessages.foreach(_.foreach(output.appendLine))
      }

      Await.result(Future.sequence(resultFutures), Duration.Inf)
    }
  }

  private def prepareDayData(dir: String, month: Int, day: Int): Array[String] =
    withCloseable(Source.fromFile(dayFile(dir, month, day))(Codec.UTF8)){ source =>
      val result = iterate(source.getLines()){ decode[ChatMsgInfo] _}.toArray.sortBy(_.timems)
      val documentsByUserIds = result.foldLeft(mutable.Map[String, ArrayBuffer[ChatMsgInfo]]()) { (map, cmi) =>
        if(!map.contains(cmi.userId)) map(cmi.userId) = ArrayBuffer[ChatMsgInfo]()
        map(cmi.userId).append(cmi.copy(sentences = cmi.sentences.map(_.toLowerCase())))
        map
      }
      val finalResult: Array[Array[ChatMsgInfo.Sentence]] =
        documentsByUserIds.flatMap { case (_, msgs) => TextAggregator.distinct(msgs.map(_.sentences)) }.toArray

      println(s"month: $month and day: $day read completed")
      finalResult.map(clusterizedUserMessages => printer.pretty(clusterizedUserMessages.asJson))
    }

  private def dayFile(dir: String, month: Int, day: Int) = {
    dir + month + "_" + day
  }
}
