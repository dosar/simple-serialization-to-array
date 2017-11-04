package com.dosar.sentiment

import java.io.BufferedWriter
import java.time.{Instant, LocalDate, ZoneId}

import io.circe.Decoder

import scala.io.Codec

object FileUtils {

  def recreateFile(path: String) = {
    val f = reflect.io.File(path)(Codec.UTF8)
    if(f.exists) f.delete()
    f.createFile()
  }

  implicit class FileOps(val file: reflect.io.File) extends AnyVal {

    def writer: BufferedWriter = file.bufferedWriter(false)
  }

  implicit class WriterOps(val writer: BufferedWriter) extends AnyVal {

    def appendLine(line: String) = {
      writer.append(line)
      writer.newLine()
    }
  }
}

object JavaTimeUtils {

  implicit class MillisOps(val millis: Long) extends AnyVal {

    def toLocalDate = Instant.ofEpochMilli(millis).atZone(UtcZoneId).toLocalDate
  }

  implicit class LocalDateOps(val date: LocalDate) extends AnyVal {
    def year = date.getYear
    def month = date.getMonthValue
    def day = date.getDayOfMonth
  }

  val UtcZoneId = ZoneId.of("UTC")
}