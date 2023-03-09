package com.linkedin.feathr.offline.testfwk

import org.joda.time.Interval

/**
 * mock data parameters for a source in the Feathr anchor
 * @param saveBasePath base path to save the mock data
 * @param dateParam time range to generate the mock data
 */
private[feathr] class SourceMockParam(val saveBasePath: String, val dateParam: Option[Interval])
