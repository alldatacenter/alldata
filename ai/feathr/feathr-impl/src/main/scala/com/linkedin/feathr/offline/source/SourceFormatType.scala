package com.linkedin.feathr.offline.source

/**
 * An Enum to represent the different source format types.
 * "FIXED_PATH" is for general feature data path, where the #LATEST will be loaded;
 * "TIME_PATH" is for time-based feature data path (see implementation for details);
 * "TIME_SERIES_PATH" is for time-window feature data path (see implementation for details);
 * "LIST_PATH" is a list of semicolon separated data paths, which will be loaded verbatim.
 */
private[offline] object SourceFormatType extends Enumeration {
  type SourceFormatType = Value
  val FIXED_PATH, TIME_PATH, TIME_SERIES_PATH, LIST_PATH = Value
}
