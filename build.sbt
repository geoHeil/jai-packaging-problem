import java.io.{File, FileOutputStream}
import java.nio.file.{Files, Paths}

import sbtassembly.MergeStrategy
import sbtassembly.MergeStrategy.createMergeTarget

import scala.collection.mutable

name := "jaiPackagingProblems"
organization := "imagio"

scalaVersion := "2.11.8"

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfuture",
  "-Xlint:missing-interpolator",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-dead-code",
  "-Ywarn-unused"
)

fork := true

lazy val spark = "2.1.1"
lazy val geotools = "17.1"

resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"
resolvers += "osgeo" at "http://download.osgeo.org/webdav/geotools"
resolvers += "boundless" at "http://repo.boundlessgeo.com/main"
resolvers += "imageio" at "http://maven.geo-solutions.it"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % spark % "provided",
  "org.apache.spark" %% "spark-sql" % spark % "provided",
  "org.apache.spark" %% "spark-hive" % spark % "provided")
  .map(_.excludeAll(
    ExclusionRule(organization = "org.scalacheck"),
    ExclusionRule(organization = "org.scalactic"),
    ExclusionRule(organization = "org.scalatest")
  ))

libraryDependencies ++= Seq(
  "org.geotools" % "gt-main" % geotools,
  "org.geotools" % "gt-arcgrid" % geotools,
  "org.geotools" % "gt-process-raster" % geotools)
//  .map(_.excludeAll(
//    ExclusionRule(organization = "com.vividsolutions")
//  ))
// show sbt evicted
//libraryDependencies ++= Seq(
//  "org.datasyslab" % "geospark" % "0.7.0",
//  "org.datasyslab" % "babylon" % "0.2.0"
//).map(_.excludeAll(
//  ExclusionRule(organization = "org.geotools", artifact = "gt-main")
//))

//libraryDependencies ++= Seq(
//  "com.oracle" % "ojdbc8" % "12.2.0.1",
//  "com.holdenkarau" % "spark-testing-base_2.11" % s"2.1.0_0.6.0" % "test"
//)

run in Compile := Defaults.runTask(fullClasspath in Compile, mainClass.in(Compile, run), runner.in(Compile, run)).evaluated

assemblyMergeStrategy in assembly := {
  // http://stackoverflow.com/questions/43910006/geotools-jai-fatjar-causing-problems-in-native-dependencies
//  case PathList("org", "datasyslab", ps@_*) if ps.contains("showcase") => MergeStrategy.discard
//  case PathList("META-INF", xs@_*) =>
//      xs match {
//        case ("MANIFEST.MF" :: Nil) => MergeStrategy.discard
//        case ("NOTICE.txt" :: Nil) => MergeStrategy.discard
//        case ("LICENSE.txt" :: Nil) => MergeStrategy.discard
//        case ("NOTES.txt" :: Nil) => MergeStrategy.discard
////   Concatenate everything in the services directory to keep GeoTools happy.
//        case ("services" :: _ :: Nil) => MergeStrategy.concat
//        // Concatenate these to keep JAI happy.
//        case ("javax.media.jai.registryFile.jai" :: Nil) | ("registryFile.jai" :: Nil) | ("registryFile.jaiext" :: Nil) =>
//          MergeStrategy.first
//        case (name :: Nil) => {
//          // Must exclude META-INF/*.([RD]SA|SF) to avoid "Invalid signature file digest for Manifest main attributes" exception.
//          if (name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".SF")) {
//            MergeStrategy.discard
//          }
//          else {
//            MergeStrategy.deduplicate
//          }
//        }
//    // TODO get rid of merge first and find proper strategy which works
//        case _ => MergeStrategy.first
//      }
//    case _ => MergeStrategy.first

  case PathList("org", "datasyslab", ps@_*) if ps.contains("showcase") => MergeStrategy.discard
  case PathList("org", "datasyslab", xs@_*) => MergeStrategy.deduplicate
  case PathList("com", "esotericsoftware", xs@_*) => MergeStrategy.last
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case PathList("META-INF", "LICENSE.txt") => MergeStrategy.discard
  case PathList("META-INF", "NOTICE.txt") => MergeStrategy.discard
  case PathList("oracle", xs@_*) => MergeStrategy.deduplicate
  case PathList("org", "opengis", xs@_*) => MergeStrategy.deduplicate
  case PathList("org", "jdom", xs@_*) => MergeStrategy.deduplicate
  case PathList("META-INF", xs@_*) =>
        xs match {
          case ("javax.media.jai.registryFile.jai" :: Nil) | ("registryFile.jai" :: Nil) | ("registryFile.jaiext" :: Nil) =>
                    MergeStrategy.concat
          case _ => MergeStrategy.first
        }
  case _ => MergeStrategy.deduplicate
}

assemblyShadeRules in assembly := Seq(
  ShadeRule.rename("com.google.**" -> "shadedguava.@1").inAll
)

test in assembly := {}

mainClass := Some("geo.JAIParsingProblemLocalFine")