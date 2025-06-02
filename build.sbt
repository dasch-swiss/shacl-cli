import scala.collection.Seq

val scala3Version = "3.3.5"

val ZioVersion = "2.1.19"

val JenaVersion          = "5.2.0" // should be aligned with the version topbraid-shacl uses
val TopbraidShaclVersion = "1.4.4"

// zio-test and friends
val zioTest    = "dev.zio" %% "zio-test"     % ZioVersion
val zioTestSbt = "dev.zio" %% "zio-test-sbt" % ZioVersion

// production dependencies
val zio           = "dev.zio"        %% "zio"       % ZioVersion
val zioCli        = "dev.zio"        %% "zio-cli"   % "0.7.2"
val jenaCore      = "org.apache.jena" % "jena-core" % JenaVersion
val topbraidShacl = "org.topbraid"    % "shacl"     % TopbraidShaclVersion

val prodDependencies = Seq(zio, zioCli, jenaCore, topbraidShacl)
val testDependencies = Seq(zioTest, zioTestSbt).map(_ % Test)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

enablePlugins(
  JavaAppPackaging,
  DockerPlugin,
)

Compile / mainClass  := Some("swiss.dasch.shacl.cli.Main")
Docker / packageName := "daschswiss/shacl-cli"
dockerExposedPorts ++= Seq()

dockerBaseImage       := "eclipse-temurin:21-jre-noble"
dockerBuildxPlatforms := Seq("linux/arm64/v8", "linux/amd64")
dockerUpdateLatest    := true

lazy val root =
  Project(id = "root", file("."))
    .settings(
      name         := "shacl-cli",
      scalaVersion := scala3Version,
      version      := "0.1.0-SNAPSHOT",
      libraryDependencies ++= testDependencies ++ prodDependencies,
    )
