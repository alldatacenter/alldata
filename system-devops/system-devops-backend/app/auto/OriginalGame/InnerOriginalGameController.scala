package auto.OriginalGame

import controllers._
import io.swagger.annotations.{ApiImplicitParam, ApiImplicitParams, ApiOperation}
import models.OriginalGameSqler
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.{Configuration, Environment}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration


case class OriginalGameRead(
  name: String
                    )

case class OriginalGameData(
  id: Int,
  createTime: java.sql.Timestamp,
  updateTime: java.sql.Timestamp,
  name: String
                    )

case class ReOriginalGame(isSuccess: Boolean = true, data: List[OriginalGameData] = List[OriginalGameData]())

case class OriginalGameListRead(where: Option[String], order: Option[String], offset: Int = 0, limit: Int = 100)


/**
  * Created by wulinhao on 2020/04/12.
  */
abstract class InnerOriginalGameController(OriginalGameSpler: OriginalGameSqler,
                                    ws: WSClient, environment: Environment, apiAction: ApiAction,
                                    config: Configuration, cc: ControllerComponents)(implicit executionContext: ExecutionContext) extends AbstractController(cc) with OriginalGameJsonFormat {
  val duration = Duration("5000 sec")
  val appLogger: Logger = LoggerFactory.getLogger("application")

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.OriginalGame.OriginalGameRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "add OriginalGame", response = classOf[ReId])
  def addOriginalGame() = apiAction(parse.json[OriginalGameRead]).async { request =>
    val read = request.body
    OriginalGameSpler.addOriginalGame( read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.OriginalGame.OriginalGameRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "update OriginalGame", response = classOf[ReId])
  def updateOriginalGame(id: Int) = apiAction(parse.json[OriginalGameRead]).async { request =>
    val read = request.body
    OriginalGameSpler.updateOriginalGame(
              id,
            read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiOperation(value = "get one OriginalGame", response = classOf[ReOriginalGame])
  def getOne(id: Int) = apiAction { request =>
    val read = request.body
    val list = OriginalGameSpler.findByIdSync(
              id          )
    Ok(Json.toJson(ReOriginalGame(data = list.toList)))
  }


  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.OriginalGame.OriginalGameListRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "query OriginalGame list", response = classOf[ReOriginalGame])
  def getList() = apiAction(parse.json[OriginalGameListRead]) {
    request =>
      val read = request.body
      val list = OriginalGameSpler.findList(read)
      Ok(Json.toJson(ReOriginalGame(data = list.toList)))
  }

  @ApiOperation(value = "delete OriginalGame", response = classOf[ReId])
  def deleteOriginalGame(id: Int) = apiAction.async {
    request =>
      OriginalGameSpler.deleteOriginalGame(
                  id              ).map(item => {
        Ok(Json.toJson(ReId(data = item)))
      })
  }

}
