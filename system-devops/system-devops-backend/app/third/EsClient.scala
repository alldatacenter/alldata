package third

import java.util.Base64
import javax.inject.{Inject, Singleton}

import org.json.JSONObject
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.ws.WSClient

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

case class EsFilter(filter: Map[String, Array[JsValue]], offset: Int, limit: Int)

case class EsResult(total: Int, records: Array[(String, JsObject)])
case class EsFavorite(records: (String, JsObject))

class EsTableQuery(host: String, index: (String, String), token: String) {
  val logger = LoggerFactory.getLogger("application")

  //分页和scroll的区别
  def mulitFilterTerm(q: Map[String, Array[JsValue]], isSearch: Boolean, offset: Int, limit: Int): JsObject = {
    val filterQ = q.filter(_._2.nonEmpty)
    val jo = if (filterQ.isEmpty) {
      Json.obj(
        "query" -> Json.obj(
          "match_all" -> Json.obj()
        )
      )
    }
    else {
      val arrs = filterQ.map(item => {
        if (item._1.equals("tag") || item._1.equals("keyword")) {
          val exists = item._2.map(one => {
            val name = one.as[String]
            Json.obj("exists" -> Json.obj("field" -> s"${item._1}.${name}"))
          })
          Json.obj("bool" -> Json.obj("should" -> Json.toJson(exists)))
        }
        else if (item._1.equals("field_exists")) {
          val exists = item._2.map(one => {
            val name = one.as[String]
            Json.obj("exists" -> Json.obj("field" -> s"${name}"))
          })
          Json.obj("bool" -> Json.obj("must" -> Json.toJson(exists)))
        }
        else if (item._1.equals("sort")){
          Json.obj()
        }
        else {
          Json.obj(
            "constant_score" -> Json.obj(
              "filter" -> Json.obj(
                "terms" -> Json.obj(
                  item._1 -> item._2
                )
              )
            )
          )
        }
      })
      Json.obj(
        "query" -> Json.obj(
          "bool" -> Json.obj {
            "must" -> Json.toJson(arrs)
          }
        )
      )
    }

    if (isSearch) {
      var m = jo.value ++ Map(
        "from" -> Json.toJson(offset),
        "size" -> Json.toJson(limit)
      )
      if (filterQ.contains("sort")){
        println("conTain sort")
        val s = Array(Map("animCount"->Map("order"->"desc")))
        val mystr ="""{"_script" : {"type" : "number","script" : {"lang": "painless","inline": "score = doc['animCount']*100*doc['ratio'].value;if(doc['animArray'].value.contains("Idle");ctx._source['modelscore'] =10000);ctx._source['modelscore'] =score"},"order" : "desc"}}"""
        m=m++ Map(
          "sort" -> Json.toJson(filterQ.get("sort"))
//          "sort" -> Json.parse(mystr)
        )
      }
      val tem = Json.toJson(m).as[JsObject]
      println(tem)
      tem

    }
    else jo

  }

  def search(ws: WSClient, read: EsFilter): EsResult = {
    val body = mulitFilterTerm(read.filter, true, read.offset, read.limit).toString()
    val url = s"http://${host}/${index._1}/${index._2}/_search"
    val request = ws.url(url)
      .withHttpHeaders("Content-Type" -> "application/json")
      .withHttpHeaders("Authorization" -> s"Basic ${token}")
      .withBody(body)
      .get()
//    logger.info(s"qery with $url $body")
    val response = Await.result(request, 60 seconds)
    val j = response.json

//    logger.info(j.toString())
    val total = (j \ "hits" \ "total").as[Int]
    val values = (j \ "hits" \ "hits").as[JsArray].value.map(item => {
      val id = (item \ "_id").as[String]
      val t = (item \ "_source").as[JsObject]
      (id, t)
    }).toArray
    EsResult(total, values)
  }

  def searchFavorites(ws: WSClient, item: String): Tuple2[String, JsObject] = {
    val body = Json.obj(
      "query" -> Json.obj(
        "match" -> Json.obj("_id" -> item)
      )
    ).toString()

    val url = s"http://${host}/${index._1}/${index._2}/_search"
    val request = ws.url(url)
      .withHttpHeaders("Content-Type" -> "application/json")
      .withHttpHeaders("Authorization" -> s"Basic ${token}")
      .withBody(body)
      .get()
    logger.info(s"qery with $url $body")
    val response = Await.result(request, 60 seconds)
    val j = response.json
    //    logger.info(j.toString())
    val total = (j \ "hits" \ "total").as[Int]
    val values = (j \ "hits" \ "hits").as[JsArray].value.map(item => {
      val id = (item \ "_id").as[String]
      val t = (item \ "_source").as[JsObject]
      (id, t)
    }).toArray
    Tuple2(values.head._1, values.head._2)
  }


  def count(ws: WSClient, read: EsFilter): Int = {
    val body = mulitFilterTerm(read.filter, false, read.offset, read.limit).toString()
    val url = s"http://${host}/${index._1}/${index._2}/_count"
    val request = ws.url(url)
      .withHttpHeaders("Content-Type" -> "application/json")
      .withHttpHeaders("Authorization" -> s"Basic ${token}")
      .withBody(body)
      .get()
    logger.info(s"qery with $url $body")
    val response = Await.result(request, 60 seconds)
    val j = response.json
    //    logger.info(j.toString())
    val count = (j \ "count").as[Int]
    count
  }

