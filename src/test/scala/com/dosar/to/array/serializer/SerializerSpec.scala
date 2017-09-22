package com.dosar.to.array.serializer

import java.nio.ByteBuffer

import com.dosar.to.array.serializer.utils.ByteBufferUtils.ByteBufferOps
import org.scalatest.{Matchers, WordSpec}
import shapeless._

class SerializerSpec extends WordSpec with Matchers {
  "IntSerializer" should {
    "serialize/deserialize" in {
      val serializer = new IntSerializer()
      for(i <- -10000000 to 10000000) {
        val buffer = ByteBuffer.wrap(new Array[Byte](4))
        serializer.serialize(buffer, i)
        buffer.rewind()
        serializer.deserialize(buffer) shouldBe i
      }
    }
  }

  "StringSerializer" should {
    "serialize/deserialize" in {
      val serializer = new StringSerializer()
      val str = "Hi guys! I'm from Russia and am writing nice shapeless based serialization library"
      val buf = ByteBuffer.wrap(new Array[Byte](serializer.length(str)))
      serializer.serialize(buf, str)
      buf.rewind()
      serializer.deserialize(buf) shouldBe str
    }
  }

  "HListSerializer" should {
    "serialize/deserialize" in {
      val serializer = implicitly[Serializer[String :: Int :: HNil]]
      val hlist = "hey! uahahaha" :: 123 :: HNil
      val buf = ByteBuffer.wrap(new Array[Byte](serializer.length(hlist)))
      serializer.serialize(buf, hlist)
      buf.rewind()
      serializer.deserialize(buf) shouldBe hlist
    }

    "serialize/deserialize generic case class" in {
      val gen = Generic[StringIntPair]
      val serializer = implicitly[Serializer[gen.Repr]]
      val hlist = gen.to(StringIntPair("hey! uahahaha", 123))
      val buf = ByteBuffer.wrap(new Array[Byte](serializer.length(hlist)))
      serializer.serialize(buf, hlist)
      buf.rewind()
      serializer.deserialize(buf) shouldBe hlist
    }
  }

  "Case class serializer" should {
    "serialize/deserialize" in {
      val serializer = implicitly[Serializer[StringIntPair]]
      val value = StringIntPair("youroshky onegaishemas! I am", 24)
      val buf = ByteBuffer.wrap(new Array[Byte](serializer.length(value)))
      serializer.serialize(buf, value)
      buf.rewind()
      serializer.deserialize(buf) shouldBe value
    }

    "serialize/deserialize nested types" in {
      implicit val ser = implicitly[Serializer[FailingCase]]
      val value = FailingCase(Nested(List(10, 12, 15)))
      val buf = ByteBuffer.wrap(new Array[Byte](ser.length(value)))
      buf.write(value)
      buf.rewind()
      buf.read[FailingCase] shouldBe value
    }

    "serialize/deserialize big case class" in {
      implicit val ser = implicitly[Serializer[Test]]
      val value = Test(Some("bebebe"), List(Bet("user id", Some(10)), Bet("hui", None)), 10.0, 45609798l)
      val buf = ByteBuffer.wrap(new Array[Byte](ser.length(value)))
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
