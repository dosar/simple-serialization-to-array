package com.dosar.to.array.string

import java.nio.ByteBuffer

import com.dosar.to.array.utils.ByteBufferUtils.ByteBufferOps
import com.dosar.to.array.Deserializer

class StringDeserializer(implicit CS: Deserializer[Char], IS: Deserializer[Int]) extends Deserializer[String] {
  override def apply(buffer: ByteBuffer) = {
    val strLength = buffer.read[Int]
    val chars = new Array[Char](strLength)
    var i = 0
    while(i < strLength){
      chars(i) = buffer.read[Char]
      i += 1
    }
    new String(chars)
  }
}
