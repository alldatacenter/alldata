package auto.OriginalLevel1

import controllers._
import io.swagger.annotations.{ApiImplicitParam, ApiImplicitParams, ApiOperation}
import models.OriginalLevel1Sqler
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.{Configuration, Environment}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration


case class OriginalLevel1Read(
  originalGameId: Int,
  name: String
                    )

case class OriginalLevel1Data(
  id: Int,
  createTime: java.sql.Timestamp,
  updateTime: java.sql.Timestamp,
  originalGameId: Int,
  name: String
                    )

case class ReOriginalLevel1(isSuccess: Boolean = true, data: List[OriginalLevel1Data] = List[OriginalLevel1Data]())

case class OriginalLevel1ListRead(where: Option[String], order: Option[String], offset: Int = 0, limit: Int = 100)


/**
  * Created by wulinhao on 2020/04/12.
  */
abstract class InnerOriginalLevel1Controller(OriginalLevel1Spler: OriginalLevel1Sqler,
                                    ws: WSClient, environment: Environment, apiAction: ApiAction,
                                    config: Configuration, cc: ControllerComponents)(implicit executionContext: ExecutionContext) extends AbstractController(cc) with OriginalLevel1JsonFormat {
  val duration = Duration("5000 sec")
  val appLogger: Logger = LoggerFactory.getLogger("application")

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.OriginalLevel1.OriginalLevel1Read", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "add OriginalLevel1", response = classOf[ReId])
  def addOriginalLevel1() = apiAction(parse.json[OriginalLevel1Read]).async { request =>
    val read = request.body
    OriginalLevel1Spler.addOriginalLevel1( read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.OriginalLevel1.OriginalLevel1Read", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "update OriginalLevel1", response = classOf[ReId])
  def updateOriginalLevel1(id: Int) = apiAction(parse.json[OriginalLevel1Read]).async { request =>
    val read = request.body
    OriginalLevel1Spler.updateOriginalLevel1(
              id,
            read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiOperation(value = "get one OriginalLevel1", response = classOf[ReOriginalLevel1])
  def getOne(id: Int) = apiAction { request =>
    val read = request.body
    val list = OriginalLevel1Spler.findByIdSync(
              id          )
    Ok(Json.toJson(ReOriginalLevel1(data = list.toList)))
  }


  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.OriginalLevel1.OriginalLevel1ListRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "query OriginalLevel1 list", response = classOf[ReOriginalLevel1])
  def getList() = apiAction(parse.json[OriginalLevel1ListRead]) {
    request =>
      val read = request.body
      val list = OriginalLevel1Spler.findList(read)
      Ok(Json.toJson(ReOriginalLevel1(data = list.toList)))
  }

  @ApiOperation(value = "delete OriginalLevel1", response = classOf[ReId])
  def deleteOriginalLevel1(id: Int) = apiAction.async {
    request =>
      OriginalLevel1Spler.deleteOriginalLevel1(
                  id              ).map(item => {
        Ok(Json.toJson(ReId(data = item)))
      })
  }

}
