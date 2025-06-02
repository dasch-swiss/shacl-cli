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

val prodDependencies = Seq(
  zio,
  zioCli,
  jenaCore,
  topbraidShacl,
)

val testDependencies = Seq(
  zioTest,
  zioTestSbt,
).map(_ % Test)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

lazy val root = project
  .in(file("."))
  .settings(
    name         := "shacl-cli",
    version      := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= testDependencies ++ prodDependencies,
  )
