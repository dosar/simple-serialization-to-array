package com.dosar.sentiment

import java.time.Instant

import io.circe.generic.auto._
import cats.syntax.either._
import io.circe.parser.decode

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import Helper._

import scala.concurrent.duration.FiniteDuration

object SentimentDetector {

  def toChatMsgInfo(str: String) = {
    decode[ChatMessage](cleanStr(str)).ensure(Left(())) { chatMessage =>
      chatMessage.source.`type` == "player"
    }.map { cm =>
      ChatMsgInfo(cm.source.id, cm.tableId,  cm.text.splitToSentences(), cm.timestamp)
    }.ensure(Left(())) { _.sentences.length > 0 }
  }
}

object TextAggregator {

  def clusterize(sortedArr: ArrayBuffer[ChatMsgInfo], distance: FiniteDuration): ArrayBuffer[Array[String]] = {
    if(sortedArr.isEmpty) ArrayBuffer[Array[String]]()
    else {
      val threshold = distance.toMillis
      val result = ArrayBuffer[Array[String]](sortedArr(0).sentences)
      var ind = 1
      while(ind < sortedArr.length) {
        val prevLine = sortedArr(ind - 1)
        val line = sortedArr(ind)
        if(line.timems - prevLine.timems <= threshold)
//          result(result.length - 1) = result.last + "\n" + line.instant + ":" + line.text
          result(result.length - 1) = result.last ++ line.sentences
//        else result.append(line.instant + ":" + line.text)
        else result.append(line.sentences)
        ind += 1
      }
      result
    }
  }

  def distinct(arr: ArrayBuffer[Array[ChatMsgInfo.Sentence]]): ArrayBuffer[Array[ChatMsgInfo.Sentence]] = {
    val seen = mutable.HashSet[ChatMsgInfo.Sentence]()
    val result = ArrayBuffer[Array[ChatMsgInfo.Sentence]]()
    var ind = 0
    while(ind < arr.length) {
      val elem = arr(ind)
      val text = elem.mkString(" ")
      if(seen.add(text)){
        result.append(elem)
      }
      ind += 1
    }
    result
  }
}

case class ChatMessage(tableId: String, text: String, timestamp: Long, source: SourceWithType)
case class SourceWithType(`type`: String, id: String)

object ChatMsgInfo {
  type Sentence = String
}

case class ChatMsgInfo(userId: String, tableId: String, sentences: Array[ChatMsgInfo.Sentence], timems: Long)
case class UserChatMsg(words: Array[String], timems: Long)