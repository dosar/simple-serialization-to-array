package com.dosar.to.array.serializer

import java.nio.ByteBuffer

import com.dosar.to.array.serializer.utils.ByteBufferUtils.ByteBufferOps

class StringSerializer(implicit charSerializer: Serializer[Char], intSerializer: Serializer[Int]) extends Serializer[String] {
  override def serialize(buffer: ByteBuffer, t: String): Unit = {
    buffer.write(t.length)
    var i = 0
    while(i < t.length) {
      buffer.write(t.charAt(i))
      i += 1
    }
  }

  override def deserialize(buffer: ByteBuffer): String = {
    val strLength = buffer.read[Int]
    val chars = new Array[Char](strLength)
    var i = 0
    while(i < strLength){
      chars(i) = buffer.read[Char]
      i += 1
    }
    new String(chars)
  }

  override def length(t: String): Int = t.length * 2 + 4
}
