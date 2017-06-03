# jai native dependencies on imageio packaging problem

I want to use the java library JAI to parse some spatial raster files. Unfortunately, I face strange classpath issues.
JAI only works when running via the build tool i.e. `sbt run`. 


To demonstrate the error in spark:

```
spark-submit --verbose \
        --class geo.SparkDoesNotWork \
	target/scala-2.11/jaiPackagingProblems-assembly-0.0.1.SNAPSHOT.jar
```

which will throw some sort of null validation of the underling native geos api

```
java.lang.IllegalArgumentException: The input argument(s) may not be null.
	at javax.media.jai.ParameterBlockJAI.getDefaultMode(ParameterBlockJAI.java:136)
	at javax.media.jai.ParameterBlockJAI.<init>(ParameterBlockJAI.java:157)
	at javax.media.jai.ParameterBlockJAI.<init>(ParameterBlockJAI.java:178)
	at org.geotools.process.raster.PolygonExtractionProcess.execute(PolygonExtractionProcess.java:171)
```

To demonstrate that it works just fine in the build tool

```
sbt run
# choose 2 (second main class)
```
you will see the following output
```
[info] +--------------------+----+--------------------+
[info] |              idPath|  db|                 geo|
[info] +--------------------+----+--------------------+
[info] |file:/Users/geohe...|12.0|POLYGON ((3 1, 3 ...|
[info] |file:/Users/geohe...|10.0|POLYGON ((2 2, 1 ...|
[info] |file:/Users/geohe...| 9.0|POLYGON ((2 1, 2 ...|
[info] +--------------------+----+--------------------+
[info]
[info] 3
```


Now for the weird part: assuming the classpath is broken, this means when not using spark but a regular
java / scala function to parse the files it should fail as well:

```
java -jar target/scala-2.11/jaiPackagingProblems-assembly-0.0.1.SNAPSHOT.jar
```
however, it will output the result just fine

```
 ### file data/dummy2.asc with 3 polygonsGeometryId(/Users/geoheil/Downloads/dummy/spark/data/dummy2.asc,2.0,POLYGON ((3 0, 4 0, 4 3, 0 3, 0 0, 2 0, 2 1, 3 1, 3 0), (1 1, 1 2, 2 2, 2 1, 1 1)))
GeometryId(/Users/geoheil/Downloads/dummy/spark/data/dummy2.asc,1.0,POLYGON ((2 2, 1 2, 1 1, 2 1, 2 2)))
GeometryId(/Users/geoheil/Downloads/dummy/spark/data/dummy2.asc,1.0,POLYGON ((2 1, 2 0, 3 0, 3 1, 2 1)))
```

Is spark messing with the classpaths?

> The main parsing functionality is the same in both cases!

Also I found out when setting
```
val vectorizedFeatures = extractor.execute(rasterData, 0, true, null, null, classificationRanges, null).features
```

to

```
val vectorizedFeatures = extractor.execute(rasterData, 0, true, null, null, null, null).features
```
spark will not mess with the classpath / not throw the error. However, this option is mandatory for my project.

The problem sounds a bit similar to http://git.net/ml/geoserver-development-geospatial-java/2013-10/msg00251.html but I could not solve it yet.

# edit
Meanwhile I packaged the same parsing functionality for NiFi but I can observe the same problems i.e. that something is null which should not be null.


https://stackoverflow.com/questions/7051603/jai-vendorname-null sounds somewhat similar, though I could not find missing netries in the manifest. https://stackoverflow.com/questions/9010202/adding-vendor-information-to-manifest-mf-using-sbt-assembly shows how to add some with sbt and https://github.com/geoHeil/jai-packaging-problem/commit/bedf62a93ac5c48052a30cd342aa22603d1d44db checks that they really are present - no luck there.
This are the contents of the manifest. This looks like everything which should be there - is there.
```
Manifest-Version: 1.0
Implementation-Title: myLib
Implementation-Version: 0.0.1.SNAPSHOT
Specification-Vendor: imagio
Specification-Title: jaiPackagingProblems
Implementation-Vendor-Id: imagio
Specification-Version: 0.0.1.SNAPSHOT
Main-Class: geo.JAIParsingProblemLocalFine
Implementation-Vendor: myCompany
```

