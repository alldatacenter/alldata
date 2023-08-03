package com.linkedin.feathr.offline.client

import com.linkedin.feathr.common.DateParam
import com.linkedin.feathr.offline.source.SourceFormatType
import com.linkedin.feathr.offline.source.SourceFormatType.SourceFormatType

/**
  * Case class to hold the different parameters related to input data
  *
  * @param inputPath Root path of the obs data
  * @param sourceType  [[SourceFormatType]]
  * @param startDate start date of the obs data
  * @param endDate end date of the obs data
  * @param dateOffset  num of days to be offset from the intended date
  * @param numDays window days starting from reference (date-dateoffset)
  */
private[offline] case class InputData(
   inputPath: String,
   sourceType: SourceFormatType = SourceFormatType.FIXED_PATH,
   startDate: Option[String] = None,
   endDate: Option[String] = None,
   dateOffset: Option[String] = None,
   numDays: Option[String] = None) {
  def dateParam: Option[DateParam] = Some(DateParam(startDate, endDate, dateOffset, numDays))
}
