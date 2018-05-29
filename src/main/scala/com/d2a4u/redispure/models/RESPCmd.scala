package com.d2a4u.redispure.models

import cats.effect._
import cats.implicits._
import com.d2a4u.redispure.RedisClient
import com.d2a4u.redispure.resp.{REArray, RESP, RESPParser}
import com.d2a4u.redispure.serialization.{Encoder, SerializationException}
import fs2.{Chunk, Stream}

trait FRun[F[_], T] {
  def run(): F[Either[Throwable, T]]
}

trait FSteam[F[_]] {
  def stream(): Stream[F, Chunk[Byte]]
}

trait RESPCmd[Out <: RESP] {
  def send[F[_]](
    request: REArray
  )(implicit encoder: Encoder[REArray],
    client: RedisClient,
    F: Effect[F]
  ): F[Either[Throwable, Out]] = {
    encoder.encode(request) match {
      case Right(cmd) =>
        val response = client.send[F](cmd).compile.toList
        F.map(response) { f =>
          val str = f.foldLeft(Array.empty[Byte])(_ ++ _.toArray[Byte])
          RESPParser(new String(str)).toEither.leftMap(SerializationException.apply).map(_.asInstanceOf[Out])
        }
      case Left(err) =>
        F.pure(err.asLeft)
    }
  }

  def sendStream[F[_]](
    request: REArray
  )(implicit encoder: Encoder[REArray],
    client: RedisClient,
    F: Effect[F]
  ): Either[SerializationException, Stream[F, Chunk[Byte]]] = {
    encoder.encode(request) match {
      case Right(cmd) =>
        client.send[F](cmd).asRight[SerializationException]

      case Left(err) =>
        err.asLeft[Stream[F, Chunk[Byte]]]
    }
  }
}

abstract class BasicRESPCmd[F[_], Out <: RESP](implicit client: RedisClient, F: Effect[F]) extends RESPCmd[Out] with FRun[F, Out] {
  def cmd: REArray

  override def run(): F[Either[Throwable, Out]] = {
    send[F](cmd)
  }
}
