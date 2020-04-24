package controllers

import javax.inject.{Inject, Singleton}

import auto.Dict.DictRead
import auto.OriginalGame.{InnerOriginalGameController, OriginalGameRead}
import io.swagger.annotations.{Api, ApiImplicitParam, ApiImplicitParams, ApiOperation}
import models.OriginalGameSqler
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.ControllerComponents
import controllers._
import io.swagger.annotations.{ApiImplicitParam, ApiImplicitParams, ApiOperation}
import models.OriginalGameSqler
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.ws.WSClient
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.{Configuration, Environment}
import services.DictServices
import third.{EsClient, EsFilter, OssClient}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext

/**
  * Created by wulinhao on 2020/03/14.
  */
//case class SearchGameFilter(name: Array[String], pn: Array[String], category: Array[String], tag: Array[String], keyword: Array[String])

//case class SearchGameRead(filter: Map[String, Array[String]], offset: Int, limit: Int)

//case class ExternalGameEsRecord(name: String, pn: String, category: String, create_time: String, update_time: String, is_unpack: Boolean,
//                                tag: Map[String, Double], keyword: Map[String, Double],
//                                cty: String,
//                                developer: String,
//                                min_android: String,
//                                content_rating: String,
//                                installs: Int,
//                                review_num: Int,
//                                review_star: Int
//                               )


case class ExternalGame(id: String, record: JsObject, icon: String)

case class ReSearchData(list: Array[ExternalGame], total: Int)

case class ReSearchGame(isSuccess: Boolean = true, data: ReSearchData)

case class ExternalGameDetail(id: String, record: JsObject, icon: String, previews: Map[String, Array[String]])

case class ReGetOneExternalGame(isSuccess: Boolean = true, data: ExternalGameDetail)

case class ExternalGameTagRead(tag: Map[String, Double], keyword: Map[String, Double])

case class ReSuggestGame(isSuccess: Boolean = true, data: Array[String])

@Api("externalGame")
@Singleton
class ExternalGameController @Inject()(esClient: EsClient, ossClient: OssClient, dictServices: DictServices,
                                       originalGameSpler: OriginalGameSqler,
                                       ws: WSClient, environment: Environment, apiAction: ApiAction,
                                       config: Configuration, cc: ControllerComponents)(implicit executionContext: ExecutionContext) extends AbstractController(cc) with JsonFormat {
  val logger = LoggerFactory.getLogger("application")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "third.EsFilter", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "search ExternalGame", response = classOf[ReSearchGame])
  def searchGame() = apiAction(parse.json[EsFilter]) { request =>
    val read = request.body
    val result = esClient.searchExternalGame(read)
    val datas = result.records.map(item => {
//      logger.info(item.toString())
      val pn = (item._2 \ "pn").as[String]
      val icon = ossClient.getIcon(pn)
      ExternalGame(item._1, item._2, icon)
    })
    Ok(Json.toJson(ReSearchGame(data = ReSearchData(datas, result.total))))
  }

  @ApiOperation(value = "get one ExternalGame", response = classOf[ReGetOneExternalGame])
  def getOne(id: String) = apiAction { request =>
    val joOpiton = esClient.getExternalGame(id)
    if (joOpiton.isDefined) {
      val jo = joOpiton.get
      val pn = (jo \ "pn").as[String]
      val icon = ossClient.getIcon(pn)
      val previews = ossClient.getAllPreviewList(pn)
      val jsonTem=Json.toJson(ReGetOneExternalGame(data = ExternalGameDetail(id, jo, icon, previews)))
      println(jsonTem)

      Ok(jsonTem)
    }
    else {
      Ok(Json.toJson(ReMsg(msg = "object not found")))
    }
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.ExternalGameTagRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "tag one ExternalGame", response = classOf[ReId])
  def tag(id: String) = apiAction(parse.json[ExternalGameTagRead]) { request =>
    val read = request.body
    val re = esClient.tagExternalGame(id, read.tag, read.keyword)
    val tagDict = read.tag.map(item => {
      DictRead(DictName.game_tag.toString, item._1, "")
    }).toArray
    dictServices.add(tagDict)
    val keywordDict = read.keyword.map(item => {
      DictRead(DictName.game_keyword.toString, item._1, "")
    }).toArray
    dictServices.add(keywordDict)
    Ok(Json.toJson(ReId(data = re)))
  }

  @ApiOperation(value = "get suggest with field and input", response = classOf[ReGetOneExternalGame])
  def suggest(field: String, input: String) = apiAction { request =>
    if (field == "name" || field == "pn") {
      val values = esClient.suggestExternalGame(field, input)
      Ok(Json.toJson(ReSuggestGame(data = values)))
    }
    else {
      Ok(Json.toJson(ReMsg(msg = "field must be name or pn")))
    }
  }
}
