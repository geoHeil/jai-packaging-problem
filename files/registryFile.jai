#
# The registry file for Geotools Grid Coverage implementation. This file should be automatically
# parsed by JAI under initialization. If it is not, then Registry.registerServices(...) can parse
# it. This may happen if this file is unreachable from the JAI class loader.
#
# If this file is updated, remember to update Registry.registerServices(...) accordingly.
#
# Version: $Id$
#
##################################################################################################
#
# Each line can be in one of the formats described below. Space or tab characters separate
# keywords in each line. The comment character is '#'; on each line all characters following
# the first comment character are ignored. The file must be encoded in UTF-8.
#
# 1. To register descriptors :
#
#      descriptor <descriptor-class-name>
#
# 2. To register factory objects under a product against a specific mode :
#
#      <registry-mode-name>  <factory-class-name>  <product-name>  <descriptor-name>  <local-name>
#
#    The <local-name> is (only) used later on in this file to set preferences
#    between factory objects, if wanted.
#
##################################################################################################
#
# Image operation descriptors :
#
descriptor  org.geotools.image.palette.ColorReductionDescriptor
descriptor  org.geotools.image.palette.ColorInversionDescriptor

#
# "rendered" factory objects
#
rendered  org.geotools.image.palette.ColorReductionCRIF	org.geotools  org.geotools.ColorReduction ColorReduction
rendered  org.geotools.image.palette.ColorInversionCRIF	org.geotools  org.geotools.ColorInversion ColorInversion
#
# Image operation descriptors :
#
descriptor  org.jaitools.media.jai.zonalstats.ZonalStatsDescriptor


#
# "rendered" factory objects
#
rendered  org.jaitools.media.jai.zonalstats.ZonalStatsRIF  org.jaitools.media.jai  ZonalStats  ZonalStats
#
# Image descriptors
#
descriptor  org.geotools.processing.jai.ClassBreaksDescriptor
descriptor  org.geotools.processing.jai.TransparencyFillDescriptor

#
# RenderedImageFactories
#
rendered  org.geotools.processing.jai.ClassBreaksRIF  org.jaitools.media.jai  ClassBreaks  ClassBreaks
rendered  org.geotools.processing.jai.TransparencyFillRIF  it.geosolutions.jaiext  TransparencyFill  TransparencyFill
#
# Image operation descriptors :
#
descriptor  org.jaitools.media.jai.rangelookup.RangeLookupDescriptor


#
# "rendered" factory objects
#
rendered  org.jaitools.media.jai.rangelookup.RangeLookupRIF  org.jaitools.media.jai  RangeLookup  RangeLookup
#
# Image operation descriptors :
#
descriptor  org.jaitools.media.jai.contour.ContourDescriptor


#
# "rendered" factory objects
#
rendered  org.jaitools.media.jai.contour.ContourRIF  org.jaitools.media.jai  Contour  Contour
#
# Image operation descriptors :
#
descriptor  org.jaitools.media.jai.vectorize.VectorizeDescriptor


#
# "rendered" factory objects
#
rendered  org.jaitools.media.jai.vectorize.VectorizeRIF  org.jaitools.media.jai  Vectorize  Vectorize
