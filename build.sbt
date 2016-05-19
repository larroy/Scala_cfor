lazy val commonSettings = Seq(
  version := "0.3",
  organization := "com.larroy.openquant",
  name := "Cfor",
  scalaVersion := "2.11.7",
  scalacOptions := Seq(
    "-target:jvm-1.8",
    "-unchecked",
    "-deprecation",
    "-feature",
    "-encoding", "utf8",
    "-Xlint"

  ),

  // Sonatype publishing
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  autoScalaLibrary := false,
  autoScalaLibrary in test := false,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra := (
    <url>https://github.com/openquant</url>
    <licenses>
      <license>
        <name>MIT</name>
        <url>http://opensource.org/licenses/MIT</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>https://github.com/larroy/Scala_cfor</url>
      <connection>scm:git@github.com:larroy/Scala_cfor.git</connection>
    </scm>
    <developers>
      <developer>
        <id>larroy</id>
        <name>Pedro Larroy</name>
        <url>https://github.com/larroy</url>
      </developer>
    </developers>
  )
)

lazy val commonDependencies = Seq(
)

lazy val testDependencies = Seq(
  "org.specs2" %% "specs2" % "3.+" % "test"
)

lazy val main = project.in(file("."))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= commonDependencies)
  .settings(libraryDependencies ++= testDependencies)
  .settings(libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value)

