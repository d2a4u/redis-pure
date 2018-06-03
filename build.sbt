val commonSettings = Seq(
  organization := "d2a4u",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.12.6",
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  scalafmtOnCompile := true,
  resolvers += Resolver.sonatypeRepo("releases"),
  scalacOptions += "-Ypartial-unification"
)

lazy val core = (project in file("core")).settings(
  commonSettings ++
    Seq(
      name := "redis-pure-core",
      libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-core" % "1.1.0",
        "org.typelevel" %% "cats-effect" % "0.10",
        "org.parboiled" %% "parboiled" % "2.1.4",
        "co.fs2" %% "fs2-core" % "0.10.3",
        "co.fs2" %% "fs2-io" % "0.10.3",
        "io.monix" %% "monix-nio" % "0.0.3",
        "org.scalatest" %% "scalatest" % "3.0.1" % Test
      ),
      parallelExecution in Test := false
    )
)

lazy val benchmarkMacros = (project in file("benchmark-macros"))
  .settings(
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
    commonSettings ++
      Seq(
        name := "redis-pure-benchmark-macros"
      )
  )
  .dependsOn(core)

lazy val benchmark = (project in file("benchmark"))
  .settings(
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
    commonSettings ++
      Seq(
        name := "redis-pure-benchmark"
      )
  )
  .enablePlugins(JmhPlugin)
  .dependsOn(core, benchmarkMacros)

lazy val root = (project in file("."))
  .settings(
    commonSettings ++
      Seq(
        name := "redis-pure"
      )
  )
  .aggregate(core, benchmark)