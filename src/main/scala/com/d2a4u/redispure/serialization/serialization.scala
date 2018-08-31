package com.d2a4u.redispure.serialization

import java.nio.charset.StandardCharsets

import com.d2a4u.redispure.DecodingFailure
import com.d2a4u.redispure.resp._
import fs2.Chunk

trait Encoder[T] {
  def encode(t: T): String
}

trait Decoder[T] {
  def decode(value: String): Either[DecodingFailure, T]
  def decode(value: Chunk[Byte]): Either[DecodingFailure, T]
}

object Encoder {
  def apply[T]()(implicit encoder: Encoder[T]): Encoder[T] = encoder

  def instance[T](func: T => String): Encoder[T] = new Encoder[T] {
    override def encode(t: T): String = func(t)
  }

  implicit val strEnc: Encoder[String] = Encoder.instance(identity)
  implicit val intEnc: Encoder[Int] = Encoder.instance(_.toString)
  implicit val doubleEnc: Encoder[Double] = Encoder.instance(_.toString)
  implicit val longEnc: Encoder[Long] = Encoder.instance(_.toString)

  implicit val reIntEnc: Encoder[REInt] = Encoder.instance(t => s":${t.value}\r\n")
  implicit val reErrorEnc: Encoder[REError] = Encoder.instance(t => s"-${t.`type`} ${t.message}\r\n")
  implicit val reBulkStringEnc: Encoder[REBulkString] = Encoder.instance {
    case RENullString =>
      "$-1\r\n"

    case str: REString =>
      s"$$${str.value.getBytes(StandardCharsets.UTF_8).length}\r\n${str.value}\r\n"
  }
  implicit val reSimpleStringEnc: Encoder[RESimpleString.type] = Encoder.instance(_ => s"+OK\r\n")
  implicit val reArrayEnc: Encoder[REArray] = Encoder.instance(t => {
    def encode(array: REArray): String = {
      array match {
        case RENullArray =>
          "*-1\r\n"

        case arr: RENeArray =>
          val parsedRESP = arr.elems.map {
            case r: REInt => Encoder[REInt].encode(r)
            case r: REError => Encoder[REError].encode(r)
            case r: REBulkString => Encoder[REBulkString].encode(r)
            case r: RESimpleString.type => Encoder[RESimpleString.type].encode(r)
            case r: REArray => encode(r)
            case _: RESP => ""
          }
          val result: Array[String] =
            parsedRESP.foldRight(Array.empty[String])(_ +: _)
          s"*${result.length}\r\n" + result.mkString
      }
    }
    encode(t)
  })
}
