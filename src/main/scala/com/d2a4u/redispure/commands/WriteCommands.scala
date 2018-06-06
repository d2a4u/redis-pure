package com.d2a4u.redispure.commands

import cats.effect._
import cats.implicits._
import com.d2a4u.redispure.RedisClient
import com.d2a4u.redispure.models._
import com.d2a4u.redispure.resp._
import com.d2a4u.redispure.serialization.{Encoder, SerializationException}

case class Set[F[_], T](key: String, value: T, px: Option[Long] = None, nx: Boolean = false, xx: Boolean = false)(
  implicit encoder: Encoder[T],
  client: RedisClient,
  F: Effect[F]
) extends RESPCmd[RESimpleString.type]
    with FRun[F, RESimpleString.type] {
  override def run(): F[Either[Throwable, RESimpleString.type]] = {
    val ioAction: F[Either[SerializationException, Either[Throwable, RESimpleString.type]]] =
      encoder.encode(this.value).traverse { encodedValue =>
        val reArray: REArray =
          RENeArray({
            val arr: Array[RESP] = Array(REString("SET"), REString(this.key), REString(encodedValue))
            val pxArray = px.map(p => arr :+ REString("PX") :+ REString(p.toString)).getOrElse(arr)
            if (nx && !xx)
              pxArray :+ REString("NX")
            else if (!nx && xx)
              pxArray :+ REString("XX")
            else
              pxArray
          })
        send[F](reArray)
      }
    ioAction.map(_.flatMap(identity))
  }
}

object Set {
  def apply[F[_]](key: String, value: String)(implicit client: RedisClient, F: Effect[F]): Set[F, String] =
    Set(key, value, None, false, false)
}

case class Append[F[_], T](key: String, value: T)(implicit encoder: Encoder[T], client: RedisClient, F: Effect[F])
    extends RESPCmd[REInt]
    with FRun[F, REInt] {
  override def run(): F[Either[Throwable, REInt]] = {
    val ioAction: F[Either[SerializationException, Either[Throwable, REInt]]] =
      encoder.encode(this.value).traverse { encodedValue =>
        val reArray: REArray = REArray("APPEND", this.key, encodedValue)
        send[F](reArray)
      }
    ioAction.map(_.flatMap(identity))
  }
}
