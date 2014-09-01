
name := """virtuoSOA"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  jdbc,
  javaEbean,
  javaWs,
  cache
)     
