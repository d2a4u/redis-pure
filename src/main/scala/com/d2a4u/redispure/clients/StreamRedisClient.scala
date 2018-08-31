package com.d2a4u.redispure.clients
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousChannelGroup

import cats.effect.Effect
import fs2.{Chunk, Stream}
import fs2.io.tcp

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

case class StreamRedisClient[F[_]](override val host: String, override val port: Int, readTimeout: FiniteDuration)(
  override implicit val AG: AsynchronousChannelGroup,
  override implicit val ec: ExecutionContext
) extends RedisClient[F] {

  override val timeout: Option[FiniteDuration] = Some(readTimeout)

  def stream(cmd: Chunk[Byte], bufferSize: Int = 128 * 1024)(implicit F: Effect[F]): Stream[F, Chunk[Byte]] = {
    for {
      address <- Stream(new InetSocketAddress(host, port))
      sock <- tcp.client[F](address)
      _ <- Stream.eval(socket.write(cmd))
      firstLine <- nextLine(sock)
      values <- readAll(sock, firstLine, bufferSize)
    } yield {
      socket = sock
      values
    }
  }
}
