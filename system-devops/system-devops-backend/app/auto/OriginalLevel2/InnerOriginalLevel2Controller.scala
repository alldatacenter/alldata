package auto.OriginalLevel2

import controllers._
import io.swagger.annotations.{ApiImplicitParam, ApiImplicitParams, ApiOperation}
import models.OriginalLevel2Sqler
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.{Configuration, Environment}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration


case class OriginalLevel2Read(
  originalLevel1Id: Int,
  name: String
                    )

case class OriginalLevel2Data(
  id: Int,
  createTime: java.sql.Timestamp,
  updateTime: java.sql.Timestamp,
  originalLevel1Id: Int,
  name: String
                    )

case class ReOriginalLevel2(isSuccess: Boolean = true, data: List[OriginalLevel2Data] = List[OriginalLevel2Data]())

case class OriginalLevel2ListRead(where: Option[String], order: Option[String], offset: Int = 0, limit: Int = 100)


/**
  * Created by wulinhao on 2020/04/12.
  */
abstract class InnerOriginalLevel2Controller(OriginalLevel2Spler: OriginalLevel2Sqler,
                                    ws: WSClient, environment: Environment, apiAction: ApiAction,
                                    config: Configuration, cc: ControllerComponents)(implicit executionContext: ExecutionContext) extends AbstractController(cc) with OriginalLevel2JsonFormat {
  val duration = Duration("5000 sec")
  val appLogger: Logger = LoggerFactory.getLogger("application")

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.OriginalLevel2.OriginalLevel2Read", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "add OriginalLevel2", response = classOf[ReId])
  def addOriginalLevel2() = apiAction(parse.json[OriginalLevel2Read]).async { request =>
    val read = request.body
    OriginalLevel2Spler.addOriginalLevel2( read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.OriginalLevel2.OriginalLevel2Read", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "update OriginalLevel2", response = classOf[ReId])
  def updateOriginalLevel2(id: Int) = apiAction(parse.json[OriginalLevel2Read]).async { request =>
    val read = request.body
    OriginalLevel2Spler.updateOriginalLevel2(
              id,
            read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiOperation(value = "get one OriginalLevel2", response = classOf[ReOriginalLevel2])
  def getOne(id: Int) = apiAction { request =>
    val read = request.body
    val list = OriginalLevel2Spler.findByIdSync(
              id          )
    Ok(Json.toJson(ReOriginalLevel2(data = list.toList)))
  }


  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.OriginalLevel2.OriginalLevel2ListRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "query OriginalLevel2 list", response = classOf[ReOriginalLevel2])
  def getList() = apiAction(parse.json[OriginalLevel2ListRead]) {
    request =>
      val read = request.body
      val list = OriginalLevel2Spler.findList(read)
      Ok(Json.toJson(ReOriginalLevel2(data = list.toList)))
  }

  @ApiOperation(value = "delete OriginalLevel2", response = classOf[ReId])
  def deleteOriginalLevel2(id: Int) = apiAction.async {
    request =>
      OriginalLevel2Spler.deleteOriginalLevel2(
                  id              ).map(item => {
        Ok(Json.toJson(ReId(data = item)))
      })
  }

}
