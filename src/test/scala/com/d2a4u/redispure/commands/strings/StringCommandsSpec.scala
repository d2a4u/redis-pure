package com.d2a4u.redispure.commands.strings

import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors

import cats.effect.IO
import com.d2a4u.redispure.clients.AsyncRedisClient
import com.d2a4u.redispure.commands.strings.{Set => RSet}
import com.d2a4u.redispure.commands.server.FlushAll
import com.d2a4u.redispure.resp.{REInt, RENeArray, REString}
import monix.execution.Scheduler.Implicits.{global => sglobal}
import org.scalatest._

class StringCommandsSpec extends FlatSpec with Matchers with EitherValues with BeforeAndAfterEach {

  val RedisPort = 6379
  val es = Executors.newCachedThreadPool()
  implicit val acg = AsynchronousChannelGroup.withThreadPool(es)
  implicit val client = AsyncRedisClient[IO]("localhost", RedisPort)

  override def beforeEach(): Unit = {
    super.beforeEach()
    FlushAll[IO](false).run().unsafeRunSync()
  }

  "APPEND command" should "append values to a key" in {
    /*  redis> EXISTS mykey
     *  (integer) 0
     *  redis> APPEND mykey "Hello"
     *  (integer) 5
     *  redis> APPEND mykey " World"
     *  (integer) 11
     *  redis> GET mykey
     *  "Hello World"
     *  redis>
     */
    val Hello = "Hello"
    val World = " World"
    val hello = Append[IO, String]("mykey", Hello)
    val world = Append[IO, String]("mykey", World)
    val get = Get[IO]("mykey")

    val cmdResult = for {
      _ <- hello.run()
      _ <- world.run()
      result <- get.run()
    } yield result

    cmdResult.unsafeRunSync().right.value shouldEqual REString(Hello + World)
  }

  "BITCOUNT command" should "count the number of set bits in a string" in {
    /*  redis> SET mykey "foobar"
     *  "OK"
     *  redis> BITCOUNT mykey
     *  (integer) 26
     *  redis> BITCOUNT mykey 0 0
     *  (integer) 4
     *  redis> BITCOUNT mykey 1 1
     *  (integer) 6
     *  redis>
     */
    val Value = "foobar"
    val bitsValue = Value.getBytes.map(_.toBinaryString).flatMap(_.toCharArray.map(_.toString.toInt))
    val bits00Value = Value.getBytes.map(_.toBinaryString).apply(0).toCharArray.map(_.toString.toInt)
    val bits11Value = Value.getBytes.map(_.toBinaryString).apply(1).toCharArray.map(_.toString.toInt)
    val set = RSet[IO, String]("mykey", Value)
    val bitcount = BitCount[IO]("mykey")
    val bitcount00 = BitCount[IO]("mykey", Some(0, 0))
    val bitcount11 = BitCount[IO]("mykey", Some(1, 1))

    val cmdResult = for {
      _ <- set.run()
      bcAll <- bitcount.run()
      bc00 <- bitcount00.run()
      bc11 <- bitcount11.run()
    } yield {
      for {
        noParamBC <- bcAll
        paramBC00 <- bc00
        paramBC11 <- bc11
      } yield (noParamBC, paramBC00, paramBC11)
    }

    cmdResult.unsafeRunSync().right.value match {
      case (REInt(bcAll), REInt(bc00), REInt(bc11)) =>
        bcAll shouldEqual bitsValue.count(_ == 1)
        bc00 shouldEqual bits00Value.count(_ == 1)
        bc11 shouldEqual bits11Value.count(_ == 1)

      case _ =>
        fail("Testing success case")
    }
  }

  "BITFIELD command" should "addressing specific integer fields of varying bit widths and arbitrary non (necessary) aligned offset" in {
    /*  BITFIELD mykey INCRBY i8 100 1 GET u4 0
     *  1) (integer) 1
     *  2) (integer) 0
     */
    val bitFieldCmd = BitField[IO]("myKey", BitFieldIncrBy("i8", 100, 1), BitFieldGet("u4", 0))
    bitFieldCmd.run().unsafeRunSync().right.value match {
      case RENeArray(Array(REInt(1), REInt(0))) =>
        succeed

      case _ =>
        fail("Testing success case")
    }
  }

  "BITOP command" should "perform a bitwise operation between multiple keys (containing string values) and store the result in the destination key" in {
    /*  redis> SET key1 "foobar"
     *  "OK"
     *  redis> SET key2 "abcdef"
     *  "OK"
     *  redis> BITOP AND dest key1 key2
     *  (integer) 6
     *  redis> GET dest
     *  "`bc`ab"
     *  redis>
     */
    val cmdResult = for {
      _ <- RSet[IO]("key1", "foobar").run()
      _ <- RSet[IO]("key2", "abcdef").run()
      bitop <- BitOp[IO](BitOpAnd, "dest", "key1", "key2").run()
      result <- Get[IO]("dest").run()
    } yield {
      for {
        op <- bitop
        res <- result
      } yield (op, res)
    }

    cmdResult.unsafeRunSync().right.value shouldEqual (REInt(6), REString("`bc`ab"))
  }
}
