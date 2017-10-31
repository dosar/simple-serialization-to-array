package com.dosar.sentiment

import java.time.{LocalDate, LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter

import org.scalatest.WordSpec

import scala.io.Codec
import scala.reflect.io.File

class WeeksSpec extends WordSpec {
  "Life" should {
    "contain enough weeks for me" in {
      var start = LocalDate.of(1983, 9, 9)
      val now = LocalDate.now()
      val end = start.plusYears(90)
      var count = 0
      var beforenow = true
      while(start.isBefore(end)) {
        count += 1
        start = start.plusWeeks(1)
        if(beforenow && start.isAfter(now)) {
          println(s"weeks that are passed: $count")
          beforenow = false
          //count = 0
        }
      }
      println(s"remaining of my weeks are: $count")
    }
  }
}

class ImportCSVProcessorSpec extends WordSpec {

  final val clickhouseDateTimePattern = "yyyy-MM-dd HH:mm:ss"
  final val clickhouseDateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern(clickhouseDateTimePattern).withZone(ZoneOffset.UTC)

  "CSV" should {
    "be in the right format" ignore {
      val output = File("/Users/nightmarepipetz/work/files/cit_like_rates.csv")(Codec.UTF8).createFile().bufferedWriter(true)
      val source = scala.io.Source.fromFile("/Users/nightmarepipetz/work/files/global_currency_rates_cit.csv", "UTF-8")
      try {
        source.getLines().drop(1).foreach { line =>
          val id :: _ :: codeFrom :: rate :: codeTo :: timestamp :: Nil = line.split(',').filterNot(_.isEmpty).toList
          val dateTime = LocalDateTime.parse(timestamp, clickhouseDateTimeFormatter)
          val millis = dateTime.toInstant(ZoneOffset.UTC).toEpochMilli
          val date = timestamp.split(' ')(0)
          val transformedLine = List[String](
            id.replaceAll("[{}-]", ""),
            millis.toString,
            timestamp,
            millis.toString,
            timestamp,
            date,
            codeFrom,
            codeTo,
            ((1 / rate.toDouble) * 10000000000l).round.toString
          ).mkString("", ",", ",")
          output.append(transformedLine)
          output.newLine()
        }
      } finally {
        source.close()
        output.close()
      }
    }
  }

}

class ConvertVectorsToJsData extends WordSpec {

  import io.circe.generic.auto._
  import io.circe.syntax._

  "CSV" should {
    "be convertible to js format" in {

      val output = File("/Users/nightmarepipetz/work/files/jsVocabToPlot.js")(Codec.UTF8).createFile().bufferedWriter(true)
      val source = scala.io.Source.fromFile("/Users/nightmarepipetz/work/files/vocabToPlot.txt", "UTF-8")

      def jsDataPoint(line: String) = {
        val x :: y :: z :: word :: Nil = line.split(',').toList
        DataPoint(x.toDouble, y.toDouble, z.toDouble, word)
      }

      try {
        output.append("var dataPoints = ")
        val dataPoints = source.getLines().map(jsDataPoint).toList
        output.append(dataPoints.asJson.toString())
        output.append(";")
      } finally {
        source.close()
        output.close()
      }

    }
  }
}

case class DataPoint(x: Double, y: Double, z: Double, text: String, style: Int = 1)