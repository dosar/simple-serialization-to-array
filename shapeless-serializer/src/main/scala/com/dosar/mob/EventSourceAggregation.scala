package com.dosar.mob

import shapeless._

class EventSourceAggregation {

}

trait Handler[-M, R] {
  def handle(o: M): R
}

case class CnilHandler[R]() extends Handler[CNil, R] {
  override def handle(o: CNil) = o.impossible

  def :++:[A](handler: Handler[A, R]): ComposedHandler[A, CNil, R] =
    ComposedHandler(handler, this)
}

case class ComposedHandler[H, T <: Coproduct, R](
  headHandler: Handler[H, R],
  tailHandler: Handler[T, R]
) extends Handler[H :+: T, R] {

  override def handle(o: :+:[H, T]) = o match {
    case Inl(head) => headHandler.handle(head)
    case Inr(tail) => tailHandler.handle(tail)
  }

  def :++:[A](handler: Handler[A, R]): ComposedHandler[A, H :+:T, R] =
    ComposedHandler(handler, this)
}



trait SubCoproduct[Inner, Outer <: Coproduct] {
  def lift(i: Inner): Outer
}

trait Included[A, C <: Coproduct] {
  def include(a: A): C
}

object Included {

  import Test._

  implicit def inductiveStep1[A, AA <: A, C <: Coproduct] = new Included[AA, A :+: C] {
    override def include(a: AA) = Inl(a: A)
  }

  implicit def inductiveStep2[B, A, C <: Coproduct](implicit I: Included[A, C]) = new Included[A, B :+: C] {
    override def include(a: A): B :+: C = Inr(I.include(a))
  }

  implicitly[Included[A1, A :+: B :+: CNil]]
}

object SubCoproduct {

  implicit def baseCase[Outer <: Coproduct]: SubCoproduct[CNil, Outer] = new SubCoproduct[CNil, Outer] {
    override def lift(i: CNil) = i.impossible
  }

  implicit def inductiveCase[IH, IT <: Coproduct, Outer <: Coproduct](implicit
    TailIs: SubCoproduct[IT, Outer],
    HeadIs: Included[IH, Outer]) = {

    new SubCoproduct[IH :+: IT, Outer] {
      override def lift(i: IH :+: IT): Outer = i match {
        case Inl(head) => HeadIs.include(head)
        case Inr(tail) => TailIs.lift(tail)
      }
    }
  }

  implicit def genericCase[E, C <: Coproduct, Outer <: Coproduct](implicit
    G: Generic.Aux[E, C],
    Cis: SubCoproduct[C, Outer]
  ): SubCoproduct[E, Outer] = new SubCoproduct[E, Outer] {
    override def lift(event: E): Outer = Cis.lift(G.to(event))
  }

}


object Test {

  sealed trait Event

  sealed trait A extends Event
  case class A1() extends A
  case class A2() extends A

  sealed trait B extends Event
  case class B1() extends B
  case class B2() extends B

  sealed trait C extends Event
  case class C1() extends C
  case class C2() extends C

  object AHandler extends Handler[A, Int] {
    def handle(f: A) = f match {
      case A1() => 1
      case A2() => 2
    }
  }

  object BHandler extends Handler[B, Int] {
    def handle(b: B) = b match {
      case B1() => 3
      case B2() => 4
    }
  }

  object CHandler extends Handler[C, Int] {
    def handle(b: C) = b match {
      case C1() => 5
      case C2() => 6
    }
  }

  val h: Handler[A :+: B :+: C :+: CNil, Int] = AHandler :++: BHandler :++: CHandler :++: CnilHandler()

  h.handle(Coproduct(A1(): A))

  import Test._
  implicitly[SubCoproduct[Event, A :+: B :+: C :+: CNil]]

}

