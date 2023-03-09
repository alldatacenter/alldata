package com.linkedin.feathr.offline.client

object TypedRef {

/*
  * The delimiter used to separate namespace, name and version fields. It must be chosen such that it doesn't
  * conflict with the restricted characters used in HOCON, Pegasus's PathSpec and the characters used in Java
  * variable names.
  */
val DELIM: String = "-"

}
