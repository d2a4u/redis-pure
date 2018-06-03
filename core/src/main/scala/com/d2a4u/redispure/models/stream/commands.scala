package com.d2a4u.redispure.models.stream

import cats.effect.Effect
import com.d2a4u.redispure.RedisClient
import com.d2a4u.redispure.models._
import com.d2a4u.redispure.resp.{REArray, REBulkString, REInt}

case class XKeyValue(key: String, value: String)

case class XaddCmd[F[_]](id: String, key: String, entries: XKeyValue*)(implicit client: RedisClient, F: Effect[F])
    extends RESPCmd[REBulkString]
    with FRun[F, REBulkString] {
  override def run(): F[Either[Throwable, REBulkString]] = {
    val input = Array("XADD", key, id) ++ entries.flatMap(kv => Array(kv.key, kv.value))
    val cmd = REArray(input: _*)
    send[F](cmd)
  }
}

object XaddCmd {
  def apply[F[_]](key: String, values: XKeyValue*)(implicit client: RedisClient, F: Effect[F]): XaddCmd[F] =
    XaddCmd("*", key, values: _*)
}

case class XlenCmd[F[_]](key: String)(implicit client: RedisClient, F: Effect[F])
    extends RESPCmd[REInt]
    with FRun[F, REInt] {
  override def run(): F[Either[Throwable, REInt]] = {
    val input = Array("XLEN", key)
    val cmd = REArray(input: _*)
    send[F](cmd)
  }
}
