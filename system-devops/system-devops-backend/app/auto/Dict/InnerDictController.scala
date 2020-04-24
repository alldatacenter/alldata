package auto.Dict

import controllers._
import io.swagger.annotations.{ApiImplicitParam, ApiImplicitParams, ApiOperation}
import models.DictSqler
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.{Configuration, Environment}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration


case class DictRead(
                     dictName: String,
                     dictValue: String,
                     dictDesc: String
                   )

case class DictData(
                     id: Int,
                     createTime: java.sql.Timestamp,
                     updateTime: java.sql.Timestamp,
                     dictName: String,
                     dictValue: String,
                     dictDesc: String
                   )

case class ReDict(isSuccess: Boolean = true, data: List[DictData] = List[DictData]())

case class DictListRead(where: Option[String], order: Option[String], offset: Int = 0, limit: Int = 100)


/**
  * Created by wulinhao on 2020/04/12.
  */
abstract class InnerDictController(DictSpler: DictSqler,
                                   ws: WSClient, environment: Environment, apiAction: ApiAction,
                                   config: Configuration, cc: ControllerComponents)(implicit executionContext: ExecutionContext) extends AbstractController(cc) with DictJsonFormat {
  val duration = Duration("5000 sec")
  val appLogger: Logger = LoggerFactory.getLogger("application")

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.Dict.DictRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "add Dict", response = classOf[ReId])
  def addDict() = apiAction(parse.json[DictRead]).async { request =>
    val read = request.body
    DictSpler.addDict(read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.Dict.DictRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "update Dict", response = classOf[ReId])
  def updateDict(id: Int) = apiAction(parse.json[DictRead]).async { request =>
    val read = request.body
    DictSpler.updateDict(
      id,
      read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiOperation(value = "get one Dict", response = classOf[ReDict])
  def getOne(id: Int) = apiAction { request =>
    val read = request.body
    val list = DictSpler.findByIdSync(
      id)
    Ok(Json.toJson(ReDict(data = list.toList)))
  }


  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.Dict.DictListRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "query Dict list", response = classOf[ReDict])
  def getList() = apiAction(parse.json[DictListRead]) {
    request =>
      val read = request.body
      val list = DictSpler.findList(read)
      Ok(Json.toJson(ReDict(data = list.toList)))
  }

  @ApiOperation(value = "delete Dict", response = classOf[ReId])
  def deleteDict(id: Int) = apiAction.async {
    request =>
      DictSpler.deleteDict(
        id).map(item => {
        Ok(Json.toJson(ReId(data = item)))
      })
  }

}
