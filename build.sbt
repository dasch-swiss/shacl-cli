import com.typesafe.sbt.packager.docker.*
import sbt._
import sbt.Keys.version

import scala.language.postfixOps
import scala.sys.process._

addCommandAlias("fmt", "; all root/scalafmtSbt root/scalafmtAll; root/scalafixAll")

Global / scalaVersion      := "3.3.5"
Global / semanticdbEnabled := true
Global / semanticdbVersion := scalafixSemanticdb.revision

val ZioVersion = "2.1.19"

val JenaVersion          = "5.2.0" // should be aligned with the version topbraid-shacl uses
val TopbraidShaclVersion = "1.4.4"

// zio-test and friends
val zioTest    = "dev.zio" %% "zio-test"     % ZioVersion
val zioTestSbt = "dev.zio" %% "zio-test-sbt" % ZioVersion

// production dependencies
val zio           = "dev.zio"        %% "zio"                       % ZioVersion
val zioCli        = "dev.zio"        %% "zio-cli"                   % "0.7.2"
val jenaCore      = "org.apache.jena" % "jena-core"                 % JenaVersion
val topbraidShacl = "org.topbraid"    % "shacl"                     % TopbraidShaclVersion
val slf4j         = "dev.zio"        %% "zio-logging-slf4j2-bridge" % "2.5.0"

val prodDependencies = Seq(zio, zioCli, jenaCore, topbraidShacl, slf4j)
val testDependencies = Seq(zioTest, zioTestSbt).map(_ % Test)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

enablePlugins(
  JavaAppPackaging,
  DockerPlugin,
)

Compile / mainClass  := Some("swiss.dasch.shacl.cli.Main")
Docker / packageName := "daschswiss/shacl-cli"
dockerExposedPorts ++= Seq()

dockerBaseImage       := "eclipse-temurin:21-jre"
dockerBuildxPlatforms := Seq("linux/arm64/v8", "linux/amd64")
dockerUpdateLatest    := true

val gitCommit = ("git rev-parse HEAD" !!).trim
val gitBranch = Option("git rev-parse --abbrev-ref HEAD" !!)
  .map(_.trim)
  .filter(b => !(b == "main" || b == "HEAD"))
  .map(_.replace('/', '-'))
val gitVersion = ("git describe --tag --dirty --abbrev=7 --always  " !!).trim + gitBranch.fold("")("-" + _)

ThisBuild / version := gitVersion

val customScalacOptions = Seq(
  "-feature",
  "-unchecked",
  "-deprecation",
  "-Yresolve-term-conflict:package",
  "-Wvalue-discard",
  "-Xmax-inlines:64",
  "-Wunused:all",
  "-Xfatal-warnings",
)

lazy val root =
  Project(id = "root", file("."))
    .settings(
      name := "shacl-cli",
      libraryDependencies ++= testDependencies ++ prodDependencies,
      scalacOptions ++= customScalacOptions,
    )
