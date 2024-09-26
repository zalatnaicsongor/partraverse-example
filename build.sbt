val scala3Version = "3.5.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "partraverse-example",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test
  )

addCommandAlias("scalafmtFormatAll", "; scalafmtAll ; scalafmtSbt")
addCommandAlias("lint", "; scalafmtAll ; scalafmtSbt ; scalafixAll")
addCommandAlias(
  "validate",
  List(
    "clean",
    "scalafmtCheckAll",
    "scalafmtSbtCheck",
    "compile",
    "Test/compile",
    "scalafixAll --check",
    "test"
  ).mkString(";", "; ", "")
)