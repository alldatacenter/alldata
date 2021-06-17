package auto.Material

import controllers._
import io.swagger.annotations.{ApiImplicitParam, ApiImplicitParams, ApiOperation}
import models.MaterialSqler
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.{Configuration, Environment}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration


case class MaterialRead(
                         itemId: String,
                         favoriteId: Int
                       )

case class MaterialCreate(
                           id: Int,
                           items: String
                         )

case class MaterialData(
                         id: Int,
                         createTime: java.sql.Timestamp,
                         updateTime: java.sql.Timestamp,
                         itemId: String,
                         favoriteId: Int
                       )

case class ReMaterial(isSuccess: Boolean = true, data: List[MaterialData] = List[MaterialData]())

case class MaterialListRead(where: Option[String], order: Option[String], offset: Int = 0, limit: Int = 100)


/**
  * Created by wulinhao on 2020/04/12.
  */
abstract class InnerMaterialController(materialSpler: MaterialSqler,
                                       ws: WSClient, environment: Environment, apiAction: ApiAction,
                                       config: Configuration, cc: ControllerComponents)(implicit executionContext: ExecutionContext) extends AbstractController(cc) with MaterialJsonFormat {
  val duration = Duration("5000 sec")
  val appLogger: Logger = LoggerFactory.getLogger("application")

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.Material.MaterialRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "add Material", response = classOf[ReId])
  def addMaterial() = apiAction(parse.json[MaterialRead]).async { request =>
    val read = request.body
    materialSpler.addMaterial(read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.Material.MaterialRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "update Material", response = classOf[ReId])
  def updateMaterial(id: Int) = apiAction(parse.json[MaterialRead]).async { request =>
    val read = request.body
    materialSpler.updateMaterial(
      id,
      read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiOperation(value = "get one Material", response = classOf[ReMaterial])
  def getOne(id: Int) = apiAction { request =>
    val read = request.body
    val list = materialSpler.findByIdSync(
      id)
    Ok(Json.toJson(ReMaterial(data = list.toList)))
  }


  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.Material.MaterialListRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "query Material list", response = classOf[ReMaterial])
  def getList() = apiAction(parse.json[MaterialListRead]) {
    request =>
      val read = request.body
      val list = materialSpler.findList(read)
      Ok(Json.toJson(ReMaterial(data = list.toList)))
  }

  @ApiOperation(value = "delete Material", response = classOf[ReId])
  def deleteMaterial(id: Int) = apiAction.async {
    request =>
      materialSpler.deleteMaterial(
        id).map(item => {
        Ok(Json.toJson(ReId(data = item)))
      })
  }

}
