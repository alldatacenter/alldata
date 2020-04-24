package auto.VideoContentTask

import controllers._
import io.swagger.annotations.{ApiImplicitParam, ApiImplicitParams, ApiOperation}
import models.VideoContentTaskSqler
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.{Configuration, Environment}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration


case class VideoContentTaskRead(
  aepPath: String = "",
  name: String = "",
  status: String = "",
  videoTime: String = "",
  finalTime: String,
  endingTime: String,
  preViewPath: String = "",
  resultPath: String = "",
  pixelLength: String,
  pixelHeight: String,
  frameRate: String,
  language: String,
  videoValue: String,
  tag: String
                    )

case class VideoContentTaskData(
  id: Int,
  createTime: java.sql.Timestamp,
  updateTime: java.sql.Timestamp,
  aepPath: String = "",
  name: String = "",
  status: String = "",
  videoTime: String = "",
  finalTime: String,
  endingTime: String,
  preViewPath: String = "",
  resultPath: String = "",
  pixelLength: String,
  pixelHeight: String,
  frameRate: String,
  language: String,
  videoValue: String,
  tag: String
                    )

case class ReVideoContentTask(isSuccess: Boolean = true, data: List[VideoContentTaskData] = List[VideoContentTaskData]())

case class VideoContentTaskListRead(where: Option[String], order: Option[String], offset: Int = 0, limit: Int = 100)


/**
  * Created by wulinhao on 2020/04/12.
  */
abstract class InnerVideoContentTaskController(videoContentTaskSpler: VideoContentTaskSqler,
                                    ws: WSClient, environment: Environment, apiAction: ApiAction,
                                    config: Configuration, cc: ControllerComponents)(implicit executionContext: ExecutionContext) extends AbstractController(cc) with VideoContentTaskJsonFormat {
  val duration = Duration("5000 sec")
  val appLogger: Logger = LoggerFactory.getLogger("application")

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.VideoContentTask.VideoContentTaskRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "add VideoContentTask", response = classOf[ReId])
  def addVideoContentTask() = apiAction(parse.json[VideoContentTaskRead]).async { request =>
    val read = request.body
    videoContentTaskSpler.addVideoContentTask( read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.VideoContentTask.VideoContentTaskRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "update VideoContentTask", response = classOf[ReId])
  def updateVideoContentTask(id: Int) = apiAction(parse.json[VideoContentTaskRead]).async { request =>
    val read = request.body
    videoContentTaskSpler.updateVideoContentTask(
              id,
            read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiOperation(value = "get one VideoContentTask", response = classOf[ReVideoContentTask])
  def getOne(id: Int) = apiAction { request =>
    val read = request.body
    val list = videoContentTaskSpler.findByIdSync(
              id          )
    Ok(Json.toJson(ReVideoContentTask(data = list.toList)))
  }


  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.VideoContentTask.VideoContentTaskListRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "query VideoContentTask list", response = classOf[ReVideoContentTask])
  def getList() = apiAction(parse.json[VideoContentTaskListRead]) {
    request =>
      val read = request.body
      val list = videoContentTaskSpler.findList(read)
      Ok(Json.toJson(ReVideoContentTask(data = list.toList)))
  }

  @ApiOperation(value = "delete VideoContentTask", response = classOf[ReId])
  def deleteVideoContentTask(id: Int) = apiAction.async {
    request =>
      videoContentTaskSpler.deleteVideoContentTask(
                  id              ).map(item => {
        Ok(Json.toJson(ReId(data = item)))
      })
  }

}
