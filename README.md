# redis-pure

[![Build Status](https://travis-ci.org/d2a4u/redis-pure.svg?branch=master)](https://travis-ci.org/d2a4u/redis-pure)

Redis Pure is an redis client based on [FS2](https://github.com/functional-streams-for-scala/fs2)
and [cats-effect](https://github.com/typelevel/cats-effect).
The goal is to prodive non-blocking IO actions on Redis store but be flexible on
IO data type.

## Getting Started

Current, this library has not been released and the only way to use it at the
moment is to clone the project and build it locally.

### Prerequisites

- SBT 1.1.4
- Scala 2.12.*
- Java 8

### Installing

After cloning the project, run:

```scala
sbt publishLocal
```

to publish the library locally.

### Usage

Redis commands are encoded as type class `trait RESPCmd[RESP]` for example,
Ping command is `case class PingCmd[F[_]](implicit client: RedisClient, F: Effect[F]) extends RESPCmd[REPong.type]`.
Hence, we can use different data type such as `cats.effect.IO` or Monix’s `Task`.
For example:

- Using `cats.effect.IO`:
    ```scala
    val cmd1 = Ping[IO]()
    cmd1.run().unsafeRunSync()
    // Right(REPong)
    ```

- Using Monix’s `Task`:
    ```scala
    val cmd2 = Ping[Task]()
    cmd2.run.runAsync(
      new Callback[Either[Throwable, REPong.type]] {
      def onSuccess(value: Either[Throwable, REPong.type]): Unit =
        println(value)
      def onError(ex: Throwable): Unit =
        System.err.println(s"ERROR: ${ex.getMessage}")
      }
    )
    // Right(REPong)
    ```

## Running the tests

Some tests rely on a running `redis-server`, if you want to run tests locally,
please make sure that you have `redis-server` runs on port 6379. These tests can 
be run by `sbt test`

## License

This project is licensed under the MIT License - see the 
[LICENSE.md](LICENSE.md) file for details
