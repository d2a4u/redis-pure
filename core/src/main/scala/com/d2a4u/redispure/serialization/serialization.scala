package com.d2a4u.redispure.serialization

import java.nio.charset.StandardCharsets

import com.d2a4u.redispure.models._
import cats.implicits._
import com.d2a4u.redispure.resp._

abstract class SerializationException(message: String) extends Exception(message)

object SerializationException {
  def apply(err: Throwable): SerializationException = new SerializationException(err.getMessage) {}
}
case class EncoderException(message: String) extends SerializationException(message)
case class DecoderException(message: String) extends SerializationException(message)

case class Encoder[T](encode: T => Either[EncoderException, String])
case class Decoder[T](decode: String => Either[DecoderException, T])

object Encoder extends Encoders {
  def apply[T]()(implicit encoder: Encoder[T]): Encoder[T] = encoder
}

trait Encoders {
  implicit val strEnc: Encoder[String] = Encoder(_.asRight)
  implicit val intEnc: Encoder[Int] = Encoder(_.toString.asRight)
  implicit val doubleEnc: Encoder[Double] = Encoder(_.toString.asRight)
  implicit val longEnc: Encoder[Long] = Encoder(_.toString.asRight)

  implicit val reIntEnc: Encoder[REInt] = Encoder(t => s":${t.value}\r\n".asRight)
  implicit val reErrorEnc: Encoder[REError] = Encoder(t => s"-${t.`type`} ${t.message}\r\n".asRight)
  implicit val reBulkStringEnc: Encoder[REBulkString] = Encoder {
    case RENullString =>
      "$-1\r\n".asRight

    case str: REString =>
      s"$$${str.value.getBytes(StandardCharsets.UTF_8).length}\r\n${str.value}\r\n".asRight
  }
  implicit val reSimpleStringEnc: Encoder[RESimpleString.type] = Encoder(_ => s"+OK\r\n".asRight)
  implicit val reArrayEnc: Encoder[REArray] = Encoder(t => {
    def encode(array: REArray): Either[EncoderException, String] = {
      array match {
        case RENullArray =>
          "*-1\r\n".asRight

        case arr: RENeArray =>
          val parsedRESP = arr.elems.map {
            case r: REInt => implicitly[Encoder[REInt]].encode(r)
            case r: REError => implicitly[Encoder[REError]].encode(r)
            case r: REBulkString => implicitly[Encoder[REBulkString]].encode(r)
            case r: RESimpleString.type => implicitly[Encoder[RESimpleString.type]].encode(r)
            case r: REArray => encode(r)
            case r: RESP => EncoderException(s"Could not find encoder for $r").asLeft
          }
          val result: Either[EncoderException, Array[String]] =
            parsedRESP.foldRight(Right(Array.empty): Either[EncoderException, Array[String]]) { (e, acc) =>
              for {
                strArr <- acc.right
                str <- e.right
              } yield str +: strArr
            }
          result.map(arr => s"*${arr.length}\r\n" + arr.mkString)
      }
    }
    encode(t)
  })
}
