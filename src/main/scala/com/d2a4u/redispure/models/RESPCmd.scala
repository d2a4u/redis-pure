package com.d2a4u.redispure.models

import cats.effect._
import com.d2a4u.redispure.resp.{REArray, REError, RESP, RESPParser}
import com.d2a4u.redispure.serialization.Encoder
import com.d2a4u.redispure.{DecodingFailure, Error, RedisClient, RedisError}
import fs2.{Chunk, Stream}

trait FRun[F[_], T] {
  def run(): F[Either[Error, T]]
}

trait FStream[F[_]] {
  def stream(): Stream[F, Chunk[Byte]]
}

trait RESPCmd[Out <: RESP] {
  def send[F[_]](
    request: REArray
  )(implicit encoder: Encoder[REArray], client: RedisClient[F], F: Effect[F]): F[Either[Error, Out]] = {
    val response = client.send(encoder.encode(request)).compile.toList
    F.map(response) { f =>
      val str = f.foldLeft(Array.empty[Byte])(_ ++ _.toArray[Byte])
      RESPParser(new String(str)).toEither match {
        case Left(err) =>
          Left[Error, Out](DecodingFailure(err.getMessage))

        case Right(REError(typ, message)) =>
          Left[Error, Out](RedisError(typ, message))

        case Right(resp) =>
          Right[Error, Out](resp.asInstanceOf[Out])
      }
    }
  }
}

abstract class BasicRESPCmd[F[_], Out <: RESP](implicit client: RedisClient[F], F: Effect[F])
    extends RESPCmd[Out]
    with FRun[F, Out] {
  def cmd: REArray

  override def run(): F[Either[Error, Out]] =
    send(cmd)
}
