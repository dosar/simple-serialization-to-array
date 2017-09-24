package com.dosar.to.array.utils

import java.nio.ByteBuffer

import com.dosar.to.array.{Deserializer, Serializer}

object ByteBufferUtils {
  implicit class ByteBufferOps(val b: ByteBuffer) extends AnyVal {

    def read[T](implicit deserializer: Deserializer[T]): T = deserializer.apply(b)

    def write[T](t: T)(implicit serializer: Serializer[T]) = serializer.apply(b, t)
  }
}
