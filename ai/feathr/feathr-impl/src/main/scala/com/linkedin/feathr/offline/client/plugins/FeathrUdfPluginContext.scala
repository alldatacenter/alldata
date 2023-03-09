package com.linkedin.feathr.offline.client.plugins

import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast

import scala.collection.mutable

/**
 * A shared registry for loading [[UdfAdaptor]]s, which basically can tell Feathr's runtime how to support different
 * kinds of "external" UDFs not natively known to Feathr, but which have similar behavior to Feathr's.
 *
 * All "external" UDF classes are required to have a public default zero-arg constructor.
 */
object FeathrUdfPluginContext {
  private val localRegisteredUdfAdaptors = mutable.Buffer[UdfAdaptor[_]]()
  private var registeredUdfAdaptors: Broadcast[mutable.Buffer[UdfAdaptor[_]]] = null
  def registerUdfAdaptor(adaptor: UdfAdaptor[_], sc: SparkContext): Unit = {
    this.synchronized {
      localRegisteredUdfAdaptors += adaptor
      if (registeredUdfAdaptors != null) {
        registeredUdfAdaptors.destroy()
      }
      registeredUdfAdaptors = sc.broadcast(localRegisteredUdfAdaptors)
    }
  }

  def getRegisteredUdfAdaptor(clazz: Class[_]): Option[UdfAdaptor[_]] = {
    if (registeredUdfAdaptors != null) {
      registeredUdfAdaptors.value.find(_.canAdapt(clazz))
    } else None
  }
}