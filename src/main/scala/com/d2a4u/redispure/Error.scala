package com.d2a4u.redispure

sealed abstract class Error extends Exception {
  final override def fillInStackTrace(): Throwable = this
}

final case class RedisError(`type`: String, message: String) extends Error {
  final override def getMessage: String = s"type: ${`type`}, message: $message"
}

case class DecodingFailure(message: String) extends Error {
  final override def getMessage: String = message
}
