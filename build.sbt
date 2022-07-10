import Dependencies._

ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "lamedh"

lazy val root = (project in file("."))
  .settings(
    name := "forex",
    Defaults.itSettings,
    libraryDependencies ++= Seq(
      compilerPlugin(kindProjector cross CrossVersion.full),
      cats,
      catsEffect,
      circeCore,
      circeGeneric,
      http4sDsl,
      http4sCirce,
      http4sServer,
      http4sClient,
      log4cats,
      logback % Runtime,
      enumeratum,
      pureConfig,
      weaverTest,
      weaverCheck
    ) ++ List(contextApplied, betterMonadic).map(compilerPlugin(_)),
    testFrameworks += new TestFramework("weaver.framework.TestFramework")
  )
  .configs(IntegrationTest)
  .enablePlugins(DockerPlugin, AshScriptPlugin)
  .settings(
    dockerBaseImage := "openjdk:8-alpine",
    dockerExposedPorts ++= Seq(9090),
    dockerUpdateLatest := true
  )
