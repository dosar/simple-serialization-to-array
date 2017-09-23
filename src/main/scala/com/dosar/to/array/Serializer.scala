package com.dosar.to.array

import java.nio.ByteBuffer

import com.dosar.to.array.list.ListSerializer
import com.dosar.to.array.string.StringSerializer
import com.dosar.to.array.utils.ByteBufferUtils.ByteBufferOps
import shapeless._

trait Serializer[T] {
  def serialize(buffer: ByteBuffer, t: T): Unit
}

object Serializer {
  def apply[T](implicit s: Serializer[T]) = s

  def instance[T](f: (ByteBuffer, T) => Unit) = new Serializer[T] {
    override def serialize(buffer: ByteBuffer, t: T): Unit = f(buffer, t)
  }

  implicit val booleanSerializer = instance[Boolean] { (buf, v) => buf.put(if(v) 1.toByte else 0.toByte) }
  implicit val byteSerializer = instance[Byte] { _.put(_) }
  implicit val charSerializer = instance[Char] { _.putChar(_) }
  implicit val shortSerializer = instance[Short] { _.putShort(_) }
  implicit val intSerializer = instance[Int] { _.putInt(_) }
  implicit val floatSerializer = instance[Float] { _.putFloat(_) }
  implicit val doubleSerializer = instance[Double] { _.putDouble(_) }
  implicit val longSerializer = instance[Long] { _.putLong(_) }
  implicit def stringSerializer(implicit CS: Serializer[Char], IS: Serializer[Int]) = new StringSerializer()(CS, IS)
  implicit val hNilSerializer = instance[HNil] { (_, _) => () }
  implicit def listSerializer[T](implicit IS: Serializer[Int], TS: Serializer[T]) = new ListSerializer[T]()(IS, TS)

  implicit def optionSerializer[T](implicit BS: Serializer[Boolean], TS: Serializer[T]): Serializer[Option[T]] =
    instance[Option[T]] { (buf, v) =>
      buf.write(v.isDefined)(BS)
      if(v.isDefined) buf.write(v.get)
    }

  implicit def hlistSerializer[H, T <: HList](implicit HS: Lazy[Serializer[H]], TS: Serializer[T]) =
    instance[H :: T] { (buf, v) =>
      buf.write(v.head)(HS.value)
      buf.write(v.tail)
    }

  implicit def genSerializer[T, R](implicit gen: Generic.Aux[T, R], RS: Lazy[Serializer[R]]) =
    instance[T] { (buf, v) => RS.value.serialize(buf, gen.to(v)) }
}