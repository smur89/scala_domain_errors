import sbt._

object Dependencies {

  object versions {
    val cats = "2.5.0"
    val `cats-effect` = "2.4.1"
    val `cats-tagless` = "0.14.0"
    val `cats-testing` = "0.5.4"
    val circe = "0.13.0"
    val doobie = "0.10.0"
    val enumeratumCirce = "1.7.0"
    val flyway = "8.0.2"
    val http4s = "0.21.22"
    val kindProjector = "0.13.2"
    val odin = "0.11.0"
    val paradise = "2.1.1"
    val pureConfig = "0.17.0"
    val scalatest = "3.2.9"
  }

  object Plugin {
    val kindProjector = "org.typelevel" % "kind-projector" % versions.kindProjector cross CrossVersion.full
    val paradise = "org.scalamacros"    % "paradise"       % versions.paradise cross CrossVersion.full
  }

  val cats = Seq(
    "org.typelevel" %% "cats-core"           % versions.cats,
    "org.typelevel" %% "cats-tagless-macros" % versions.`cats-tagless`
  )

  val `cats-effect` = Seq("org.typelevel" %% "cats-effect" % versions.`cats-effect`)

  val circe: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % versions.circe)

  val doobie: Seq[ModuleID] = Seq(
    // Start with this one
    "org.tpolecat" %% "doobie-core"      % versions.doobie,
    // And add any of these as needed
    "org.tpolecat" %% "doobie-h2"        % versions.doobie, // H2 driver 1.4.200 + type mappings.
    "org.tpolecat" %% "doobie-hikari"    % versions.doobie, // HikariCP transactor.
    "org.tpolecat" %% "doobie-postgres"  % versions.doobie, // Postgres driver 42.2.23 + type mappings.
    "org.tpolecat" %% "doobie-quill"     % versions.doobie, // Support for Quill 3.7.2
    "org.tpolecat" %% "doobie-specs2"    % versions.doobie % Test, // Specs2 support for typechecking statements.
    "org.tpolecat" %% "doobie-scalatest" % versions.doobie % Test // ScalaTest support for typechecking statements.
  )

  val enumeratumCirce = Seq("com.beachape" %% "enumeratum-circe" % versions.enumeratumCirce)

  val flyway = Seq("org.flywaydb" % "flyway-core" % versions.flyway)

  val http4s: Seq[ModuleID] = Seq(
    "org.http4s" %% "http4s-blaze-server",
    "org.http4s" %% "http4s-blaze-client",
    "org.http4s" %% "http4s-circe",
    "org.http4s" %% "http4s-dsl"
  ).map(_ % versions.http4s)

  val logger: Seq[ModuleID] = Seq("com.github.valskalla" %% "odin-core").map(_ % versions.odin)

  val pureconfig: Seq[ModuleID] = Seq("com.github.pureconfig" %% "pureconfig" % versions.pureConfig)

  val tests: Seq[ModuleID] = Seq(
    "org.scalatest"  %% "scalatest"                     % versions.scalatest,
    "com.codecommit" %% "cats-effect-testing-scalatest" % versions.`cats-testing`
  ).map(_ % Test)

}
