package com.dosar.to.array.serializer.utils

import java.nio.ByteBuffer

import com.dosar.to.array.serializer.Serializer

object ByteBufferUtils {
  implicit class ByteBufferOps(val b: ByteBuffer) extends AnyVal {

    def read[T](implicit tSerializer: Serializer[T]): T = tSerializer.deserialize(b)

    def write[T](t: T)(implicit tSerializer: Serializer[T]) = tSerializer.serialize(b, t)
  }
}
