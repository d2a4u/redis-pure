package com.d2a4u.redispure

import com.d2a4u.redispure.models._
import com.d2a4u.redispure.resp._
import com.d2a4u.redispure.serialization.Encoder
import org.scalatest.{EitherValues, FlatSpec, Matchers}

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

class ModelSpec extends FlatSpec with Matchers with EitherValues {

  implicit class MatchResult(parsed: Try[RESP]) {
    def resultType[T](implicit tag: ClassTag[T]): Unit = {
      parsed match {
        case Success(resp) =>
          resp shouldBe a[T]

        case Failure(err) =>
          throw err
      }
    }
  }

  "RESPParser" should "parse BulkString" in {
    new RESPParser("$-1\r\n").BulkStringInput.run().resultType[RENullString.type]
    new RESPParser("$1\r\na\r\n").BulkStringInput.run().resultType[REBulkString]
    new RESPParser("$12\r\nfoo b4r  ba$\r\n").BulkStringInput.run().resultType[REBulkString]
    new RESPParser("$0\r\n\r\n").BulkStringInput.run().resultType[REBulkString]
    new RESPParser("$3\r\nbar\r\n").BulkStringInput.run().resultType[REBulkString]
    new RESPParser("$10\r\nfoo\r\n").BulkStringInput.run() shouldBe a[Failure[_]]

    new RESPParser("00000000$-1\r\n").BulkStringInput.run().resultType[RENullString.type]
    new RESPParser("00000000$1\r\na\r\n").BulkStringInput.run().resultType[REBulkString]
    new RESPParser("00000000$12\r\nfoo b4r  ba$\r\n").BulkStringInput.run().resultType[REBulkString]
    new RESPParser("00000000$0\r\n\r\n").BulkStringInput.run().resultType[REBulkString]
    new RESPParser("00000000$3\r\nbar\r\n").BulkStringInput.run().resultType[REBulkString]
    new RESPParser("00000000$10\r\nfoo\r\n").BulkStringInput.run() shouldBe a[Failure[_]]
  }

  it should "parse SimpleString" in {
    new RESPParser("+OK\r\n").SimpleStringInput.run().resultType[RESimpleString.type]

    new RESPParser("00000000+OK\r\n").SimpleStringInput.run().resultType[RESimpleString.type]
  }

  it should "parse SimpleString PONG" in {
    new RESPParser("+PONG\r\n").PongSimpleStringInput.run().resultType[REPong.type]
  }

  it should "parse Integer" in {
    new RESPParser(":8\r\n").IntegerInput.run().resultType[REInt]

    new RESPParser("00000000:8\r\n").IntegerInput.run().resultType[REInt]
  }

  it should "parse Error" in {
    new RESPParser("-ERR unknown command 'foobar'\r\n").ErrorInput.run().resultType[REError]
    new RESPParser("-WRONGTYPE Operation against a key holding the wrong kind of value\r\n").ErrorInput.run().resultType[REError]

    new RESPParser("00000000-ERR unknown command 'foobar'\r\n").ErrorInput.run().resultType[REError]
    new RESPParser("00000000-WRONGTYPE Operation against a key holding the wrong kind of value\r\n").ErrorInput.run().resultType[REError]
  }

  it should "parse Array" in {
    new RESPParser("*0\r\n").ArrayInput.run().resultType[REArray]
    new RESPParser("*2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n").ArrayInput.run().resultType[REArray]
    new RESPParser("*3\r\n:1\r\n:2\r\n:3\r\n").ArrayInput.run().resultType[REArray]
    new RESPParser("*5\r\n:1\r\n:2\r\n:3\r\n:4\r\n$6\r\nfoobar\r\n").ArrayInput.run().resultType[REArray]
    new RESPParser("*3\r\n$3\r\nset\r\n$3\r\nfoo\r\n$3\r\nbar\r\n").ArrayInput.run().resultType[REArray]

    new RESPParser("0000000*0\r\n").ArrayInput.run().resultType[REArray]
    new RESPParser("0000000*2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n").ArrayInput.run().resultType[REArray]
    new RESPParser("0000000*3\r\n:1\r\n:2\r\n:3\r\n").ArrayInput.run().resultType[REArray]
    new RESPParser("0000000*5\r\n:1\r\n:2\r\n:3\r\n:4\r\n$6\r\nfoobar\r\n").ArrayInput.run().resultType[REArray]
    new RESPParser("0000000*3\r\n$3\r\nset\r\n$3\r\nfoo\r\n$3\r\nbar\r\n").ArrayInput.run().resultType[REArray]
  }

  it should "parse stream ID" in {
    new RESPParser("+1527712969546-0\r\n").StreamIdInput.run().resultType[REStreamId]
  }

  "Encoder" should "encode REInt" in {
    val encoder: Encoder[REInt] = Encoder()
    val value = 1
    encoder.encode(REInt(value)).right.value shouldEqual s":$value\r\n"
  }
}
