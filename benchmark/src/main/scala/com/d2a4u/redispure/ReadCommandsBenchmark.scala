package com.d2a4u.redispure

import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors

import cats.effect.IO
import com.d2a4u.redispure.commands.{Get, Set}
import com.d2a4u.redispure.resp.REString
import org.openjdk.jmh.annotations.{Benchmark, Scope, State}
import scala.concurrent.ExecutionContext.Implicits.global

@State(Scope.Thread)
class ReadCommandsBenchmark {

//  val RedisPort = 6379
//  val es = Executors.newCachedThreadPool()
//  implicit val acg = AsynchronousChannelGroup.withThreadPool(es)

//  @GenerateN(1)
  @Benchmark
  def read(): String =
    s"hello world "
//    implicit val client = RedisClient("localhost", RedisPort)
//    val setCmd = Set[IO]("foo", "bar")
//    setCmd.run.unsafeRunSync()
//    val getBarCmd = Get[IO]("foo")
//    getBarCmd.run.unsafeRunSync().right.get.asInstanceOf[REString].value
}
