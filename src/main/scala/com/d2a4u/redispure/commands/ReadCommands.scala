package com.d2a4u.redispure.commands

import cats.effect.Effect
import com.d2a4u.redispure.RedisClient
import com.d2a4u.redispure.models._
import com.d2a4u.redispure.resp._

case class Get[F[_]](
  key: String
)(implicit client: RedisClient, F: Effect[F]) extends BasicRESPCmd[F, REBulkString] {
  val cmd: REArray = REArray("GET", this.key)
}

case class Exists[F[_]](
  keys: String*
)(implicit client: RedisClient, F: Effect[F]) extends BasicRESPCmd[F, REInt] {
  val cmd: REArray = RENeArray(Array(REString("EXISTS")) ++ keys.map(REString.apply))
}

case class Bitcount[F[_]](
  key: String,
  start: Int,
  end: Int
)(implicit client: RedisClient, F: Effect[F]) extends BasicRESPCmd[F, REInt] {
  val cmd: REArray = REArray("BITCOUNT", key, start.toString, end.toString)
}

sealed trait BitopOp
case object AND extends BitopOp
case object OR extends BitopOp
case object XOR extends BitopOp
case object NOT extends BitopOp

case class Bitop[F[_]](
  op: BitopOp,
  destKey: String,
  srcKeys: String*
)(implicit client: RedisClient, F: Effect[F]) extends BasicRESPCmd[F, REInt] {
  override val cmd: REArray = RENeArray(Array(REString("BITOP"), REString(op.toString), REString(destKey)) ++ srcKeys.map(REString.apply))
}
