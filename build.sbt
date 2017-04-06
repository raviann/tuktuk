name := """tuktuk"""

version := "1.0"

scalaVersion := "2.11.7"

retrieveManaged := true

libraryDependencies ++= {
  val AkkaHttpVersion   = "2.4.7"
  Seq(
    "com.typesafe.akka" %% "akka-http-experimental" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit-experimental" % "2.4.2-RC3",
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.2",
    "org.scalatest"     %% "scalatest"                 % "2.2.6",
    "com.github.etaty" %% "rediscala" % "1.6.0",
    "io.spray" %% "spray-routing" % "1.3.2",
    "io.spray" %% "spray-can" % "1.3.2",
    "io.spray" %% "spray-json" % "1.3.2",
    "io.spray" %% "spray-client" % "1.3.2",
    "org.mongodb.scala" %% "mongo-scala-driver" % "2.0.0",
    "org.slf4j" % "slf4j-simple" % "1.6.4"
  )
}



fork in run := true
