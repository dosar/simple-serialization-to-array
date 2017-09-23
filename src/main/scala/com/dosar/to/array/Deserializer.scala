package com.dosar.to.array

import java.nio.ByteBuffer

import com.dosar.to.array.list.ListDeserializer
import com.dosar.to.array.string.StringDeserializer
import com.dosar.to.array.utils.ByteBufferUtils.ByteBufferOps
import shapeless._

trait Deserializer[T] {
  def deserialize(buffer: ByteBuffer): T
}

object Deserializer {
  def apply[T](implicit s: Deserializer[T]) = s

  def instance[T](f: ByteBuffer => T) = new Deserializer[T] {
    override def deserialize(buffer: ByteBuffer): T = f(buffer)
  }

  implicit val booleanDeserializer = instance[Boolean] { _.get() == 1.toByte }
  implicit val byteDeserializer = instance[Byte] { _.get() }
  implicit val charDeserializer = instance[Char] { _.getChar() }
  implicit val shortDeserializer = instance[Short] { _.getShort() }
  implicit val intDeserializer = instance[Int] { _.getInt() }
  implicit val floatDeserializer = instance[Float] { _.getFloat() }
  implicit val doubleDeserializer = instance[Double] { _.getDouble() }
  implicit val longDeserializer = instance[Long] { _.getLong() }
  implicit def stringDeserializer(implicit CD: Deserializer[Char], ID: Deserializer[Int]) = new StringDeserializer()(CD, ID)
  implicit val hNilDeserializer = instance[HNil] { _ => HNil }
  implicit def listDeserializer[T](implicit ID: Deserializer[Int], TD: Deserializer[T]) = new ListDeserializer[T]()(ID, TD)

  implicit def optionDeserializer[T](implicit BD: Deserializer[Boolean], TD: Deserializer[T]) = instance[Option[T]] { buf =>
    if(buf.read[Boolean](BD)) Some(buf.read[T]) else None
  }

  implicit def hlistDeserializer[H, T <: HList](implicit HD: Lazy[Deserializer[H]], TD: Deserializer[T]) =
    instance[H :: T] { buffer => buffer.read[H](HD.value) :: buffer.read[T] }

  implicit def genDeserializer[T, R](implicit gen: Generic.Aux[T, R], RD: Lazy[Deserializer[R]]) =
    instance[T] { buf => gen.from(RD.value.deserialize(buf)) }
}