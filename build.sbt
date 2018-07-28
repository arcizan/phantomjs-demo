name := """phantomjs-demo"""

version := "1.0"

scalaVersion := "2.11.6"

// Change this to another test framework if you prefer
libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

libraryDependencies += "com.github.detro.ghostdriver" % "phantomjsdriver" % "1.1.0"

// libraryDependencies += "org.seleniumhq.selenium" % "selenium-java" % "2.44.0"
// libraryDependencies += "org.seleniumhq.selenium" % "selenium-remote-driver" % "2.44.0"

// Uncomment to use Akka
//libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.3.9"

