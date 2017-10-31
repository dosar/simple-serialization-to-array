package com.dosar.sentiment

import java.io.BufferedWriter
import java.time.{Instant, LocalDate, ZoneId}

import scala.io.{BufferedSource, Source}
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

  def prepareForOctave(file: String, cellArrayFile: String, matrixFile: String) = {
    val size = withCloseable(Source.fromFile(file)) { _.getLines().drop(1).size }
    withCloseable(recreateFile(cellArrayFile).bufferedWriter()) { cellOutput =>
      cellOutput.appendLine("# Created by Octave 4.2.1, Sun Oct 29 03:48:48 2017 EET <nightmarepipetz@Andreys-MacBook-Pro-2.local>")
      cellOutput.appendLine("# name: csa")
      cellOutput.appendLine("# type: cell")
      cellOutput.appendLine(s"# rows: $size")
      cellOutput.appendLine("# columns: 1")
      withCloseable(recreateFile(matrixFile).bufferedWriter()) { matrixOutput =>
        withCloseable(Source.fromFile(file)) { source =>
          for(line <- source.getLines().drop(1)) {
            val wordB64 :: points = line.split(' ').toList
            matrixOutput.appendLine(points.mkString(","))
            val word = WordVectorSerializer.decodeB64(wordB64)
            cellOutput.appendLine("# name: <cell-element>")
            cellOutput.appendLine("# type: sq_string")
            cellOutput.appendLine("# elements: 1")
            cellOutput.appendLine(s"# length: ${word.getBytes().length}")
            cellOutput.appendLine(word)
            cellOutput.append("\n\n")
          }
        }
      }
    }
  }

  type Month = Int; type Day = Int
  def putDataAccordingToDate(fileFrom: String, dirTo: String) = {

    val outputs: Map[(Month, Day), BufferedWriter] = (8 to 10)
      .flatMap(m => (1 to 31).map(d => (m, d) -> recreateFile(dayFile(dirTo, m, d)).bufferedWriter()))
      .toMap

    withCloseable(CSVReader.open(fileFrom)) { source =>
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
    withCloseable(Source.fromFile(dayFile(dir, month, day))){ source =>
      val result = iterate(source.getLines()){ decode[ChatMsgInfo] _}.toArray.sortBy(_.timems)
      val documentsByUserIds = result.foldLeft(mutable.Map[String, ArrayBuffer[ChatMsgInfo]]()) { (map, cmi) =>
        if(!map.contains(cmi.userId)) map(cmi.userId) = ArrayBuffer[ChatMsgInfo]()
        map(cmi.userId).append(cmi.copy(sentences = cmi.sentences.map(_.toLowerCase())))
        map
      }
//      val finalResult: ArrayBuffer[Array[String]] = TextAggregator.distinct {
//        documentsByUserIds.iterator.flatMap { case (_, msgs) =>
//          TextAggregator.clusterize(msgs, 1 minute)
//        }.to[ArrayBuffer]
//      }
      val finalResult: Array[Array[ChatMsgInfo.Sentence]] =
        documentsByUserIds.flatMap { case (_, msgs) => TextAggregator.distinct(msgs.map(_.sentences)) }.toArray

      println(s"month: $month and day: $day read completed")
      finalResult.map(clusterizedUserMessages => printer.pretty(clusterizedUserMessages.asJson))
    }

  private def dayFile(dir: String, month: Int, day: Int) = {
    dir + month + "_" + day
  }
}
