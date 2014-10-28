organization := "wabisabi"

name := "wabisabi"

version := "2.0.10"

scalaVersion := "2.11.1"

crossScalaVersions := Seq("2.10.4")

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

libraryDependencies += "net.databinder.dispatch" %% "dispatch-core" % "0.11.0"

libraryDependencies += "org.clapper" %% "grizzled-slf4j" % "1.0.2"

libraryDependencies += "org.specs2" %% "specs2" % "2.3.11" % "test"

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.5" % "test"

libraryDependencies += "com.netaporter" %% "scala-uri" % "0.4.3"

publishTo := Some(Resolver.file("file",  new File( "/Users/gphat/src/mvn-repo/releases" )) )


