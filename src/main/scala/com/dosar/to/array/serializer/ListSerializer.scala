package com.dosar.to.array.serializer

import java.nio.ByteBuffer

import com.dosar.to.array.serializer.utils.ByteBufferUtils.ByteBufferOps

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

//TODO: abstract over type constructor
class ListSerializer[T](implicit
  intSerializer: Serializer[Int],
  tSerializer: Serializer[T]
) extends Serializer[List[T]] {

  override def serialize(buffer: ByteBuffer, t: List[T]): Unit = {
    buffer.write(t.length)

    @tailrec
    def iter(list: List[T]): Unit = list match {
      case head :: tail => buffer.write(head); iter(tail)
      case Nil =>
    }

    iter(t)
  }

  override def deserialize(buffer: ByteBuffer): List[T] = {
    val length = buffer.read[Int]
    val listBuffer = ListBuffer[T]()
    var i = 0
    while(i < length) {
      listBuffer += buffer.read[T]
      i += 1
    }
    listBuffer.toList
  }

  override def length(t: List[T]): Int = {
    @tailrec
    def accSize(list: List[T], result: Int): Int = list match {
      case head :: tail => accSize(tail, result + tSerializer.length(head))
      case Nil => result
    }
    4 + accSize(t, 0)
  }
}


