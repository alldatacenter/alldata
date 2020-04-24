package controllers

import auto.Favorites._
import io.swagger.annotations._
import javax.inject.{Inject, Singleton}
import models.{FavoritesSqler, MaterialSqler}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws._
import play.api.mvc.ControllerComponents
import play.api.{Configuration, Environment}
import third.{EsClient, OssClient}

import scala.concurrent.{ExecutionContext, Future}


case class FavoriteData(id: String, record: JsObject, zip: String,
                        thumbnail: String, rollThumbnail: String, glbKey: String)

case class FavoriteResult(id: Int, name: String, list: collection.mutable.ArrayBuffer[FavoriteData])

case class ReSearchFavorites(isSuccess: Boolean = true, data: Array[FavoriteResult])



case class FavoritesUpdateRead(name: String, username: String)


@Api("favorites")
@Singleton
class FavoritesController @Inject()(favoritesSpler: FavoritesSqler, materialSqler: MaterialSqler,
                                    ws: WSClient, ossClient: OssClient, environment: Environment, apiAction: ApiAction,
                                    config: Configuration, esClient: EsClient, cc: ControllerComponents)(implicit executionContext: ExecutionContext)
  extends InnerFavoritesController(favoritesSpler, materialSqler, ws, ossClient, environment, apiAction, config, esClient, cc)(executionContext) with JsonFormat {
  val logger = LoggerFactory.getLogger("application")

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.Favorites.FavoritesCreate", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "add Favorites", response = classOf[ReId])
  def addFavoritesCreate() = apiAction(parse.json[FavoritesCreate]).async { request =>
    val read = request.body
    favoritesSpler.addFavoritesCreate(read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.Favorites.FavoritesListRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "query Favorites list", response = classOf[ReSearchFavorites])
  override def getList() = apiAction(parse.json[FavoritesListRead]) {
    request =>
      val read = request.body
      val list = favoritesSpler.findList(read)
      val resultList = collection.mutable.ArrayBuffer[FavoriteResult]()
      try {
        for (e <- list) {
          val items = materialSqler.findItems(e.id);
          val itemList = collection.mutable.ArrayBuffer[FavoriteData]()
          items.map(materialData => {
            val item = materialData.itemId
            if (!item.equals("") && item != null) {
              val result = esClient.searchFavoritesModel(item)
              val paths = ossClient.getModelPath(result._2)
              itemList.+=(FavoriteData(result._1, result._2, paths._1, paths._2, paths._3, paths._4))
            }
          })
          resultList.+=(FavoriteResult(e.id, e.favorite, itemList))
        }
        Ok(Json.toJson(ReSearchFavorites(data = resultList.toArray)))
      } catch {
        case e: Exception =>
          logger.info(e.getMessage)
          Ok(Json.toJson(ReSearchFavorites(data = resultList.toArray)))
      }
  }

  @ApiOperation(value = "delete Favorites", response = classOf[ReId])
  override def deleteFavorites(id: Int) = apiAction.async {
    request =>
      val itemList = materialSqler.findItems(id)
      favoritesSpler.deleteFavoritesAndMaterials(id, materialSqler).map(item => {
        Ok(Json.toJson(ReId(data = item)))
      })
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.FavoritesUpdateRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "update Favorites", response = classOf[ReId])
  def updateFavoritesRead(id: Int) = apiAction(parse.json[FavoritesUpdateRead]).async {
    request =>
      val read = request.body
      try {
        favoritesSpler.updateFavoritesRead(
          id,
          read).map(item => {
          Ok(Json.toJson(ReId(data = item)))
        })
      } catch {
        case e: Exception =>
          logger.info(e.getMessage)
          Future.successful(
            Ok(Json.toJson(ReId(data = -1)))
          )
      }
  }
}
