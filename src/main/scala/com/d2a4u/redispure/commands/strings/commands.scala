package com.d2a4u.redispure.commands.strings

import cats.implicits._
import cats.effect.Effect
import com.d2a4u.redispure.clients.RedisClient
import com.d2a4u.redispure.models._
import com.d2a4u.redispure.resp._
import com.d2a4u.redispure.serialization.Encoder

/**
  * Refer to https://redis.io/commands#string for more details about command usages
  */
case class Append[F[_], T](key: String, value: T)(implicit encoder: Encoder[T], client: RedisClient[F], F: Effect[F])
    extends AsyncRESPCmd[F, REInt] {
  override val cmd: REArray = REArray("APPEND", this.key, encoder.encode(this.value))
}

case class BitCount[F[_]](key: String, startEnd: Option[(Int, Int)] = None)(
  implicit client: RedisClient[F],
  F: Effect[F]
) extends AsyncRESPCmd[F, REInt] {
  override val cmd: REArray = startEnd match {
    case Some((start, end)) =>
      REArray("BITCOUNT", key, start.toString, end.toString)

    case None =>
      REArray("BITCOUNT", key)
  }
}

sealed trait BitFieldSubCmd {
  def cmd: REArray
}

case class BitFieldGet(typ: String, offset: Int) extends BitFieldSubCmd {
  override val cmd: REArray = REArray("GET", typ, offset.toString)
}

case class BitFieldSet[T](typ: String, offset: Int, value: T)(implicit encoder: Encoder[T]) extends BitFieldSubCmd {
  override val cmd: REArray = REArray("SET", typ, offset.toString, encoder.encode(value))
}

case class BitFieldIncrBy[T](typ: String, offset: Int, value: T)(implicit encoder: Encoder[T]) extends BitFieldSubCmd {
  override val cmd: REArray = REArray("INCRBY", typ, offset.toString, encoder.encode(value))
}

case object BitFieldOverflowWrap extends BitFieldSubCmd {
  override val cmd: REArray = REArray("OVERFLOW", "WRAP")
}
case object BitFieldOverflowSat extends BitFieldSubCmd {
  override val cmd: REArray = REArray("OVERFLOW", "SAT")
}
case object BitFieldOverflowFail extends BitFieldSubCmd {
  override val cmd: REArray = REArray("OVERFLOW", "FAIL")
}

case class BitField[F[_]](key: String, subCommands: BitFieldSubCmd*)(implicit client: RedisClient[F], F: Effect[F])
    extends AsyncRESPCmd[F, REArray] {
  override val cmd: REArray = {
    val subCmds = subCommands.map(_.cmd).reduce(_ |+| _)
    REArray("BITFIELD", key) |+| subCmds
  }
}

sealed trait BitopOp {
  def value: String
}
case object BitOpAnd extends BitopOp {
  override val value: String = "AND"
}
case object BitOpOr extends BitopOp {
  override val value: String = "OR"
}
case object BitOpXor extends BitopOp {
  override val value: String = "XOR"
}
case object BitOpNot extends BitopOp {
  override val value: String = "NOT"
}

case class BitOp[F[_]](op: BitopOp, destKey: String, srcKeys: String*)(implicit client: RedisClient[F], F: Effect[F])
    extends AsyncRESPCmd[F, REInt] {
  override val cmd: REArray = REArray("BITOP", op.value, destKey) |+| RENeArray(srcKeys.toArray.map(REString.apply))
}

case class BitPos[F[_]](key: String, start: Int, end: Option[Int] = None)(implicit client: RedisClient[F], F: Effect[F])
    extends AsyncRESPCmd[F, REInt] {
  override val cmd: REArray = {
    end match {
      case Some(endPos) =>
        REArray("BITPOS", this.key, start.toString, endPos.toString)

      case None =>
        REArray("BITPOS", this.key, start.toString)
    }
  }
}

case class Decr[F[_]](key: String)(implicit client: RedisClient[F], F: Effect[F]) extends AsyncRESPCmd[F, REInt] {
  override val cmd: REArray = REArray("DECR", this.key)
}

case class DecrBy[F[_]](key: String, decrement: Int)(implicit client: RedisClient[F], F: Effect[F])
    extends AsyncRESPCmd[F, REInt] {
  override val cmd: REArray = REArray("DECRBY", this.key, decrement.toString)
}

case class Get[F[_]](key: String)(implicit client: RedisClient[F], F: Effect[F]) extends AsyncRESPCmd[F, REBulkString] {
  override val cmd: REArray = REArray("GET", this.key)
}

case class GetBit[F[_]](key: String, offset: Int)(implicit client: RedisClient[F], F: Effect[F])
    extends AsyncRESPCmd[F, REInt] {
  override val cmd: REArray = REArray("GETBIT", this.key, offset.toString)
}

case class GetRange[F[_]](key: String, start: Int, end: Int)(implicit client: RedisClient[F], F: Effect[F])
    extends AsyncRESPCmd[F, REBulkString] {
  override val cmd: REArray = REArray("GETRANGE", this.key, start.toString, end.toString)
}

