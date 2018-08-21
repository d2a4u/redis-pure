package com.d2a4u.redispure

import java.net.InetSocketAddress
import java.nio.channels.AsynchronousChannelGroup

import cats.effect.Effect
import com.d2a4u.redispure.resp.RESP
import fs2.io.tcp
import fs2.io.tcp.Socket
import fs2.{Chunk, Pipe, Stream}

import scala.concurrent.ExecutionContext

case class RedisClient[F[_]](
  host: String,
  port: Int,
  timeout: Int = 5 //seconds
)(implicit AG: AsynchronousChannelGroup, ec: ExecutionContext) {

  /** TypeByte is a representation of Redis's RESP data type.
    * In RESP, the type of some data depends on the first byte:
    * For Simple Strings the first byte of the reply is "+"
    * For Errors the first byte of the reply is "-"
    * For Integers the first byte of the reply is ":"
    * For Bulk Strings the first byte of the reply is "$"
    * For Arrays the first byte of the reply is "*"
    *
    * NonEmptyArrayTypeByte represents an Array data type, this type
    * is special because we then need to read the data from socket
    * recurrsively because an array can have multiple types
    *
    * NonEmptySimpleTypeByte represents all other RESP type
    */
  sealed trait TypeByte
  case class NonEmptySimpleTypeByte(index: Int, value: Byte) extends TypeByte
  case class NonEmptyArrayTypeByte(index: Int) extends TypeByte
  case object EmptyTypeByte extends TypeByte

  val CRLF: Array[Byte] = Array(13, 10)

  private var socket: Socket[F] = _

  def send(cmd: String, bufferSize: Int = 128 * 1024)(implicit F: Effect[F]): Stream[F, Chunk[Byte]] = {
    for {
      address <- Stream(new InetSocketAddress(host, port))
      sock <- tcp.client[F](address)
      _ <- write(sock, cmd)
      firstLine <- nextLine(sock)
      values <- readAll(sock, firstLine, bufferSize)
    } yield {
      socket = sock
      values
    }
  }

  def close()(implicit F: Effect[F]): F[Unit] =
    if (socket != null) socket.close
    else F.unit

  private def write(socket: Socket[F], cmd: String): Stream[F, Unit] =
    Stream.eval(socket.write(Chunk.array(cmd.getBytes)))

  private def readAll(
    socket: Socket[F],
    firstLine: Chunk[Byte],
    bufferSize: Int = 128 * 1024
  ): Stream[F, Chunk[Byte]] = {
    extractTypeByte(firstLine) match {
      case NonEmptySimpleTypeByte(typeByteAt, charByte) =>
        readSimple(socket, firstLine, bufferSize)(typeByteAt, charByte)
      case _: NonEmptyArrayTypeByte =>
        readArray(socket, firstLine, bufferSize)
      case EmptyTypeByte =>
        Stream(Chunk.empty)
    }
  }

  private def readSimple(socket: Socket[F], firstLine: Chunk[Byte], bufferSize: Int)(
    index: Int,
    byte: Byte
  ): Stream[F, Chunk[Byte]] = {
    val numBytes = numberOfBytesToReadNext(firstLine)(index, byte)
    val readNext = {
      if (numBytes == 0) Stream(Chunk.empty).covary[F]
      else readN(socket, numBytes, bufferSize)
    }
    Stream(firstLine).covary[F] ++ readNext
  }

  private def readArray(socket: Socket[F], firstLine: Chunk[Byte], bufferSize: Int): Stream[F, Chunk[Byte]] = {
    val numElems = numberOfElemsToReadNext(firstLine)
    val readNext = if (numElems > 0) {
      val readElem: Pipe[F, Int, Chunk[Byte]] = { inStream =>
        inStream.flatMap { _ =>
          for {
            line <- nextLine(socket)
            values <- readAll(socket, line, bufferSize)
          } yield values
        }
      }
      Stream.range(0, numElems).covary[F].through(readElem)
    } else {
      Stream(Chunk.empty).covary[F]
    }
    Stream(firstLine).covary[F] ++ readNext
  }

  private def extractTypeByte(firstLine: Chunk[Byte]): TypeByte = {
    (for {
      index <- firstLine.indexWhere(RESP.AllTypeBytes.contains)
      typeByte = firstLine(index)
    } yield {
      if (typeByte == RESP.ArrayCharByte) NonEmptyArrayTypeByte(index)
      else NonEmptySimpleTypeByte(index, typeByte)
    }).getOrElse(EmptyTypeByte)
  }

  private def nextLine(socket: Socket[F]): Stream[F, Chunk[Byte]] = {
    val maybeFirstLine = for {
      line <- Stream.eval(socket.readN(1)).repeat.takeThrough {
        case Some(data) =>
          !data.head.contains(RESP.CRLF(0))

        case None => true
      } ++ Stream.eval(socket.readN(1))
    } yield line

    /* have to unwrap Chunk into array and wrap it back into Chunk
       here to be able read line as a whole since this line contains
       type and number of bytes to read next so it should be small,
       acceptable to read in memory for now */
    maybeFirstLine.fold(Chunk.empty[Byte]) { (acc, item) =>
      val toAdd = item.getOrElse(Chunk.empty).toArray
      Chunk.array(acc.toArray ++ toAdd)
    }
  }

  private def readN(socket: Socket[F], numBytes: Int, bufferSize: Int): Stream[F, Chunk[Byte]] = {
    val read = {
      if (bufferSize > numBytes) {
        Stream.eval(socket.readN(numBytes))
      } else {
        Stream.eval(socket.readN(bufferSize)).repeat.take(numBytes)
      }
    }
    read.map(_.getOrElse(Chunk.empty))
  }

  private def numberOfBytesToReadNext(firstLine: Chunk[Byte])(typeByteAt: Int, charByte: Byte): Int = {
    (for {
      foundCRByteAt <- firstLine.indexWhere(_ == RESP.CRLF(0))
    } yield {
      if (charByte == RESP.BulkStringCharByte) {
        val numBytes = new String(firstLine.toArray.slice(typeByteAt + 1, foundCRByteAt)).toInt
        if (numBytes >= 0) {
          // Read extra 2 bytes because bulk string ends with CRLF
          numBytes + 2
        } else 0
      } else {
        /* Simple String or Integer or ErrorChar has all info in 1 line, hence,
         * we do not need to read more data
         */
        0
      }
    }).getOrElse(0)
  }

  private def numberOfElemsToReadNext(firstLine: Chunk[Byte]): Int = {
    (for {
      typeByteAt <- firstLine.indexWhere(_ == RESP.ArrayCharByte)
      foundCRByteAt <- firstLine.indexWhere(_ == RESP.CRLF(0))
    } yield {
      new String(firstLine.toArray.slice(typeByteAt + 1, foundCRByteAt)).toInt
    }).getOrElse(0)
  }
}
