package com.d2a4u.redispure.models

import cats.effect._
import com.d2a4u.redispure.clients.RedisClient
import com.d2a4u.redispure.resp.{REArray, REError, RESP, RESPParser}
import com.d2a4u.redispure.serialization.Encoder
import com.d2a4u.redispure.{DecodingFailure, Error, RedisError}

trait FRun[F[_], T] {
  def run(): F[Either[Error, T]]
}

abstract class AsyncRESPCmd[F[_], Out <: RESP](implicit client: RedisClient[F], F: Effect[F]) extends FRun[F, Out] {

  def cmd: REArray

  def send[F[_]](
    request: REArray
  )(implicit encoder: Encoder[REArray], client: RedisClient[F], F: Effect[F]): F[Either[Error, Out]] = {
    val response = client.send(encoder.encode(request)).compile.toList
    F.map(response) { f =>
      val responseData = f.foldLeft(Array.empty[Byte])(_ ++ _.toArray[Byte])
      val str = new String(responseData)
      RESPParser(str).toEither match {
        case Left(err) =>
          throw err
          Left[Error, Out](DecodingFailure(err.getMessage))

        case Right(REError(typ, message)) =>
          Left[Error, Out](RedisError(typ, message))

        case Right(resp) =>
          Right[Error, Out](resp.asInstanceOf[Out])
      }
    }
  }

  override def run(): F[Either[Error, Out]] =
    send(cmd)
}
