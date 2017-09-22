package com.dosar.to.array.serializer

import java.nio.ByteBuffer

import com.dosar.to.array.serializer.utils.ByteBufferUtils.ByteBufferOps
import shapeless._

class CaseClassSerializer[T, R](implicit
  gen: Generic.Aux[T, R],
  RS: Lazy[Serializer[R]]
) extends Serializer[T] {
  override def serialize(buffer: ByteBuffer, t: T): Unit = RS.value.serialize(buffer, gen.to(t))
  override def deserialize(buffer: ByteBuffer): T = gen.from(RS.value.deserialize(buffer))
  override def length(t: T): Int = RS.value.length(gen.to(t))
}

class HListSerializer[Head, Tail <: HList](implicit
  HeadSerializer: Lazy[Serializer[Head]],
  TailSerializer: Serializer[Tail],
) extends Serializer[Head :: Tail] {

  override def serialize(buffer: ByteBuffer, t: Head :: Tail): Unit = {
    buffer.write(t.head)(HeadSerializer.value)
    buffer.write(t.tail)
  }

  override def deserialize(buffer: ByteBuffer): Head :: Tail = {
    buffer.read[Head](HeadSerializer.value) :: buffer.read[Tail]
  }

  override def length(t: Head :: Tail): Int = HeadSerializer.value.length(t.head) + TailSerializer.length(t.tail)
}

class HNilSerializer extends Serializer[HNil] {
  override def serialize(buffer: ByteBuffer, t: HNil): Unit = {}
  override def deserialize(buffer: ByteBuffer): HNil = HNil
  override def length(t: HNil): Int = 0
}