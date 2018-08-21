package com.d2a4u.redispure.commands.server

import cats.effect.Effect
import com.d2a4u.redispure.RedisClient
import com.d2a4u.redispure.models._
import com.d2a4u.redispure.resp._

/**
  * Refer to https://redis.io/commands#server for more details about command usages
  */
case class FlushAll[F[_]](async: Boolean)(implicit client: RedisClient[F], F: Effect[F])
    extends BasicRESPCmd[F, REInt] {
  override val cmd: REArray =
    if (async) {
      REArray("FLUSHALL", "ASYNC")
    } else {
      REArray("FLUSHALL")
    }
}

case class FlushDb[F[_]](async: Boolean)(implicit client: RedisClient[F], F: Effect[F]) extends BasicRESPCmd[F, REInt] {
  override val cmd: REArray =
    if (async) {
      REArray("FLUSHDB", "ASYNC")
    } else {
      REArray("FLUSHDB")
    }
}
