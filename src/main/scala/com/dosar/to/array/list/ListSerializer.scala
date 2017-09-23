package com.dosar.to.array.list

import java.nio.ByteBuffer

import com.dosar.to.array.Serializer
import com.dosar.to.array.utils.ByteBufferUtils.ByteBufferOps

import scala.annotation.tailrec

//TODO: abstract over type constructor
class ListSerializer[T](implicit IS: Serializer[Int], TS: Serializer[T]) extends Serializer[List[T]] {

  override def serialize(buffer: ByteBuffer, t: List[T]): Unit = {
    @tailrec
    def iter(list: List[T]): Unit = list match {
      case head :: tail => buffer.write(head); iter(tail)
      case Nil =>
    }

    buffer.write(t.length)
    iter(t)
  }
}


