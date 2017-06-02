package geo

import org.apache.spark.SparkConf
import org.apache.spark.input.PortableDataStream
import org.apache.spark.sql.SparkSession
import org.geotools.gce.arcgrid.ArcGridReader

object SparkDoesNotWork extends App {

  val conf: SparkConf = new SparkConf()
    .setMaster("local[*]")
    .setAppName("myapp")
    .set("spark.driver.memory", "12G")
    .set("spark.default.parallelism", "12")

  val spark = SparkSession
    .builder()
    .config(conf)
    .getOrCreate()

  import spark.implicits._

  val df = spark.sparkContext
    .binaryFiles("data/", 12)
    .mapPartitions(mapToSimpleTypesBytes)
    .toDS

  df.show
  print(df.count)

  def mapToSimpleTypesBytes(iterator: Iterator[(String, PortableDataStream)]): Iterator[GeometryId] = iterator.flatMap(r => {
    val readRaster = new ArcGridReader(r._2.open).read(null)
    Parser.parse(readRaster)
  })
}
