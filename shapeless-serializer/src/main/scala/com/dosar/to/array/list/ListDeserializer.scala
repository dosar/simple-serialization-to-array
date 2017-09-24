package com.dosar.to.array.list

import java.nio.ByteBuffer

import com.dosar.to.array.Deserializer
import com.dosar.to.array.utils.ByteBufferUtils.ByteBufferOps

import scala.collection.mutable.ListBuffer

class ListDeserializer[T](implicit ID: Deserializer[Int], TD: Deserializer[T]) extends Deserializer[List[T]] {
  override def apply(buffer: ByteBuffer) = {
    val length = buffer.read[Int]
    val listBuffer = ListBuffer[T]()
    var i = 0
    while(i < length) {
      listBuffer += buffer.read[T]
      i += 1
    }
    listBuffer.toList
  }
}
