package com.linkedin.feathr.offline.mvel.plugins

import com.linkedin.feathr.common.FeatureValue
import com.linkedin.feathr.offline.mvel.MvelContext
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.mvel2.ConversionHandler
import org.mvel2.conversion.ArrayHandler
import org.mvel2.util.ReflectionUtil.{isAssignableFrom, toNonPrimitiveType}

import java.io.Serializable
import java.util.Optional
import scala.collection.mutable

/**
 * The context needed for the Feathr expression transformation language, in order to
 * support the automatic conversion between the Feather Feature value class and
 * some customized external data, e.g. 3rd-party feature value class.
 * It is intended for advanced cases to enable compatibility with old versions of FeathrExpression language
 * and that most users would not need to use it.
 */
class FeathrExpressionExecutionContext extends Serializable {

  // A map of converters that are registered to convert a class into customized data format.
  // This include convert from and to feature value.
  // The Map is broadcast from the driver to executors
  private var converters: Broadcast[mutable.HashMap[String, ConversionHandler]] = null
  // A map of adaptors that are registered to convert a Feathr FeatureValue to customized external data format
  // The Map is broadcasted from the driver to executors
  private var featureValueTypeAdaptors: Broadcast[mutable.HashMap[String, FeatureValueTypeAdaptor[AnyRef]]] = null

  // Same as converters, used to build the map on the driver during the job initialization.
  // Will be broadcasted to all executors and available as converters
  private val localConverters = new mutable.HashMap[String, ConversionHandler]
  // Same as featureValueTypeAdaptors, used to build the map on the driver during the job initialization.
  // Will be broadcasted to all executors and available as converters
  private val localFeatureValueTypeAdaptors = new mutable.HashMap[String, FeatureValueTypeAdaptor[AnyRef]]

  /**
   * Setup Executor Mvel Expression Context by adding a type adaptor to Feathr's MVEL runtime,
   * it will enable Feathr's expressions to support some alternative
   * class representation of {@link FeatureValue} via coercion.
   *
   * @param clazz       the class of the "other" alternative representation of feature value
   * @param typeAdaptor the type adaptor that can convert between the "other" representation and {@link FeatureValue}
   * @param <           T> type parameter for the "other" feature value class
   */
  def setupExecutorMvelContext[T](clazz: Class[T],
                                  typeAdaptor: FeatureValueTypeAdaptor[T],
                                  sc: SparkContext,
                                  mvelExtContext: Option[Class[Any]] = None): Unit = {
    localFeatureValueTypeAdaptors.put(clazz.getCanonicalName, typeAdaptor.asInstanceOf[FeatureValueTypeAdaptor[AnyRef]])
    featureValueTypeAdaptors = sc.broadcast(localFeatureValueTypeAdaptors)
    // Add a converter that can convert external data to feature value
    addConversionHandler(classOf[FeatureValue], new ExternalDataToFeatureValueHandler(featureValueTypeAdaptors), sc)
    // Add a converter that can convert a feature value to external data
    addConversionHandler(clazz, new FeatureValueToExternalDataHandler(typeAdaptor), sc)
    MvelContext.mvelAlienUDFRegisterClazz = if (mvelExtContext.isDefined) {
      Optional.of(sc.broadcast(mvelExtContext.get))
    } else {
      Optional.empty()
    }
  }

  /**
   * Check if there is registered converters that can handle the conversion.
   * @param toType type to convert to
   * @param convertFrom type to convert from
   * @return whether it can be converted or not
   */
  def canConvert(toType: Class[_], convertFrom: Class[_]): Boolean = {
    if (isAssignableFrom(toType, convertFrom)) return true
    if (converters.value.contains(toType.getCanonicalName)) {
      converters.value.get(toType.getCanonicalName).get.canConvertFrom(toNonPrimitiveType(convertFrom))
    } else if (toType.isArray && canConvert(toType.getComponentType, convertFrom)) {
      true
    } else {
      false
    }
  }

  /**
   * Convert the input to output type using the registered converters
   * @param in value to be converted
   * @param toType output type
   * @tparam T
   * @return
   */
  def convert[T](in: Any, toType: Class[T]): T = {
    if ((toType eq in.getClass) || toType.isAssignableFrom(in.getClass)) return in.asInstanceOf[T]
    val converter = if (converters.value != null) {
      converters.value.get(toType.getCanonicalName).get
    } else {
      throw new RuntimeException(s"Cannot convert ${in} to ${toType} due to no converters found.")
    }
    if (converter == null && toType.isArray) {
      val handler = new ArrayHandler(toType)
      converters.value.put(toType.getCanonicalName, handler)
      handler.convertFrom(in).asInstanceOf[T]
    }
    else converter.convertFrom(in).asInstanceOf[T]
  }

  /**
   * Register a new {@link ConversionHandler} with the factory.
   *
   * @param type    - Target type represented by the conversion handler.
   * @param handler - An instance of the handler.
   */
  private[plugins] def addConversionHandler(`type`: Class[_], handler: ConversionHandler, sc: SparkContext): Unit = {
    localConverters.put(`type`.getCanonicalName, handler)
    converters = sc.broadcast( localConverters)
  }

  /**
   * Convert Feathr FeatureValue to external FeatureValue
   * @param adaptor An adaptor that knows how to convert the Feathr feature value to requested external data
   */
  class FeatureValueToExternalDataHandler(val adaptor: FeatureValueTypeAdaptor[_])
    extends ConversionHandler with Serializable {
    /**
     * Convert a FeatureValue into requested external data
     * @param fv the input feature value
     * @return requested external data
     */
    override def convertFrom(fv: Any): AnyRef = adaptor.fromFeathrFeatureValue(fv.asInstanceOf[FeatureValue]).asInstanceOf[AnyRef]

    override def canConvertFrom(cls: Class[_]): Boolean =  classOf[FeatureValue] == cls
  }


  /**
   * Convert external data types to Feathr FeatureValue automatically
   * @param adaptors a map of adaptors that knows how to convert external data to feature value.
   *                 It maps the supported input class name to its adaptor.
   */
  class ExternalDataToFeatureValueHandler(val adaptors: Broadcast[mutable.HashMap[String, FeatureValueTypeAdaptor[AnyRef]]])
    extends ConversionHandler with Serializable {

    /**
     * Convert external data to a Feature value
     *
     * @param externalData to convert
     * @return result feature value
     */
    def convertFrom(externalData: Any): AnyRef = {
      val adaptor = adaptors.value.get(externalData.getClass.getCanonicalName).get
      if (adaptor == null) throw new IllegalArgumentException("Can't convert to Feathr FeatureValue from " + externalData + ", current type adaptors: " + adaptors.value.keySet.mkString(","))
      adaptor.toFeathrFeatureValue(externalData.asInstanceOf[AnyRef])
    }

    override def canConvertFrom(cls: Class[_]): Boolean = adaptors.value.contains(cls.getCanonicalName)
  }
}