case class GetSet[F[_], T](key: String, value: T)(implicit encoder: Encoder[T], client: RedisClient[F], F: Effect[F])
    extends AsyncRESPCmd[F, RESimpleString.type] {
  override val cmd: REArray = REArray("GETSET", this.key, encoder.encode(this.value))
}

case class Incr[F[_]](key: String)(implicit client: RedisClient[F], F: Effect[F]) extends AsyncRESPCmd[F, REInt] {
  override val cmd: REArray = REArray("INCR", this.key)
}

case class IncrBy[F[_]](key: String, increment: Int)(implicit client: RedisClient[F], F: Effect[F])
    extends AsyncRESPCmd[F, REInt] {
  override val cmd: REArray = REArray("INCRBY", this.key, increment.toString)
}

case class IncrByFloat[F[_]](key: String, increment: Double)(implicit client: RedisClient[F], F: Effect[F])
    extends AsyncRESPCmd[F, REBulkString] {
  override val cmd: REArray = REArray("INCRBYFLOAT", this.key, String.valueOf(increment))
}

case class MGet[F[_]](key: String, keys: String*)(implicit client: RedisClient[F], F: Effect[F])
    extends AsyncRESPCmd[F, REBulkString] {
  override val cmd: REArray = REArray("MGET", this.key) |+| RENeArray(keys.toArray.map(REString.apply))
}

case class MSet[F[_], T](kv: (String, T)*)(implicit encoder: Encoder[T], client: RedisClient[F], F: Effect[F])
    extends AsyncRESPCmd[F, REBulkString] {
  override val cmd: REArray = REArray("MSET") |+| REArray(kv.flatMap {
    case (key, value) =>
      Array(key, encoder.encode(value))
  }: _*)
}

case class MSetNx[F[_], T](kv: (String, T), kvs: (String, T)*)(
  implicit encoder: Encoder[T],
  client: RedisClient[F],
  F: Effect[F]
) extends AsyncRESPCmd[F, REInt] {
  override val cmd: REArray = REArray("MSETNX") |+| REArray((kv +: kvs).flatMap {
    case (key, value) =>
      Array(key, encoder.encode(value))
  }: _*)
}

case class PSetEx[F[_], T](key: String, milliseconds: Long, value: T)(
  implicit encoder: Encoder[T],
  client: RedisClient[F],
  F: Effect[F]
) extends AsyncRESPCmd[F, RESimpleString.type] {
  override val cmd: REArray = REArray("PSETEX", this.key, milliseconds.toString, encoder.encode(value))
}

case class Set[F[_], T](key: String, value: T, px: Option[Long] = None, nx: Boolean = false, xx: Boolean = false)(
  implicit encoder: Encoder[T],
  client: RedisClient[F],
  F: Effect[F]
) extends AsyncRESPCmd[F, RESimpleString.type] {
  override val cmd: REArray =
    RENeArray({
      val arr: Array[RESP] = Array(REString("SET"), REString(this.key), REString(encoder.encode(this.value)))
      val pxArray = px.map(p => arr :+ REString("PX") :+ REString(p.toString)).getOrElse(arr)
      if (nx && !xx)
        pxArray :+ REString("NX")
      else if (!nx && xx)
        pxArray :+ REString("XX")
      else
        pxArray
    })
}

object Set {
  def apply[F[_]](key: String, value: String)(implicit client: RedisClient[F], F: Effect[F]): Set[F, String] =
    Set[F, String](key, value, None, false, false)
}

case class SetBit[F[_]](key: String, offset: Int, value: Boolean)(implicit client: RedisClient[F], F: Effect[F])
    extends AsyncRESPCmd[F, REInt] {
  override val cmd: REArray = REArray("SETBIT", this.key, offset.toString, { if (value) 1 else 0 }.toString)
}

case class SetEx[F[_], T](key: String, seconds: Int, value: T)(
  implicit encoder: Encoder[T],
  client: RedisClient[F],
  F: Effect[F]
) extends AsyncRESPCmd[F, RESimpleString.type] {
  override val cmd: REArray = REArray("SETEX", this.key, seconds.toString, encoder.encode(value))
}

case class SetNx[F[_], T](key: String, value: T)(implicit encoder: Encoder[T], client: RedisClient[F], F: Effect[F])
    extends AsyncRESPCmd[F, REInt] {
  override val cmd: REArray = REArray("SETNX", this.key, encoder.encode(value))
}

case class SetRange[F[_], T](key: String, offset: Int, value: T)(
  implicit encoder: Encoder[T],
  client: RedisClient[F],
  F: Effect[F]
) extends AsyncRESPCmd[F, REInt] {
  override val cmd: REArray = REArray("SETRANGE", this.key, offset.toString, encoder.encode(value))
}

case class StrLen[F[_]](key: String)(implicit client: RedisClient[F], F: Effect[F]) extends AsyncRESPCmd[F, REInt] {
  override val cmd: REArray = REArray("STRLEN", this.key)
}
