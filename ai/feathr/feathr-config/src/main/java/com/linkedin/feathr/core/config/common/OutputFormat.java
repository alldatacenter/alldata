package com.linkedin.feathr.core.config.common;

/**
 * output format of Frame feature generation,
 * name-term-value(NAME_TERM_VALUE), name-listof-term-value(COMPACT_NAME_TERM_VALUE), RAW_DATA(raw dataframe), TENSOR
 */
public enum OutputFormat {
  NAME_TERM_VALUE, COMPACT_NAME_TERM_VALUE, RAW_DATA, TENSOR, CUSTOMIZED, QUINCE_FDS
}