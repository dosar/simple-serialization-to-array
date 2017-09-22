package com.dosar.to.array.serializer

import java.nio.ByteBuffer
import shapeless._

trait Serializer[T] {
  def serialize(buffer: ByteBuffer, t: T): Unit
  def deserialize(buffer: ByteBuffer): T
  def length(t: T): Int
}

object Serializer {
  implicit val booleanSerializer = new BooleanSerializer
  implicit val byteSerializer = new ByteSerializer
  implicit val charSerializer = new CharSerializer
  implicit val shortSerializer = new ShortSerializer
  implicit val intSerializer = new IntSerializer
  implicit val floatSerializer = new FloatSerializer
  implicit val doubleSerializer = new DoubleSerializer
  implicit val longSerializer = new LongSerializer
  implicit val stringSerializer = new StringSerializer
  implicit val hNilSerializer = new HNilSerializer
  implicit def listSerializer[T: Serializer] = new ListSerializer[T]
  implicit def optionSerializer[T: Serializer]: Serializer[Option[T]] = new OptionSerializer[T]

  implicit def hlistSerializer[H, T <: HList](implicit
    HS: Lazy[Serializer[H]],
    TS: Serializer[T]
  ): Serializer[H :: T] = new HListSerializer[H, T]

  implicit def genSerializer[T, R](implicit gen: Generic.Aux[T, R], RS: Lazy[Serializer[R]]): Serializer[T] =
    new CaseClassSerializer[T, R]
}

class IntSerializer extends Serializer[Int] {
  override def serialize(buffer: ByteBuffer, t: Int) = buffer.putInt(t)
  override def deserialize(buffer: ByteBuffer): Int = buffer.getInt
  override def length(t: Int): Int = 4
}

class ByteSerializer extends Serializer[Byte] {
  override def serialize(buffer: ByteBuffer, t: Byte): Unit = buffer.put(t)
  override def deserialize(buffer: ByteBuffer): Byte = buffer.get()
  override def length(t: Byte): Int = 1
}

class BooleanSerializer extends Serializer[Boolean] {
  override def serialize(buffer: ByteBuffer, t: Boolean): Unit = buffer.put(if(t) 1.toByte else 0.toByte)
  override def deserialize(buffer: ByteBuffer): Boolean = buffer.get() == 1.toByte
  override def length(t: Boolean): Int = 1
}

class CharSerializer extends Serializer[Char] {
  override def serialize(buffer: ByteBuffer, t: Char): Unit = buffer.putChar(t)
  override def deserialize(buffer: ByteBuffer): Char = buffer.getChar()
  override def length(t: Char): Int = 2
}

class ShortSerializer extends Serializer[Short] {
  override def serialize(buffer: ByteBuffer, t: Short): Unit = buffer.putShort(t)
  override def deserialize(buffer: ByteBuffer): Short = buffer.getShort()
  override def length(t: Short): Int = 2
}

class DoubleSerializer extends Serializer[Double] {
  override def serialize(buffer: ByteBuffer, t: Double): Unit = buffer.putDouble(t)
  override def deserialize(buffer: ByteBuffer): Double = buffer.getDouble()
  override def length(t: Double): Int = 8
}

class FloatSerializer extends Serializer[Float] {
  override def serialize(buffer: ByteBuffer, t: Float): Unit = buffer.putFloat(t)
  override def deserialize(buffer: ByteBuffer): Float = buffer.getFloat()
  override def length(t: Float): Int = 4
}

class LongSerializer extends Serializer[Long] {
  override def serialize(buffer: ByteBuffer, t: Long): Unit = buffer.putLong(t)
  override def deserialize(buffer: ByteBuffer): Long = buffer.getLong()
  override def length(t: Long): Int = 8
}