package geo

import java.io.File
import java.nio.file.{ Files, Paths }

import com.vividsolutions.jts.io.WKTWriter
import org.geotools.gce.arcgrid.ArcGridReader
import org.geotools.process.raster.PolygonExtractionProcess

import scala.collection.JavaConversions._
import scala.collection.generic.Growable

case class GeometryId(idPath: String, db: Double, geo: String)

object JAIParsingProblemLocalFine extends App {

  val extractor = new PolygonExtractionProcess()
  val writer = new WKTWriter()

  val filePath = "data/dummy2.asc"
  println(s"mypath: ${filePath}")

  println("####################")
  Files.readAllLines(Paths.get(filePath)).toSet.foreach(println)
  println("####################")

  val resultParsing = parseAsciiFile(new File(filePath))
  print(s" ### file ${filePath} with ${resultParsing.length} polygons")
  resultParsing.foreach(println)

  def parseAsciiFile(file: File): Seq[GeometryId] with Growable[GeometryId] = {
    println(file.getAbsolutePath)
    val readRaster = new ArcGridReader(file).read(null)
    Parser.parse(readRaster)
  }
}