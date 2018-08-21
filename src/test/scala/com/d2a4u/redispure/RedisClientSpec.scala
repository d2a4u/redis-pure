package com.d2a4u.redispure

import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors

import cats.effect.IO
import com.d2a4u.redispure.commands.connection._
import com.d2a4u.redispure.resp._
import com.d2a4u.redispure.commands.strings._
import com.d2a4u.redispure.commands.keys._
import com.d2a4u.redispure.commands.server.FlushAll
import monix.execution.Scheduler.Implicits.{global => sglobal}
import org.scalatest._

class RedisClientSpec extends FlatSpec with Matchers with EitherValues with BeforeAndAfterEach {

  val RedisPort = 6379
  val es = Executors.newCachedThreadPool()
  implicit val acg = AsynchronousChannelGroup.withThreadPool(es)
  implicit val client = RedisClient[IO]("localhost", RedisPort)

  override def beforeEach(): Unit = {
    super.beforeEach()
    FlushAll[IO](false).run().unsafeRunSync()
  }

  "RedisClient" should "get PONG from sending PING command" in {
    val cmd = Ping[IO]()
    cmd.run().unsafeRunSync().right.value shouldEqual REPong
  }

  it should "get OK from sending SET command" in {
    val setCmd = Set[IO]("foo", "bar")
    setCmd.run().unsafeRunSync().right.value shouldEqual RESimpleString
  }

  it should "get value back from sending GET command" in {
    val setCmd = Set[IO]("foo", "bar")
    setCmd.run().unsafeRunSync()
    val getBarCmd = Get[IO]("foo")
    getBarCmd.run().unsafeRunSync().right.value shouldEqual REString("bar")
  }

  it should "get 1 from sending DEL command for 1 existing key and 1 nonexisting key" in {
    val setCmd = Set[IO]("foo", "bar")
    setCmd.run().unsafeRunSync()
    val delBarCmd = Del[IO]("foo", "nonexisting")
    delBarCmd.run().unsafeRunSync().right.value shouldEqual REInt(1)
  }

  it should "get null String from sending GET command for nonexisting key" in {
    val getNotExistCmd = Get[IO]("nonexisting")
    getNotExistCmd.run().unsafeRunSync().right.value shouldEqual RENullString
  }
}
