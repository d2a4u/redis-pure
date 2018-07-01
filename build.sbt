import sbtrelease.ReleaseStateTransformations._

name := "redis-pure"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "1.1.0",
  "org.typelevel" %% "cats-effect" % "0.10",
  "org.parboiled" %% "parboiled" % "2.1.4",
  "co.fs2" %% "fs2-core" % "0.10.3",
  "co.fs2" %% "fs2-io" % "0.10.3",
  "io.monix" %% "monix-nio" % "0.0.3",
  "org.scalatest" %% "scalatest" % "3.0.1" % Test
)

parallelExecution in Test := false

releaseProcess := Seq[ReleaseStep](
  runClean,
  runTest,
  checkSnapshotDependencies,
  inquireVersions,
  setReleaseVersion,
  commitReleaseVersion,
  publishArtifacts,
  setNextVersion,
  commitNextVersion,
  pushChanges
)

releaseCommitMessage := s"Setting version to ${(version in ThisBuild).value} [ci skip]"

scalafmtOnCompile := true
scalacOptions += "-Ypartial-unification"
