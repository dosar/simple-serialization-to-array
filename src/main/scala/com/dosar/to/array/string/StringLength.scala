package com.dosar.to.array.string

import com.dosar.to.array.Length

object StringLength extends Length[String] {
  override def length(t: String) = t.length * 2 + 4
}
