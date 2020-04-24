package controllers

import auto.Material._
import io.swagger.annotations._
import javax.inject.{Inject, Singleton}
import models.MaterialSqler
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import play.api.libs.ws._
import play.api.mvc.ControllerComponents
import play.api.{Configuration, Environment}

import scala.concurrent.{ExecutionContext, Future}

case class FavoriteItemRead(id: Int, items: String)


@Api("material")
@Singleton
class MaterialController @Inject()(materialSpler: MaterialSqler,
                                   ws: WSClient, environment: Environment, apiAction: ApiAction,
                                   config: Configuration, cc: ControllerComponents)(implicit executionContext: ExecutionContext)
  extends InnerMaterialController(materialSpler, ws, environment, apiAction, config, cc)(executionContext) with JsonFormat {
  val logger = LoggerFactory.getLogger("application")

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.Material.MaterialCreate", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "add Material", response = classOf[ReId])
  def addMaterialCreate() = apiAction(parse.json[MaterialCreate]).async { request =>
    val read = request.body
    materialSpler.addMaterialCreate(read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }


  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.FavoriteItemRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "delete Favorites item", response = classOf[ReIdArray])
  def deleteFavoriteItem() = apiAction(parse.json[FavoriteItemRead]) { request =>
    val read = request.body
    try {
      val reArray = materialSpler.deleteMaterialFromFavorite(read)
      Ok(Json.toJson(ReIdArray(data = reArray)))
    } catch {
      case e: Exception =>
        logger.info(e.getMessage)
        Ok(Json.toJson(ReIdArray(data = Array[Int]())))

    }
  }

}
