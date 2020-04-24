package auto.VideoEndingTask

import controllers._
import io.swagger.annotations.{ApiImplicitParam, ApiImplicitParams, ApiOperation}
import models.VideoEndingTaskSqler
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.{Configuration, Environment}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration


case class VideoEndingTaskRead(
  aepPath: String = "",
  name: String = "",
  videoTime: String = "",
  videoValue: String = "",
  preViewPath: String = "",
  resultPath: String = "",
  pixelLength: String,
  pixelHeight: String,
  frameRate: String,
  status: String,
  language: String,
  tag: String
                    )

case class VideoEndingTaskData(
  id: Int,
  createTime: java.sql.Timestamp,
  updateTime: java.sql.Timestamp,
  aepPath: String = "",
  name: String = "",
  videoTime: String = "",
  videoValue: String = "",
  preViewPath: String = "",
  resultPath: String = "",
  pixelLength: String,
  pixelHeight: String,
  frameRate: String,
  status: String,
  language: String,
  tag: String
                    )

case class ReVideoEndingTask(isSuccess: Boolean = true, data: List[VideoEndingTaskData] = List[VideoEndingTaskData]())

case class VideoEndingTaskListRead(where: Option[String], order: Option[String], offset: Int = 0, limit: Int = 100)


/**
  * Created by wulinhao on 2020/04/12.
  */
abstract class InnerVideoEndingTaskController(videoEndingTaskSpler: VideoEndingTaskSqler,
                                    ws: WSClient, environment: Environment, apiAction: ApiAction,
                                    config: Configuration, cc: ControllerComponents)(implicit executionContext: ExecutionContext) extends AbstractController(cc) with VideoEndingTaskJsonFormat {
  val duration = Duration("5000 sec")
  val appLogger: Logger = LoggerFactory.getLogger("application")

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.VideoEndingTask.VideoEndingTaskRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "add VideoEndingTask", response = classOf[ReId])
  def addVideoEndingTask() = apiAction(parse.json[VideoEndingTaskRead]).async { request =>
    val read = request.body
    videoEndingTaskSpler.addVideoEndingTask( read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.VideoEndingTask.VideoEndingTaskRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "update VideoEndingTask", response = classOf[ReId])
  def updateVideoEndingTask(id: Int) = apiAction(parse.json[VideoEndingTaskRead]).async { request =>
    val read = request.body
    videoEndingTaskSpler.updateVideoEndingTask(
              id,
            read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiOperation(value = "get one VideoEndingTask", response = classOf[ReVideoEndingTask])
  def getOne(id: Int) = apiAction { request =>
    val read = request.body
    val list = videoEndingTaskSpler.findByIdSync(
              id          )
    Ok(Json.toJson(ReVideoEndingTask(data = list.toList)))
  }


  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.VideoEndingTask.VideoEndingTaskListRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "query VideoEndingTask list", response = classOf[ReVideoEndingTask])
  def getList() = apiAction(parse.json[VideoEndingTaskListRead]) {
    request =>
      val read = request.body
      val list = videoEndingTaskSpler.findList(read)
      Ok(Json.toJson(ReVideoEndingTask(data = list.toList)))
  }

  @ApiOperation(value = "delete VideoEndingTask", response = classOf[ReId])
  def deleteVideoEndingTask(id: Int) = apiAction.async {
    request =>
      videoEndingTaskSpler.deleteVideoEndingTask(
                  id              ).map(item => {
        Ok(Json.toJson(ReId(data = item)))
      })
  }

}
