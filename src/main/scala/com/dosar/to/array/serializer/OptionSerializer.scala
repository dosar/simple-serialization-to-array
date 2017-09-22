package com.dosar.to.array.serializer

import java.nio.ByteBuffer

import com.dosar.to.array.serializer.utils.ByteBufferUtils.ByteBufferOps

class OptionSerializer[T](implicit
  boolSerializer: Serializer[Boolean],
  tSerializer: Serializer[T]
) extends Serializer[Option[T]] {

  override def serialize(buffer: ByteBuffer, t: Option[T]): Unit = {
    buffer.write(t.isDefined)
    if(t.isDefined)
      buffer.write(t.get)
  }

  override def deserialize(buffer: ByteBuffer): Option[T] = {
    if(buffer.read[Boolean])
      Some(buffer.read[T])
    else None
  }

  override def length(t: Option[T]): Int = if (t.isDefined) tSerializer.length(t.get) + 1 else 1
}
