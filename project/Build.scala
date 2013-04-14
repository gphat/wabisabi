import sbt._
import Keys._

object WabisabiBuild extends Build {
  lazy val root = Project(
    id = "wabisabi",
    base = file("."),
    settings = Project.defaultSettings
  ).settings(
    scalaVersion := "2.10.0"
  )
}