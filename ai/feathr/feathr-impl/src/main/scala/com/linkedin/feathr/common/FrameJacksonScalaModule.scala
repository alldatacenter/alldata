package com.linkedin.feathr.common

import com.fasterxml.jackson.module.scala._
import com.fasterxml.jackson.module.scala.deser.UntypedObjectDeserializerModule

class FeathrJacksonScalaModule
  extends JacksonModule
    with IteratorModule
    with EnumerationModule
    with OptionModule
    with SeqModule
    with IterableModule
    with TupleModule
    with MapModule
    with SetModule
    with UntypedObjectDeserializerModule
{
  override def getModuleName = "FeathrJacksonScalaModule"
}

object FeathrJacksonScalaModule extends FeathrJacksonScalaModule;
