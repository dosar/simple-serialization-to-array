package com.dosar.akka.protocol.serializer

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

import akka.serialization.Serializer
import com.dosar.to.array.utils.ByteBufferUtils.ByteBufferOps
import com.dosar.to.array.{Deserializer, Length, Serializer => ToArraySerializer}

import scala.collection.concurrent.TrieMap
import scala.reflect.ClassTag

class ArrayProtocolSerializer extends Serializer {
  override final val includeManifest: Boolean = false
  override final val identifier: Int = 1000

  override def toBinary(o: AnyRef): Array[Byte] = ClazzRegistry.serialize(o)

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef =
    ClazzRegistry.deserialize(bytes)
}

object ClazzRegistry {

  def register[T](implicit TCT: ClassTag[T], TS: ToArraySerializer[T], TD: Deserializer[T], TL: Length[T]): Unit = {
    val classMarker = markerCounter.getAndIncrement()
    classMarkerMap.putIfAbsent(TCT.runtimeClass, classMarker) map { _ =>
      throw new Error(s"there was an existing registered class with ${TCT.runtimeClass} -> ${(TS, TD, TL)}")
    } getOrElse {
      map.putIfAbsent(classMarker, (as(TS), as(TD), as(TL)))
    }
  }

  def serialize(o: AnyRef): Array[Byte] = {
    val classMarker = classMarkerMap(o.getClass)
    val (serializer, _, length) = map(classMarker)
    val buffer = ByteBuffer.wrap(new Array[Byte](intLength(classMarker) + length(o)))
    buffer.write(classMarker)(intSerializer)
    serializer(buffer, o)
    buffer.array()
  }

  def deserialize(array: Array[Byte]): AnyRef = {
    val buffer = ByteBuffer.wrap(array)
    val classMarker = buffer.read[Int](intDeserializer)
    as(map(classMarker)._2(buffer))
  }

  //TODO: remove this dirty hack
  private def as[T](o: Any) = o.asInstanceOf[T]
  private final val classMarkerMap = new TrieMap[Class[_], Int]()
  private final val map = new TrieMap[Int, (ToArraySerializer[_ >: AnyRef], Deserializer[_ >: AnyRef], Length[_ >: AnyRef])]()
  private final val intLength = Length[Int]
  private final val intSerializer = ToArraySerializer[Int]
  private final val intDeserializer = Deserializer[Int]
  private final val markerCounter = new AtomicInteger(0)
}