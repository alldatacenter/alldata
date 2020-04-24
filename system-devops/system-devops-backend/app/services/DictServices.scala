package services

import java.util.concurrent.ConcurrentHashMap
import javax.inject.{Inject, Singleton}

import auto.Dict.{DictData, DictRead}
import models.DictSqler
import play.api.Configuration
import play.api.libs.ws.WSClient

import scala.concurrent.Await

@Singleton
class DictServices @Inject()(dictSqler: DictSqler, config: Configuration) {
  val dictCache = new ConcurrentHashMap[String, Integer]()

  def init(): Unit = {
    dictSqler.all().map(item => {
      val k = item.dictName + ":" + item.dictValue
      dictCache.put(k, 1);
    })
  }

  def add(dictDatas: Array[DictRead]): Unit = {
    val inserts = dictDatas.filter(item => {
      val k = item.dictName + ":" + item.dictValue
      !dictCache.containsKey(k)
    })

    inserts.map(item => {
      dictSqler.addWithoutException(item)
    })

    dictDatas.foreach(item => {
      val k = item.dictName + ":" + item.dictValue
      dictCache.put(k, 1)
    })

  }
}
