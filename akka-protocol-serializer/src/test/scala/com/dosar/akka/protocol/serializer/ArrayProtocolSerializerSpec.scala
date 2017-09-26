package com.dosar.akka.protocol.serializer

import java.nio.ByteBuffer

import com.dosar.to.array.utils.ByteBufferUtils.ByteBufferOps
import com.dosar.to.array.{Length, Serializer}
import org.scalatest.WordSpec

class ArrayProtocolSerializerSpec extends WordSpec {

  "ArrayProtocolSerializer" should {
    ClazzRegistry.register[FailingCase]
    ClazzRegistry.register[RichCaseClass]
    ClazzRegistry.register[Test.type]

    "work like to-array-serializer for failing case" in check(FailingCase(Nested(List(1, 2, 4))))

    "work like to-array-serializer for complex case class" in check {
      RichCaseClass(Some("hey!"), List(Bet("user", Some(10)), Bet("one more user", None)), 0.12, 1200l)
    }

    "work like to-array-serializer for objects" in check(Test)
  }

  def check[T <: AnyRef: Serializer : Length](value: T) = {
    val akkaSerializer = new ArrayProtocolSerializer()
    val bytes = akkaSerializer.toBinary(value)
    akkaSerializer.fromBinary(bytes, None) === value
    val serializer = Serializer[T]
    val length = Length[T]
    val buffer = ByteBuffer.wrap(new Array[Byte](length(value)))
    buffer.write[T](value)(serializer)
    buffer.array().toList === bytes.drop(1).toList
  }
}

case class Nested(a: List[Int])
case class FailingCase(a: Nested)
case class Bet(userId: String, amount: Option[Int])
case class RichCaseClass(s: Option[String], bets: List[Bet], successRate: Double, allBetsCount: Long)
object Test