thread safety:
geotools may not be threadsafe in general (http://docs.geotools.org/latest/userguide/library/main/repository.html) but from what I have tested for these specific features it seems to work fine. Also, when using a single threaded version of spark I still get the same error.

### connecting a debugger
To debug it I execute
```
export SPARK_SUBMIT_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005
spark-submit --verbose \
    --class geo.SparkDoesNotWork \
    --conf "spark.driver.extraJavaOptions=-XX:+UseG1GC" \
target/scala-2.11/jaiPackagingProblems-assembly-0.0.1.SNAPSHOT.jar
```
Which shows 
![null - not null validation](img/debugNotNull.png "null not null validation")

But digging deeper one will find (see below) which performs an lookup by name. This must be returning NULL
```
public ParameterBlockJAI(String operationName) {
        this((OperationDescriptor)JAI.getDefaultInstance().getOperationRegistry().getDescriptor(class$javax$media$jai$OperationDescriptor == null?(class$javax$media$jai$OperationDescriptor = class$("javax.media.jai.OperationDescriptor")):class$javax$media$jai$OperationDescriptor, operationName));
}
```

I think this must have something to do with my fat-jar /assembly merge strategy 

```
//   Concatenate everything in the services directory to keep GeoTools happy.
        case ("services" :: _ :: Nil) =>
          MergeStrategy.concat
        // Concatenate these to keep JAI happy.
        case ("javax.media.jai.registryFile.jai" :: Nil) | ("registryFile.jai" :: Nil) | ("registryFile.jaiext" :: Nil) =>
          MergeStrategy.concat
```

As some errors can be seen during startup

```
Error in registry file at line number #550
A descriptor is already registered against the name "Absolute" under registry mode "rendered"
Error in registry file at line number #551
A descriptor is already registered against the name "AddCollection" under registry mode "rendered"
Error in registry file at line number #552
A descriptor is already registered against the name "AddConst" under registry mode "rendered"
Error in registry file at line number #553
A descriptor is already registered against the name "AddConstToCollection" under registry mode "collection"
Error in registry file at line number #554
A descriptor is already registered against the name "Add" under registry mode "rendered"
Error in registry file at line number #555
A descriptor is already registered against the name "Affine" under registry mode "rendered"
Error in registry file at line number #556
A descriptor is already registered against the name "AndConst" under registry mode "rendered"
Error in registry file at line number #557
A descriptor is already registered against the name "And" under registry mode "rendered"
Error in registry file at line number #558
A descriptor is already registered against the name "AWTImage" under registry mode "rendered"
Error in registry file at line number #559
A descriptor is already registered against the name "BandCombine" under registry mode "rendered"
Error in registry file at line number #560
A descriptor is already registered against the name "BandMerge" under registry mode "rendered"
Error in registry file at line number #561
A descriptor is already registered against the name "BandSelect" under registry mode "rendered"
Error in registry file at line number #562
A descriptor is already registered against the name "Binarize" under registry mode "rendered"
Error in registry file at line number #563
A descriptor is already registered against the name "BMP" under registry mode "rendered"
Error in registry file at line number #564
A descriptor is already registered against the name "Border" under registry mode "rendered"
Error in registry file at line number #565
A descriptor is already registered against the name "BoxFilter" under registry mode "rendered"
Error in registry file at line number #566
A descriptor is already registered against the name "Clamp" under registry mode "rendered"
Error in registry file at line number #567
A descriptor is already registered against the name "ColorConvert" under registry mode "rendered"
Error in registry file at line number #568
A descriptor is already registered against the name "ColorQuantizer" under registry mode "rendered"
Error in registry file at line number #569
A descriptor is already registered against the name "Constant" under registry mode "rendered"
Error in registry file at line number #570
A descriptor is already registered against the name "Composite" under registry mode "rendered"
Error in registry file at line number #571
A descriptor is already registered against the name "Conjugate" under registry mode "rendered"
Error in registry file at line number #572
A descriptor is already registered against the name "Convolve" under registry mode "rendered"
Error in registry file at line number #573
A descriptor is already registered against the name "Crop" under registry mode "rendered"
Error in registry file at line number #574
A descriptor is already registered against the name "DCT" under registry mode "rendered"
Error in registry file at line number #575
A descriptor is already registered against the name "DFT" under registry mode "rendered"
Error in registry file at line number #576
A descriptor is already registered against the name "Dilate" under registry mode "rendered"
Error in registry file at line number #577
A descriptor is already registered against the name "Divide" under registry mode "rendered"
Error in registry file at line number #578
A descriptor is already registered against the name "DivideComplex" under registry mode "rendered"
Error in registry file at line number #579
A descriptor is already registered against the name "DivideByConst" under registry mode "rendered"
Error in registry file at line number #580
A descriptor is already registered against the name "DivideIntoConst" under registry mode "rendered"
Error in registry file at line number #581
A descriptor is already registered against the name "Erode" under registry mode "rendered"
Error in registry file at line number #582
A descriptor is already registered against the name "ErrorDiffusion" under registry mode "rendered"
Error in registry file at line number #583
A descriptor is already registered against the name "Encode" under registry mode "rendered"
Error in registry file at line number #584
A descriptor is already registered against the name "Exp" under registry mode "rendered"
Error in registry file at line number #585
A descriptor is already registered against the name "Extrema" under registry mode "rendered"
Error in registry file at line number #586
A descriptor is already registered against the name "FileLoad" under registry mode "rendered"
Error in registry file at line number #587
A descriptor is already registered against the name "FileStore" under registry mode "rendered"
Error in registry file at line number #588
A descriptor is already registered against the name "FilteredSubsample" under registry mode "rendered"
Error in registry file at line number #589
A descriptor is already registered against the name "Format" under registry mode "rendered"
Error in registry file at line number #590
A descriptor is already registered against the name "FPX" under registry mode "rendered"
Error in registry file at line number #591
A descriptor is already registered against the name "GIF" under registry mode "rendered"
Error in registry file at line number #592
A descriptor is already registered against the name "GradientMagnitude" under registry mode "rendered"
Error in registry file at line number #593
A descriptor is already registered against the name "Histogram" under registry mode "rendered"
Error in registry file at line number #594
A descriptor is already registered against the name "IDCT" under registry mode "rendered"
Error in registry file at line number #595
A descriptor is already registered against the name "IDFT" under registry mode "rendered"
Error in registry file at line number #596
A descriptor is already registered against the name "IIP" under registry mode "rendered"
Error in registry file at line number #597
A descriptor is already registered against the name "IIPResolution" under registry mode "rendered"
Error in registry file at line number #598
A descriptor is already registered against the name "ImageFunction" under registry mode "rendered"
Error in registry file at line number #599
A descriptor is already registered against the name "Invert" under registry mode "rendered"
Error in registry file at line number #600
A descriptor is already registered against the name "JPEG" under registry mode "rendered"
Error in registry file at line number #601
A descriptor is already registered against the name "Log" under registry mode "rendered"
Error in registry file at line number #602
A descriptor is already registered against the name "Lookup" under registry mode "rendered"
Error in registry file at line number #603
A descriptor is already registered against the name "Magnitude" under registry mode "rendered"
Error in registry file at line number #604
A descriptor is already registered against the name "MagnitudeSquared" under registry mode "rendered"
Error in registry file at line number #605
A descriptor is already registered against the name "Max" under registry mode "rendered"
Error in registry file at line number #606
A descriptor is already registered against the name "MaxFilter" under registry mode "rendered"
Error in registry file at line number #607
A descriptor is already registered against the name "MatchCDF" under registry mode "rendered"
Error in registry file at line number #608
A descriptor is already registered against the name "Mean" under registry mode "rendered"
Error in registry file at line number #609
A descriptor is already registered against the name "MedianFilter" under registry mode "rendered"
Error in registry file at line number #610
A descriptor is already registered against the name "Min" under registry mode "rendered"
Error in registry file at line number #611
A descriptor is already registered against the name "MinFilter" under registry mode "rendered"
Error in registry file at line number #612
A descriptor is already registered against the name "Mosaic" under registry mode "rendered"
Error in registry file at line number #613
A descriptor is already registered against the name "MultiplyConst" under registry mode "rendered"
Error in registry file at line number #614
A descriptor is already registered against the name "MultiplyComplex" under registry mode "rendered"
Error in registry file at line number #615
A descriptor is already registered against the name "Multiply" under registry mode "rendered"
Error in registry file at line number #616
A descriptor is already registered against the name "Not" under registry mode "rendered"
Error in registry file at line number #617
A descriptor is already registered against the name "Null" under registry mode "rendered"
Error in registry file at line number #618
A descriptor is already registered against the name "OrConst" under registry mode "rendered"
Error in registry file at line number #619
A descriptor is already registered against the name "Or" under registry mode "rendered"
Error in registry file at line number #620
A descriptor is already registered against the name "OrderedDither" under registry mode "rendered"
Error in registry file at line number #621
A descriptor is already registered against the name "Overlay" under registry mode "rendered"
Error in registry file at line number #622
A descriptor is already registered against the name "Pattern" under registry mode "rendered"
Error in registry file at line number #623
A descriptor is already registered against the name "PeriodicShift" under registry mode "rendered"
Error in registry file at line number #624
A descriptor is already registered against the name "Phase" under registry mode "rendered"
Error in registry file at line number #625
A descriptor is already registered against the name "Piecewise" under registry mode "rendered"
Error in registry file at line number #626
A descriptor is already registered against the name "PNG" under registry mode "rendered"
Error in registry file at line number #627
A descriptor is already registered against the name "PNM" under registry mode "rendered"
Error in registry file at line number #628
A descriptor is already registered against the name "PolarToComplex" under registry mode "rendered"
Error in registry file at line number #629
A descriptor is already registered against the name "Renderable" under registry mode "renderable"
Error in registry file at line number #630
A descriptor is already registered against the name "Rescale" under registry mode "rendered"
Error in registry file at line number #631
A descriptor is already registered against the name "Rotate" under registry mode "rendered"
Error in registry file at line number #632
A descriptor is already registered against the name "Scale" under registry mode "rendered"
Error in registry file at line number #633
A descriptor is already registered against the name "Shear" under registry mode "rendered"
Error in registry file at line number #634
A descriptor is already registered against the name "Stream" under registry mode "rendered"
Error in registry file at line number #635
A descriptor is already registered against the name "SubsampleAverage" under registry mode "rendered"
Error in registry file at line number #636
A descriptor is already registered against the name "SubsampleBinaryToGray" under registry mode "rendered"
Error in registry file at line number #637
A descriptor is already registered against the name "Subtract" under registry mode "rendered"
Error in registry file at line number #638
A descriptor is already registered against the name "SubtractConst" under registry mode "rendered"
Error in registry file at line number #639
A descriptor is already registered against the name "SubtractFromConst" under registry mode "rendered"
Error in registry file at line number #640
A descriptor is already registered against the name "TIFF" under registry mode "rendered"
Error in registry file at line number #641
A descriptor is already registered against the name "Threshold" under registry mode "rendered"
Error in registry file at line number #642
A descriptor is already registered against the name "Translate" under registry mode "rendered"
Error in registry file at line number #643
A descriptor is already registered against the name "Transpose" under registry mode "rendered"
Error in registry file at line number #644
A descriptor is already registered against the name "UnsharpMask" under registry mode "rendered"
Error in registry file at line number #645
A descriptor is already registered against the name "URL" under registry mode "rendered"
Error in registry file at line number #646
A descriptor is already registered against the name "Warp" under registry mode "rendered"
Error in registry file at line number #647
A descriptor is already registered against the name "XorConst" under registry mode "rendered"
Error in registry file at line number #648
A descriptor is already registered against the name "Xor" under registry mode "rendered"
Error in registry file at line number #653
A descriptor is already registered against the name "gzip" under registry mode "tileDecoder"
Error in registry file at line number #654
A descriptor is already registered against the name "jpeg" under registry mode "tileDecoder"
Error in registry file at line number #655
A descriptor is already registered against the name "raw" under registry mode "tileDecoder"
Error in registry file at line number #660
A descriptor is already registered against the name "jairmi" under registry mode "remoteRendered"
```

And the following files are merged

```
[warn] Merging 'META-INF/javax.media.jai.registryFile.jai' with strategy 'concat'
[warn] Merging 'META-INF/registryFile.jai' with strategy 'concat'
[warn] Merging 'META-INF/registryFile.jaiext' with strategy 'concat'
[warn] Merging 'META-INF/services/com.vividsolutions.xdo.SchemaBuilder' with strategy 'concat'
[warn] Merging 'META-INF/services/java.sql.Driver' with strategy 'concat'
[warn] Merging 'META-INF/services/javax.imageio.spi.ImageInputStreamSpi' with strategy 'concat'
[warn] Merging 'META-INF/services/javax.imageio.spi.ImageOutputStreamSpi' with strategy 'concat'
[warn] Merging 'META-INF/services/javax.imageio.spi.ImageReaderSpi' with strategy 'concat'
[warn] Merging 'META-INF/services/javax.imageio.spi.ImageWriterSpi' with strategy 'concat'
[warn] Merging 'META-INF/services/javax.media.jai.OperationRegistrySpi' with strategy 'concat'
[warn] Merging 'META-INF/services/javax.media.jai.OperationsRegistrySpi' with strategy 'concat'
[warn] Merging 'META-INF/services/org.geotools.data.DataSourceFactorySpi' with strategy 'concat'
[warn] Merging 'META-INF/services/org.geotools.data.FeatureLockFactory' with strategy 'concat'
[warn] Merging 'META-INF/services/org.geotools.feature.AttributeTypeFactory' with strategy 'concat'
[warn] Merging 'META-INF/services/org.geotools.feature.FeatureCollections' with strategy 'concat'
[warn] Merging 'META-INF/services/org.geotools.filter.FunctionFactory' with strategy 'concat'
[warn] Merging 'META-INF/services/org.geotools.filter.expression.PropertyAccessorFactory' with strategy 'concat'
[warn] Merging 'META-INF/services/org.geotools.referencing.factory.gridshift.GridShiftLocator' with strategy 'concat'
[warn] Merging 'META-INF/services/org.geotools.referencing.operation.MathTransformProvider' with strategy 'concat'
[warn] Merging 'META-INF/services/org.geotools.styling.StyleFactory' with strategy 'concat'
[warn] Merging 'META-INF/services/org.geotools.util.ConverterFactory' with strategy 'concat'
[warn] Merging 'META-INF/services/org.geotools.xml.schema.Schema' with strategy 'concat'
[warn] Merging 'META-INF/services/org.opengis.coverage.processing.Operation' with strategy 'concat'
[warn] Merging 'META-INF/services/org.opengis.feature.FeatureFactory' with strategy 'concat'
[warn] Merging 'META-INF/services/org.opengis.feature.type.FeatureTypeFactory' with strategy 'concat'
[warn] Merging 'META-INF/services/org.opengis.filter.FilterFactory' with strategy 'concat'
[warn] Merging 'META-INF/services/org.opengis.filter.expression.Function' with strategy 'concat'
[warn] Merging 'META-INF/services/org.opengis.referencing.crs.CRSAuthorityFactory' with strategy 'concat'
[warn] Merging 'META-INF/services/org.opengis.referencing.crs.CRSFactory' with strategy 'concat'
[warn] Merging 'META-INF/services/org.opengis.referencing.cs.CSAuthorityFactory' with strategy 'concat'
[warn] Merging 'META-INF/services/org.opengis.referencing.cs.CSFactory' with strategy 'concat'
[warn] Merging 'META-INF/services/org.opengis.referencing.datum.DatumAuthorityFactory' with strategy 'concat'
[warn] Merging 'META-INF/services/org.opengis.referencing.datum.DatumFactory' with strategy 'concat'
[warn] Merging 'META-INF/services/org.opengis.referencing.operation.CoordinateOperationAuthorityFactory' with strategy 'concat'
[warn] Merging 'META-INF/services/org.opengis.referencing.operation.CoordinateOperationFactory' with strategy 'concat'
[warn] Merging 'META-INF/services/org.opengis.referencing.operation.MathTransformFactory' with strategy 'concat'
```

## different merge strategy

I am pretty convinced now that something is wrong with my merge strategy. https://github.com/geoHeil/jai-packaging-problem/commit/c7ba46be2c2bcbab8196fd6ece1dfd3dece89594 Helps to get rid of all the duplciation Errors of JAI registry on startup, still the error is the same.
