name := """scalyr-logback"""
organization := "io.grhodes"
version := "git describe --tags --dirty --always".!!.stripPrefix("v").trim

scalaVersion := "2.11.8"

crossScalaVersions := Seq(scalaVersion.value, "2.10.6")

licenses += ("Apache-2.0", url("http://opensource.org/licenses/apache-2.0"))

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.7" % Configurations.Provided,
  "com.scalyr" % "scalyr-client" % "6.0.0"
)

