package com.linkedin.feathr.offline.anchored.keyExtractor

import com.linkedin.feathr.offline.client.plugins.SourceKeyExtractorAdaptor
import com.linkedin.feathr.sparkcommon.SourceKeyExtractor
import org.apache.spark.sql.DataFrame

class AlienSourceKeyExtractorAdaptor extends SourceKeyExtractorAdaptor {
  /**
   * Indicates whether this adaptor can be applied to an object of the provided class.
   *
   * Implementations should usually look like <pre>classOf[UdfTraitThatIsNotPartOfFeathr].isAssignableFrom(clazz)</pre>
   *
   * @param clazz some external UDF type
   * @return true if this adaptor can "adapt" the given class type; false otherwise
   */
  override def canAdapt(clazz: Class[_]): Boolean = classOf[AlienSourceKeyExtractor].isAssignableFrom(clazz)

  /**
   * Returns an instance of a Feathr UDF, that follows the behavior of some external UDF instance, e.g. via delegation.
   *
   * @param externalUdf instance of the "external" UDF
   * @return the Feathr UDF
   */
  override def adaptUdf(externalUdf: AnyRef): SourceKeyExtractor = new AlienSourceKeyExtractorWrapper(externalUdf.asInstanceOf[AlienSourceKeyExtractor])

  /**
   * Wrap Alien SourceKeyExtractor as Feathr SourceKeyExtractor
   */
  private class AlienSourceKeyExtractorWrapper(keyExtractor: AlienSourceKeyExtractor) extends SourceKeyExtractor{
    override def getKeyColumnNames(datum: Option[Any]): Seq[String] = Seq("mId")

    override def appendKeyColumns(dataFrame: DataFrame): DataFrame = {
      keyExtractor.getKey()
      dataFrame
    }
  }
}
