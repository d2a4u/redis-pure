package com.d2a4u.redispure.commands.keys

import cats.implicits._
import cats.effect._
import com.d2a4u.redispure.clients.RedisClient
import com.d2a4u.redispure.models._
import com.d2a4u.redispure.resp._

case class Del[F[_]](keys: String*)(implicit client: RedisClient[F], F: Effect[F]) extends AsyncRESPCmd[F, REInt] {
  override val cmd: REArray = REArray("DEL") |+| RENeArray(keys.toArray.map(REString.apply))
}

case class Exists[F[_]](keys: String*)(implicit client: RedisClient[F], F: Effect[F]) extends AsyncRESPCmd[F, REInt] {
  override val cmd: REArray = REArray("EXISTS") |+| RENeArray(keys.toArray.map(REString.apply))
}
