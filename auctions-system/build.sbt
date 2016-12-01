
name := "actors.auctions-system"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.11",
  "com.typesafe.akka" %% "akka-persistence" % "2.4.11",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.11" % "test",
  "com.typesafe.akka" %% "akka-remote" % "2.4.11",
//  "com.github.dnvriend" %% "akka-persistence-inmemory" % "1.3.14",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "org.iq80.leveldb" % "leveldb" % "0.7",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test")


EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource