
name := """virtuoSOA"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  jdbc,
  javaEbean,
  javaWs,
  cache,
  "com.hazelcast" % "hazelcast" % "3.2.5",
  "com.hazelcast" % "hazelcast-cloud" % "3.2.5",
  "com.hazelcast" % "hazelcast-client" % "3.2.5"
)     
