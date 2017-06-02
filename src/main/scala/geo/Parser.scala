package geo

import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.WKTWriter
import org.geotools.coverage.grid.GridCoverage2D
import org.geotools.process.raster.PolygonExtractionProcess

import scala.collection.JavaConversions._
import scala.collection.generic.Growable
import scala.collection.mutable

object Parser {

  @transient lazy val extractor = new PolygonExtractionProcess()
  @transient lazy val writer = new WKTWriter()

  def parse(rasterData: GridCoverage2D) = {

    // reclassify (bin values of raster

    val r1 = org.jaitools.numeric.Range.create(Integer.valueOf(-100), true, Integer.valueOf(-7), true)
    val r2 = org.jaitools.numeric.Range.create(Integer.valueOf(-7), true, Integer.valueOf(0), true)

    val classificationRanges = Seq(r1, r2)

    // store geometry as reusable WKT types
    val vectorizedFeatures = extractor.execute(rasterData, 0, true, null, null, classificationRanges, null).features

    val result: collection.Seq[GeometryId] with Growable[GeometryId] = mutable.Buffer[GeometryId]()

    while (vectorizedFeatures.hasNext) {
      val vectorizedFeature = vectorizedFeatures.next()
      val geomWKTLineString = vectorizedFeature.getDefaultGeometry match {
        case g: Geometry => writer.write(g)
      }
      val dbUserData = vectorizedFeature.getAttribute(1).asInstanceOf[Double]
      result += GeometryId("someIDNumber", dbUserData, geomWKTLineString)
    }
    result

  }

}
