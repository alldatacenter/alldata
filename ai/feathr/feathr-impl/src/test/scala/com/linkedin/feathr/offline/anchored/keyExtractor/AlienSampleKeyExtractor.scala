package com.linkedin.feathr.offline.anchored.keyExtractor

class AlienSampleKeyExtractor extends AlienSourceKeyExtractor {
  override def getKey(): Seq[String] = Seq("1")
}
