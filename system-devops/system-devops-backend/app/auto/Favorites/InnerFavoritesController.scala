package auto.Favorites

import controllers._
import io.swagger.annotations.{ApiImplicitParam, ApiImplicitParams, ApiOperation}
import models.{FavoritesSqler, MaterialSqler}
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.{Configuration, Environment}
import third.{EsClient, OssClient}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration


case class FavoritesRead(
                          favorite: String,
                          description: String,
                          username: String
                        )

case class FavoritesCreate(
                            name: String,
                            description: String,
                            username: String
                          )

case class FavoritesData(
                          id: Int,
                          createTime: java.sql.Timestamp,
                          updateTime: java.sql.Timestamp,
                          favorite: String,
                          description: String,
                          username: String
                        )

case class ReFavorites(isSuccess: Boolean = true, data: List[FavoritesData] = List[FavoritesData]())

case class FavoritesListRead(where: Option[String], order: Option[String], offset: Int = 0, limit: Int = 100)


/**
  * Created by wulinhao on 2020/04/12.
  */
abstract class InnerFavoritesController(favoritesSpler: FavoritesSqler, materialSqler: MaterialSqler,
                                        ws: WSClient, ossClient: OssClient, environment: Environment, apiAction: ApiAction,
                                        config: Configuration, esClient: EsClient, cc: ControllerComponents)(implicit executionContext: ExecutionContext) extends AbstractController(cc) with FavoritesJsonFormat {
  val duration = Duration("5000 sec")
  val appLogger: Logger = LoggerFactory.getLogger("application")

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.Favorites.FavoritesRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "add Favorites", response = classOf[ReId])
  def addFavorites() = apiAction(parse.json[FavoritesRead]).async { request =>
    val read = request.body
    favoritesSpler.addFavorites(read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.Favorites.FavoritesRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "update Favorites", response = classOf[ReId])
  def updateFavorites(id: Int) = apiAction(parse.json[FavoritesRead]).async { request =>
    val read = request.body
    favoritesSpler.updateFavorites(
      id,
      read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiOperation(value = "get one Favorites", response = classOf[ReFavorites])
  def getOne(id: Int) = apiAction { request =>
    val read = request.body
    val list = favoritesSpler.findByIdSync(
      id)
    Ok(Json.toJson(ReFavorites(data = list.toList)))
  }


  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.Favorites.FavoritesListRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "query Favorites list", response = classOf[ReFavorites])
  def getList() = apiAction(parse.json[FavoritesListRead]) {
    request =>
      val read = request.body
      val list = favoritesSpler.findList(read)
      Ok(Json.toJson(ReFavorites(data = list.toList)))
  }

  @ApiOperation(value = "delete Favorites", response = classOf[ReId])
  def deleteFavorites(id: Int) = apiAction.async {
    request =>
      favoritesSpler.deleteFavorites(
        id).map(item => {
        Ok(Json.toJson(ReId(data = item)))
      })
  }

}
