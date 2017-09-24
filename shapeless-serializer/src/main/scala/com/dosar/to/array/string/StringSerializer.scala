package com.dosar.to.array.string

import java.nio.ByteBuffer

import com.dosar.to.array.Serializer
import com.dosar.to.array.utils.ByteBufferUtils.ByteBufferOps

class StringSerializer(implicit CS: Serializer[Char], IS: Serializer[Int]) extends Serializer[String]{
  override def apply(buffer: ByteBuffer, t: String): Unit = {
    buffer.write(t.length)
    var i = 0
    while(i < t.length) {
      buffer.write(t.charAt(i))
      i += 1
    }
  }
}