  def getOne(ws: WSClient, id: String): Option[JsObject] = {
    val url = s"http://${host}/${index._1}/${index._2}/${id}"
    val request = ws.url(url)
      .withHttpHeaders("Content-Type" -> "application/json")
      .withHttpHeaders("Authorization" -> s"Basic ${token}")
      .get()
    logger.info(s"qery with $url ")
    val response = Await.result(request, 60 seconds)
    val j = response.json
    logger.info(j.toString())
    val found = (j \ "found").as[Boolean]
    if (found) {
      val r = (j \ "_source").as[JsObject]
      Option(r)
    }
    else None
  }

  def delete(ws: WSClient, id: String): JsObject = {
    val url = s"http://${host}/${index._1}/${index._2}/${id}"
    val request = ws.url(url)
      .withHttpHeaders("Content-Type" -> "application/json")
      .withHttpHeaders("Authorization" -> s"Basic ${token}")
      .delete()
    logger.info(s"delete with $url ")
    val response = Await.result(request, 60 seconds)
    val j = response.json.as[JsObject]
    logger.info(j.toString())
    j
  }

  def addOrUpdate(ws: WSClient, id: String, jo: JsObject): String = {
    val body = jo.toString()
    val url = s"http://${host}/${index._1}/${index._2}/${id}"
    val request = ws.url(url)
      .withHttpHeaders("Content-Type" -> "application/json")
      .withHttpHeaders("Authorization" -> s"Basic ${token}")
      .put(body)
    logger.info(s"put with $url $body")
    val response = Await.result(request, 60 seconds)
    val j = response.json
    logger.info(j.toString())
    val result = (j \ "result").as[String]
    result
  }

  def suggest(ws: WSClient, field: String, input: String): Array[String] = {
    val body = Json.obj(
      "query" -> Json.obj("wildcard" -> Json.obj(field -> s"*${input}*")),
      "_source" -> Json.obj("includes" -> field),
      "from" -> 0,
      "size" -> 10
    )
    val url = s"http://${host}/${index._1}/${index._2}/_search"
    val request = ws.url(url)
      .withHttpHeaders("Content-Type" -> "application/json")
      .withHttpHeaders("Authorization" -> s"Basic ${token}")
      .withBody(body)
      .get()
    logger.info(s"qery with $url $body")
    val response = Await.result(request, 60 seconds)
    val j = response.json
    logger.info(j.toString())
    val total = (j \ "hits" \ "total").as[Int]
    val values = (j \ "hits" \ "hits").as[JsArray].value.map(item => {
      val id = (item \ "_id").as[String]
      val t = (item \ "_source").as[JsObject]
      val v = (t \ field).as[String]
      v
    }).toArray
    values
  }
}

/**
  * Created by wulinhao on 2020/03/15.
  */
@Singleton
class EsClient @Inject()(ws: WSClient, config: Configuration)(implicit executionContext: ExecutionContext) {
  val logger = LoggerFactory.getLogger("application")
  val host = config.get[String]("es.host")
  val user = config.get[String]("es.user")
  val pass = config.get[String]("es.pass")
  val token = new String(Base64.getEncoder.encode((user + ':' + pass).getBytes))

  val gameIndexType = (config.get[String]("es.game.index"), config.get[String]("es.game.type"))
  val threeModelIndexType = (config.get[String]("es.3dmodel.index"), config.get[String]("es.3dmodel.type"))
  val originalModelIndexType = (config.get[String]("es.original-model.index"), config.get[String]("es.original-model.type"))

  val gameTableQuery = new EsTableQuery(host, gameIndexType, token)
  val threeModelTableQuery = new EsTableQuery(host, threeModelIndexType, token)
  val originalModelTableQuery = new EsTableQuery(host, originalModelIndexType, token)

  def searchExternalGame(read: EsFilter): EsResult = {
    gameTableQuery.search(ws, read)
  }

  def getExternalGame(id: String): Option[JsObject] = {
    gameTableQuery.getOne(ws, id)
  }

  def tagExternalGame(id: String, tag: Map[String, Double], keyword: Map[String, Double]): Int = {
    getAndUpdateTagKeyword(gameTableQuery, id, tag, keyword)
  }

  def suggestExternalGame(field: String, input: String): Array[String] = {
    gameTableQuery.suggest(ws, field, input)
  }

  def searchExternalModel(read: EsFilter): EsResult = {
    threeModelTableQuery.search(ws, read)
  }
  def searchFavoritesModel(item: String): Tuple2[String, JsObject] = {
    threeModelTableQuery.searchFavorites(ws, item)
  }

  def getExternalModel(id: String): Option[JsObject] = {
    threeModelTableQuery.getOne(ws, id)
  }

  def tagExternalModel(id: String, tag: Map[String, Double], keyword: Map[String, Double]): Int = {
    getAndUpdateTagKeyword(threeModelTableQuery, id, tag, keyword)
  }

  def suggestExternalModel(field: String, input: String): Array[String] = {
    threeModelTableQuery.suggest(ws, field, input)
  }


  def updateOriginalModel(id: String, jo: JsObject): String = {
    originalModelTableQuery.addOrUpdate(ws, id, jo)
  }

  def searchOriginalModel(read: EsFilter): EsResult = {
    originalModelTableQuery.search(ws, read)
  }

  def countOriginalModel(read: EsFilter): Int = {
    originalModelTableQuery.count(ws, read)
  }

  def deleteOriginalModel(id: String): JsObject = {
    originalModelTableQuery.delete(ws, id)
  }

  def getAndUpdateTagKeyword(query: EsTableQuery, id: String, tag: Map[String, Double], keyword: Map[String, Double]): Int = {
    val one = query.getOne(ws, id)
    if (one.isDefined) {
      val jo = one.get.value
      val njo = jo ++ Map("tag" -> Json.toJson(tag), "keyword" -> Json.toJson(keyword))
      query.addOrUpdate(ws, id, Json.toJson(njo).as[JsObject])
      1
    }
    else 0
  }
}
