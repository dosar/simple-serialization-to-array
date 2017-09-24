package com.dosar.to.array

import com.dosar.to.array.list.ListLength
import com.dosar.to.array.string.StringLength
import shapeless._

trait Length[T] {
  def apply(t: T): Int
}

object Length {
  def apply[T](implicit s: Length[T]) = s

  def instanceF[T](f: T => Int) = new Length[T] { override def apply(t: T) = f(t) }

  def instance[T](c: Int) = new Length[T] { override def apply(t: T) = c }

  implicit val booleanLength = instance[Boolean](1)
  implicit val byteLength = instance[Byte](1)
  implicit val charLength = instance[Char](2)
  implicit val shortLength = instance[Short](2)
  implicit val intLength = instance[Int](4)
  implicit val floatLength = instance[Float](4)
  implicit val doubleLength = instance[Double](8)
  implicit val longLength = instance[Long](8)
  implicit val stringLength = StringLength
  implicit val hNilLength = instance[HNil](0)

  implicit def listLength[T: Length] = new ListLength[T]

  implicit def optionLength[T](implicit TL: Length[T]) = instanceF[Option[T]] { t =>
    if (t.isDefined) TL.apply(t.get) + 1 else 1
  }

  implicit def hlistLength[H, T <: HList](implicit HL: Lazy[Length[H]], TL: Length[T]) =
    instanceF[H :: T] { v => HL.value.apply(v.head) + TL.apply(v.tail) }

  implicit def genLength[T, R](implicit gen: Generic.Aux[T, R], RL: Lazy[Length[R]]) =
    instanceF[T] { v => RL.value.apply(gen.to(v)) }
}