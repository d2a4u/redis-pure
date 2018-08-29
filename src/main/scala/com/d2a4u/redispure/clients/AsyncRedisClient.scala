package com.d2a4u.redispure.clients
import java.nio.channels.AsynchronousChannelGroup

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

case class AsyncRedisClient[F[_]](
  override val host: String,
  override val port: Int,
  readTimeout: FiniteDuration = 5.seconds
)(override implicit val AG: AsynchronousChannelGroup, override implicit val ec: ExecutionContext)
    extends RedisClient[F] {

  override val timeout: Option[FiniteDuration] = Some(readTimeout)
}
