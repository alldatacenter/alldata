package com.linkedin.feathr.offline.util

class ColumnMetadataMap extends Map[String, Any]{
  override def +[B1 >: Any](kv: (String, B1)): Map[String, B1] = ???

  override def get(key: String): Option[Any] = ???

  override def iterator: Iterator[(String, Any)] = ???

  override def -(key: String): Map[String, Any] = ???
}
