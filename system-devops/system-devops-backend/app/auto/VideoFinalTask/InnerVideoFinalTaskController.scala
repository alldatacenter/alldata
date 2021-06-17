package auto.VideoFinalTask

import controllers._
import io.swagger.annotations.{ApiImplicitParam, ApiImplicitParams, ApiOperation}
import models.VideoFinalTaskSqler
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.{Configuration, Environment}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration


case class VideoFinalTaskRead(
                               name: String,
                               idContent: String,
                               idEnding: String,
                               status: String = "",
                               videoTime: String = "",
                               preViewPath: String = "",
                               resultPath: String = "",
                               videoValue: String = "",
                               creator: String = "",
                               urlEnding: String,
                               pixelLength: String,
                               pixelHeight: String,
                               frameRate: String,
                               language: String,
                               urlContent: String
                             )

case class VideoFinalTaskData(
                               id: Int,
                               createTime: java.sql.Timestamp,
                               updateTime: java.sql.Timestamp,
                               name: String,
                               idContent: String,
                               idEnding: String,
                               status: String = "",
                               videoTime: String = "",
                               preViewPath: String = "",
                               resultPath: String = "",
                               videoValue: String = "",
                               creator: String = "",
                               urlEnding: String,
                               pixelLength: String,
                               pixelHeight: String,
                               frameRate: String,
                               language: String,
                               urlContent: String
                             )


case class ReVideoFinalTask(isSuccess: Boolean = true, data: List[VideoFinalTaskData] = List[VideoFinalTaskData]())

case class VideoFinalTaskListRead(where: Option[String], order: Option[String], offset: Int = 0, limit: Int = 100)


/**
  * Created by wulinhao on 2020/04/12.
  */
abstract class InnerVideoFinalTaskController(videoFinalTaskSpler: VideoFinalTaskSqler,
                                             ws: WSClient, environment: Environment, apiAction: ApiAction,
                                             config: Configuration, cc: ControllerComponents)(implicit executionContext: ExecutionContext) extends AbstractController(cc) with VideoFinalTaskJsonFormat {
  val duration = Duration("5000 sec")
  val appLogger: Logger = LoggerFactory.getLogger("application")

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.VideoFinalTask.VideoFinalTaskRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "add VideoFinalTask", response = classOf[ReId])
  def addVideoFinalTask() = apiAction(parse.json[VideoFinalTaskRead]).async { request =>
    val read = request.body
    videoFinalTaskSpler.addVideoFinalTask(read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.VideoFinalTask.VideoFinalTaskRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "update VideoFinalTask", response = classOf[ReId])
  def updateVideoFinalTask(id: Int) = apiAction(parse.json[VideoFinalTaskRead]).async { request =>
    val read = request.body
    videoFinalTaskSpler.updateVideoFinalTask(
      id,
      read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiOperation(value = "get one VideoFinalTask", response = classOf[ReVideoFinalTask])
  def getOne(id: Int) = apiAction { request =>
    val read = request.body
    val list = videoFinalTaskSpler.findByIdSync(
      id)
    Ok(Json.toJson(ReVideoFinalTask(data = list.toList)))
  }


  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.VideoFinalTask.VideoFinalTaskListRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "query VideoFinalTask list", response = classOf[ReVideoFinalTask])
  def getList() = apiAction(parse.json[VideoFinalTaskListRead]) {
    request =>
      val read = request.body
      val list = videoFinalTaskSpler.findList(read)
      Ok(Json.toJson(ReVideoFinalTask(data = list.toList)))
  }

  @ApiOperation(value = "delete VideoFinalTask", response = classOf[ReId])
  def deleteVideoFinalTask(id: Int) = apiAction.async {
    request =>
      videoFinalTaskSpler.deleteVideoFinalTask(
        id).map(item => {
        Ok(Json.toJson(ReId(data = item)))
      })
  }

}
