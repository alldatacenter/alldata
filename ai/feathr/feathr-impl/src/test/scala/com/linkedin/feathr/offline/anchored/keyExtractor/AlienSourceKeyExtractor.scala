package com.linkedin.feathr.offline.anchored.keyExtractor

trait AlienSourceKeyExtractor extends Serializable {
  def getKey(): Seq[String]
}
