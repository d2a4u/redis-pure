package com.d2a4u.redispure.commands.connection

import cats.effect._
import com.d2a4u.redispure.RedisClient
import com.d2a4u.redispure.models._
import com.d2a4u.redispure.resp._

case class Ping[F[_]]()(implicit client: RedisClient[F], F: Effect[F]) extends BasicRESPCmd[F, REPong.type] {
  override val cmd: REArray = REArray("PING")
}
