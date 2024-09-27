val scala3Version = "3.3.3"

lazy val root = project
  .in(file("."))
  .settings(
    name := "partraverse-example",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies += "org.scalameta" %% "munit" % "1.0.2" % Test,
    libraryDependencies += "org.typelevel" %% "cats-effect" % "3.5.4",
    libraryDependencies += "com.google.guava" % "guava" % "33.3.1-jre"
  )

scalacOptions += "-Wnonunit-statement"

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
