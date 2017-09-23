package com.dosar.to.array.list

import com.dosar.to.array.Length

import scala.annotation.tailrec

class ListLength[T](implicit TL: Length[T]) extends Length[List[T]] {
  override def length(t: List[T]) = {
    @tailrec
    def accSize(list: List[T], result: Int): Int = list match {
      case head :: tail => accSize(tail, result + TL.length(head))
      case Nil => result
    }
    4 + accSize(t, 0)
  }
}
