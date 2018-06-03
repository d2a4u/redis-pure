package com.d2a4u.redispure.resp

import java.nio.charset.StandardCharsets

import org.parboiled2._

import scala.util.{Failure, Try}

// REdis Serializaion Protocol
sealed trait RESP

object RESP {
  val SimpleStringCharType = '+'
  val IntegerCharType = ':'
  val BulkStringCharType = '$'
  val ErrorCharType = '-'
  val ArrayCharType = '*'

  val SimpleStringCharByte = SimpleStringCharType.toByte
  val IntegerCharByte = IntegerCharType.toByte
  val BulkStringCharByte = BulkStringCharType.toByte
  val ErrorCharByte = ErrorCharType.toByte
  val ArrayCharByte = ArrayCharType.toByte

  val AllTypes = Set(SimpleStringCharType, IntegerCharType, BulkStringCharType, ErrorCharType, ArrayCharType)
  val AllTypeBytes = Set(SimpleStringCharByte, IntegerCharByte, BulkStringCharByte, ErrorCharByte, ArrayCharByte)
  val SimpleTypeBytes = Set(SimpleStringCharByte, IntegerCharByte, BulkStringCharByte, ErrorCharByte)

  // byte values of \r\n
  val CRLF: Array[Byte] = Array(13, 10)
}

case object RESPStream extends RESP

case object REPong extends RESP

case class REStreamId(millis: Long, increment: Int) extends RESP {
  val id: String = s"$millis-$increment"
}

case class REInt(value: Int) extends RESP

case class REError(`type`: String, message: String) extends RESP

sealed trait REBulkString extends RESP

case object RENullString extends REBulkString

case class REString(value: String) extends REBulkString

case object RESimpleString extends RESP

sealed trait REArray extends RESP

case class RENeArray(elems: Array[RESP]) extends REArray

case object RENullArray extends REArray

object REArray {
  def apply(items: String*): REArray = RENeArray(items.toArray.map(REString.apply))
}

class RESPParser(val input: ParserInput) extends Parser {

  import CharPredicate._

  def Digits: Rule1[Int] = rule(capture(oneOrMore(Digit)) ~> ((_: String).toInt))

  def BigDigits: Rule1[Long] = rule(capture(oneOrMore(Digit)) ~> ((_: String).toLong))

  def Word: Rule1[String] = rule(capture(oneOrMore(CharPredicate.Alpha)))

  def Words: Rule1[String] = rule(capture(zeroOrMore(CharPredicate.Printable)))

  def CRLF: Rule0 = rule("\r\n")

  def RuleStreamId: Rule1[REStreamId] = rule {
    RESP.SimpleStringCharType ~ BigDigits ~ '-' ~ Digits ~ CRLF ~> REStreamId.apply _
  }

  def RuleSimpleString: Rule1[RESimpleString.type] = rule {
    RESP.SimpleStringCharType ~ "OK" ~ CRLF ~ push(RESimpleString)
  }

  def RulePong: Rule1[REPong.type] = rule {
    RESP.SimpleStringCharType ~ "PONG" ~ CRLF ~ push(REPong)
  }

  def RuleInteger: Rule1[REInt] = rule {
    RESP.IntegerCharType ~ Digits ~ CRLF ~> REInt.apply _
  }

  def RuleError: Rule1[REError] = rule {
    RESP.ErrorCharType ~ Word ~ ' ' ~ Words ~ CRLF ~> ((typ: String, msg: String) => push(REError(typ, msg)))
  }

  def RuleBulkString: Rule1[REBulkString] = rule {
    RESP.BulkStringCharType ~ '-' ~ '1' ~ CRLF ~ push(RENullString) |
      RESP.BulkStringCharType ~ Digits ~ CRLF ~ Words ~ CRLF ~> (
        (
          num: Int,
          str: String
        ) => test(num == str.getBytes(StandardCharsets.UTF_8).length) ~ push(REString(str))
      )
  }

  def RuleArray: Rule1[REArray] = rule {
    RESP.ArrayCharType ~ '-' ~ CRLF ~ push(RENullArray) |
      RESP.ArrayCharType ~ Digits ~ CRLF ~ RuleRESPRepeats ~> (
        (
          num: Int,
          resps: Seq[RESP]
        ) => test(num == resps.size) ~ push(RENeArray(resps.toArray))
      )
  }

  def RuleRESP: Rule1[RESP] = rule(RuleBulkString | RuleInteger | RuleArray)

  def RuleRESPRepeats: Rule1[Seq[RESP]] = rule {
    zeroOrMore(RuleRESP)
  }

  def StreamIdInput = rule(zeroOrMore(Digit) ~ RuleStreamId ~ EOI)

  def BulkStringInput = rule(zeroOrMore(Digit) ~ RuleBulkString ~ EOI)

  def SimpleStringInput = rule(zeroOrMore(Digit) ~ RuleSimpleString ~ EOI)

  def PongSimpleStringInput = rule(zeroOrMore(Digit) ~ RulePong ~ EOI)

  def IntegerInput = rule(zeroOrMore(Digit) ~ RuleInteger ~ EOI)

  def ErrorInput = rule(zeroOrMore(Digit) ~ RuleError ~ EOI)

  def ArrayInput = rule(zeroOrMore(Digit) ~ RuleArray ~ EOI)
}

object RESPParser {
  def apply(inputString: String): Try[RESP] = {
    val parser = new RESPParser(inputString)
    inputString.trim.headOption match {
      case Some(RESP.IntegerCharType) =>
        parser.IntegerInput.run()

      case Some(RESP.SimpleStringCharByte) =>
        parser.SimpleStringInput.run().orElse(parser.StreamIdInput.run()).orElse(parser.PongSimpleStringInput.run())

      case Some(RESP.BulkStringCharByte) =>
        parser.BulkStringInput.run()

      case Some(RESP.ErrorCharByte) =>
        parser.ErrorInput.run()

      case Some(RESP.ArrayCharType) =>
        parser.ArrayInput.run()

      case _ =>
        Failure(new Exception("Invalid type identify character"))
    }
  }
}
