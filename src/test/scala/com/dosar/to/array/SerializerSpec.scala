package com.dosar.to.array

import java.nio.ByteBuffer

import com.dosar.to.array.utils.ByteBufferUtils.ByteBufferOps
import org.scalatest.{Matchers, WordSpec}
import shapeless._

class SerializerSpec extends WordSpec with Matchers {
  "IntSerializer" should {
    "serialize/deserialize" in {
      for(i <- -10000000 to 10000000) {
        val buffer = ByteBuffer.wrap(new Array[Byte](4))
        buffer.write(i)
        buffer.rewind()
        buffer.read[Int] shouldBe i
      }
    }
  }

  "StringSerializer" should {
    "serialize/deserialize" in {
      val str = "Hi guys! I'm from Russia and am writing nice shapeless based serialization library"
      val buf = ByteBuffer.wrap(new Array[Byte](Length[String].length(str)))
      buf.write(str)
      buf.rewind()
      buf.read[String] shouldBe str
    }
  }

  "HListSerializer" should {
    "serialize/deserialize" in {
      val hlist = "hey! uahahaha" :: 123 :: HNil
      type T = String :: Int :: HNil
      val buf = ByteBuffer.wrap(new Array[Byte](Length[T].length(hlist)))
      buf.write(hlist)
      buf.rewind()
      buf.read[T] shouldBe hlist
    }

    "serialize/deserialize generic case class" in {
      val gen = Generic[StringIntPair]
      val hlist = gen.to(StringIntPair("hey! uahahaha", 123))
      val buf = ByteBuffer.wrap(new Array[Byte](Length[gen.Repr].length(hlist)))
      buf.write(hlist)
      buf.rewind()
      buf.read[gen.Repr] shouldBe hlist
    }
  }

  "Case class serializer" should {
    "serialize/deserialize" in {
      val value = StringIntPair("youroshky onegaishimas! I am", 24)
      val buf = ByteBuffer.wrap(new Array[Byte](Length[StringIntPair].length(value)))
      buf.write(value)
      buf.rewind()
      buf.read[StringIntPair] shouldBe value
    }

    "serialize/deserialize nested types" in {
      val value = FailingCase(Nested(List(10, 12, 15)))
      val buf = ByteBuffer.wrap(new Array[Byte](Length[FailingCase].length(value)))
      buf.write(value)
      buf.rewind()
      buf.read[FailingCase] shouldBe value
    }

    "serialize/deserialize big case class" in {
      val value = Test(Some("bebebe"), List(Bet("user id", Some(10)), Bet("hui", None)), 10.0, 45609798l)
      val buf = ByteBuffer.wrap(new Array[Byte](Length[Test].length(value)))
      buf.write(value)
      buf.rewind()
      buf.read[Test] shouldBe value
    }
  }
}

case class StringIntPair(s: String, i: Int)
case class Nested(a: List[Int])
case class FailingCase(a: Nested)
case class Bet(userId: String, amount: Option[Int])
case class Test(s: Option[String], bets: List[Bet], successRate: Double, allBetsCount: Long)
