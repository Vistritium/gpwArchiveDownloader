name := "gpwDownloader"

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies += "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.9"

libraryDependencies += "joda-time" % "joda-time" % "2.7"

libraryDependencies += "org.apache.commons" % "commons-csv" % "1.1"

libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.4.1"

libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.3.10"

javaOptions += "-Xmx1G"
