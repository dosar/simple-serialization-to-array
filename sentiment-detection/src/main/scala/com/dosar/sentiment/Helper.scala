package com.dosar.sentiment

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object Helper {
  val filename = "/Users/nightmarepipetz/Downloads/query-hive-37507.csv"

  implicit class StringOps(str: String){

    def splitToSentences(): Array[ChatMsgInfo.Sentence] = {
      val result = ArrayBuffer[ChatMsgInfo.Sentence]()
      for(sentence <- str.toLowerCase().split("""[\.,\?!]+""") if !sentence.isEmpty) {
        val words = sentence.split("""[-\d\s,;:\)\(]+""").filterNot(_.isEmpty)
        if(words.length > 1)
          result.append(words.mkString(" "))
      }
      result.toArray
    }
  }

  def printDocuments(documents: ArrayBuffer[Array[String]]) = {
    val res = documents.sortBy(x => -x.iterator.map(_.length).sum)
    println(s"size of corpus: ${res.length}")
    res.take(1000).foreach{ x =>
//      println("original: '" + x.mkString(" ") + "', splitted: " + x.toList)
      println(x.mkString(" "))
      println("\n")
    }
  }

  def printOccurenciesMap[D](map: mutable.Map[D, Int], mapDescription: String) = {
    println(s"top $mapDescription: ")
    println(s"size: ${map.size}")
    map.toVector.sortBy(_._2)(implicitly[Ordering[Int]].reverse).take(100).foreach(println)
  }

  def occurenciesMap[T, D](it: Iterator[T])(f: T => D): mutable.Map[D, Int] = {
    it.foldLeft(mutable.Map[D, Int]() withDefaultValue 0) { (map, t) =>
      map(f(t)) += 1
      map
    }
  }

  def iterate[T, D](it: Iterator[T])(collect: T => Either[Any, D]): Iterator[D] = {
    it.map(t => collect(t)).collect{ case Right(d) => d }
  }

  def withCloseable[R, T <: {def close(): Unit} ](s: => T)(f: T => R): R = {
    val source = s
    try {
      f(source)
    } finally {
      source.close()
    }
  }

  def meter[T](desc: String)(f: => T) = {
    val now = System.currentTimeMillis()
    println("========== start " + desc)
    val res = f
    println("========== " + desc + " took " + (System.currentTimeMillis() - now) + "ms")
    res
  }

  def cleanStr(str: String) = {
    str.replace("\"{", "{").replace("}\"", "}").replace("""\"""", "\"")
  }
}
