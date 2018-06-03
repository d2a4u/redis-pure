package com.d2a4u.redispure.commands

import cats.effect._
import com.d2a4u.redispure.RedisClient
import com.d2a4u.redispure.models._
import com.d2a4u.redispure.resp.{REPong, _}

case class Ping[F[_]](implicit client: RedisClient, F: Effect[F]) extends BasicRESPCmd[F, REPong.type] {
  override val cmd: REArray = REArray("PING")
}

case class Del[F[_]](keys: String*)(implicit client: RedisClient, F: Effect[F]) extends BasicRESPCmd[F, REInt] {
  override val cmd: REArray = RENeArray(Array(REString("DEL")) ++ keys.map(REString.apply))
}
