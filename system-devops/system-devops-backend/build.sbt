name := """system-devops-backend"""

version := "0.0.1"

scalaVersion := "2.11.8"

fullResolvers := Seq(
  "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases",
  "sonatype-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "java-net" at "http://download.java.net/maven/2",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala, ElasticBeanstalkPlugin, BuildInfoPlugin)

libraryDependencies += jdbc
libraryDependencies += guice
libraryDependencies += filters
libraryDependencies += ws

libraryDependencies ++= Seq(
  "mysql" % "mysql-connector-java" % "5.1.41",
  "com.typesafe.play" %% "play-slick" % "3.0.0",
  "commons-lang" % "commons-lang" % "2.6",
  "com.google.code.gson" % "gson" % "2.8.1",
  "com.owlike" % "genson-scala_2.11" % "1.4",
  "commons-logging" % "commons-logging" % "1.2",
  "junit" % "junit" % "4.12" % "test",
  "log4j" % "log4j" % "1.2.17",
  "commons-io" % "commons-io" % "2.6",

  "io.swagger" %% "swagger-play2" % "1.6.0",
  "org.webjars" %% "webjars-play" % "2.6.3",
  "org.webjars" % "swagger-ui" % "3.19.0",

  //  "com.typesafe.slick" %% "slick-codegen" % "3.2.0"


"com.aliyun.oss" % "aliyun-sdk-oss" % "3.3.0"
)

