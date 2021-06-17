package controllers

import auto.VideoContentTask._
import io.swagger.annotations._
import javax.inject.{Inject, Singleton}
import models.VideoContentTaskSqler
import play.api.libs.json.Json
import play.api.libs.ws._
import play.api.mvc.ControllerComponents
import play.api.{Configuration, Environment}

import scala.concurrent.ExecutionContext

case class VideoContentTaskStatus(status: String = "")

case class VideoContentUpdateFields(resultPath: String = "", pixelLength: String = "", pixelHeight: String = "", frameRate: String,
                                    videoTime: String)

case class VideoFinalAndEndingTime(finalTime: String = "", endingTime: String = "")

case class VideoContentTaskAepPath(aepPath: String = "")
case class VideoContentTag(id: String, tag: String)

@Api("videoContentTask")
@Singleton
class VideoContentTaskController @Inject()(videoContentTaskSpler: VideoContentTaskSqler,
                                           ws: WSClient, environment: Environment, apiAction: ApiAction,
                                           config: Configuration, cc: ControllerComponents)(implicit executionContext: ExecutionContext)
  extends InnerVideoContentTaskController(videoContentTaskSpler, ws, environment, apiAction, config, cc)(executionContext) with JsonFormat {

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.VideoContentTaskStatus", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "update VideoContentTask", response = classOf[ReId])
  def updateVideoContentStatus(id: Int) = apiAction(parse.json[VideoContentTaskStatus]).async { request =>
    val read = request.body
    videoContentTaskSpler.updateVideoContentStatus(
      id,
      read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.VideoContentUpdateFields", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "update VideoContentUpdateFields", response = classOf[ReId])
  def updateContentFields(id: Int) = apiAction(parse.json[VideoContentUpdateFields]).async { request =>
    val read = request.body
    videoContentTaskSpler.updateVideoContentFields(
      id,
      read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.VideoFinalAndEndingTime", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "update VideoFinalAndEndingTime", response = classOf[ReId])
  def updateFinalAndEndTime(id: Int) = apiAction(parse.json[VideoFinalAndEndingTime]).async { request =>
    val read = request.body
    videoContentTaskSpler.updateVideoFinalAndEndingTime(
      id,
      read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }


  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.VideoContentTaskAepPath", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "delete VideoContentTask", response = classOf[ReId])
  def deleteVideoContentTaskExtend() = apiAction(parse.json[VideoContentTaskAepPath]).async { request =>
    val read = request.body
    videoContentTaskSpler.deleteVideoContentTaskExtend(read.aepPath).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiOperation(value = "delete VideoContentTask", response = classOf[ReId])
  override def deleteVideoContentTask(id: Int) = apiAction.async {
    request =>
      videoContentTaskSpler.deleteVideoContentTask(id).map(item => {
        Ok(Json.toJson(ReId(data = item)))
      })
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.VideoContentTask.VideoContentTaskListRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "query VideoContentTask list", response = classOf[ReVideoContentTask])
  override def getList() = apiAction(parse.json[VideoContentTaskListRead]) {
    request =>
      val read = request.body
      val list = videoContentTaskSpler.findList(read)
      Ok(Json.toJson(ReVideoContentTask(data = list.toList)))
  }


  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.VideoContentTag", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "add VideoContentTag", response = classOf[ReId])
  def addTag() = apiAction(parse.json[VideoContentTag]).async { request =>
    val read = request.body
    videoContentTaskSpler.updateVideoContentTag(read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }


}


