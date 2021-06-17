package controllers

import java.util.UUID
import javax.inject.{Inject, Singleton}

import io.swagger.annotations._
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws._
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.{Configuration, Environment}
import third.{EsClient, EsFilter}
import utils.PlayUtils

import scala.concurrent.ExecutionContext

case class AddOriginalModelRecord(name: String, game_id: Int, level1: Int, level2: Int,
                                  operator: String,
                                  tag: Map[String, Double],
                                  keyword: Map[String, Double],
                                  url: String, file_type: String, file_size: Long)

case class UpdateOriginalModelRecord(name: String, game_id: Int, level1: Int, level2: Int,
                                     tag: Map[String, Double],
                                     keyword: Map[String, Double],
                                     url: String, file_type: String, file_size: Long,
                                     create_time: String)

case class OriginalModelData(id: String, record: JsObject)

case class OriginalModelSearch(total: Int, list: Array[OriginalModelData])

case class ReOriginalSearch(isSuccess: Boolean = true, data: OriginalModelSearch)

case class ReDeleteOriginModel(isSuccess: Boolean = true, data: JsObject)

@Api("originalModel")
@Singleton
class OriginalModelController @Inject()(esClient: EsClient,
                                        ws: WSClient, environment: Environment, apiAction: ApiAction,
                                        config: Configuration, cc: ControllerComponents)(implicit executionContext: ExecutionContext) extends AbstractController(cc) with JsonFormat {

  val logger = LoggerFactory.getLogger("application")

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.AddOriginalModelRecord", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "add one model", response = classOf[ReMsg])
  def add() = apiAction(parse.json[AddOriginalModelRecord]) { request =>
    val read = request.body
    val jo = Json.toJson(read).as[JsObject]
    val now = Json.toJson(PlayUtils.getNow())
    val newM = jo.value ++ Map("create_time" -> now, "update_time" -> now)
    val newJo = Json.toJson(newM).as[JsObject]
    logger.info("newJo " + newJo.toString())
    val id = UUID.randomUUID().toString
    val re = esClient.updateOriginalModel(id, newJo)
    Ok(Json.toJson(ReMsg(true, id)))
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.UpdateOriginalModelRecord", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "update one model", response = classOf[ReMsg])
  def update(id: String) = apiAction(parse.json[UpdateOriginalModelRecord]) { request =>
    val read = request.body
    val jo = Json.toJson(read).as[JsObject]
    val now = Json.toJson(PlayUtils.getNow())
    val newM = jo.value ++ Map("update_time" -> now)
    val newJo = Json.toJson(newM).as[JsObject]
    val re = esClient.updateOriginalModel(id, newJo)
    Ok(Json.toJson(ReMsg(true, re)))
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "third.EsFilter", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "search model record", response = classOf[ReOriginalSearch])
  def search() = apiAction(parse.json[EsFilter]) { request =>
    val read = request.body
    val result = esClient.searchOriginalModel(read)
    val datas = result.records.map(item => {
      OriginalModelData(item._1, item._2)
    })
    Ok(Json.toJson(ReOriginalSearch(data = OriginalModelSearch(result.total, datas))))
  }

  @ApiOperation(value = "delete one model record", response = classOf[ReDeleteOriginModel])
  def delete(id: String) = apiAction { request =>
    val read = request.body
    val j = esClient.deleteOriginalModel(id)
    //de
    Ok(Json.toJson(ReDeleteOriginModel(data = j)))
  }
}
