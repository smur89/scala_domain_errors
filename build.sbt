import Dependencies._
import sbt.addCompilerPlugin

name := "fp_domain_errors"
version := "0.1"

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    organization := "smur89",
    name := "domain_errors",
    scalaVersion := "2.13.6",
    Compile / mainClass := Some("smur89.domain_errors.Main"),
    libraryDependencies ++=
      cats ++
        `cats-effect` ++
        circe ++
        doobie ++
        enumeratumCirce ++
        flyway ++
        http4s ++
        logger ++
        pureconfig ++
        tests,
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 =>
          Nil
        case _                       =>
          compilerPlugin(Plugin.paradise) :: Nil
      }
    },
    Compile / scalacOptions ++= "-Ywarn-macros:after" :: {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 =>
          "-Ymacro-annotations" :: Nil
        case _                       =>
          Nil
      }
    },
    addCompilerPlugin(Plugin.kindProjector)
  )
