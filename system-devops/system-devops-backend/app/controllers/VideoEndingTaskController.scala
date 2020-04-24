package controllers

import auto.VideoEndingTask._
import io.swagger.annotations._
import javax.inject.{Inject, Singleton}
import models.VideoEndingTaskSqler
import play.api.libs.json.Json
import play.api.libs.ws._
import play.api.mvc.ControllerComponents
import play.api.{Configuration, Environment}

import scala.concurrent.ExecutionContext


case class VideoEndingUpdateFields(pixelLength: String = "", pixelHeight: String = "", frameRate: String, videoTime: String, resultPath: String)

case class VideoEndingStatus(status: String = "")

case class VideoEndingVideoValue(videoValue: String = "")

case class VideoEndingTasExtend(
                                 aepPath: String = "",
                                 name: String = "",
                                 videoTime: String = "",
                                 videoValue: String = "",
                                 preViewPath: String = "",
                                 resultPath: String = "",
                                 pixelLength: String,
                                 pixelHeight: String,
                                 frameRate: String,
                                 language: String
                               )

case class VideoEndingTaskAepPath(aepPath: String = "")

case class VideoEndingTag(id: String, tag: String)

@Api("videoEndingTask")
@Singleton
class VideoEndingTaskController @Inject()(videoEndingTaskSpler: VideoEndingTaskSqler,
                                          ws: WSClient, environment: Environment, apiAction: ApiAction,
                                          config: Configuration, cc: ControllerComponents)(implicit executionContext: ExecutionContext)
  extends InnerVideoEndingTaskController(videoEndingTaskSpler, ws, environment, apiAction, config, cc)(executionContext) with JsonFormat {


  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.VideoEndingUpdateFields", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "update VideoEndingUpdateFields", response = classOf[ReId])
  def updateEndingFields(id: Int) = apiAction(parse.json[VideoEndingUpdateFields]).async { request =>
    val read = request.body
    videoEndingTaskSpler.updateVideoEndingFields(
      id,
      read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }


  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.VideoEndingStatus", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "update VideoEndingUpdateFields", response = classOf[ReId])
  def updateVideoEndingStatus(id: Int) = apiAction(parse.json[VideoEndingStatus]).async { request =>
    val read = request.body
    videoEndingTaskSpler.updateVideoEndingStatus(
      id,
      read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.VideoEndingVideoValue", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "update VideoEndingVideoValue", response = classOf[ReId])
  def updateVideoValue(id: Int) = apiAction(parse.json[VideoEndingVideoValue]).async { request =>
    val read = request.body
    videoEndingTaskSpler.updateVideoVideoValue(
      id,
      read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.VideoEndingTasExtend", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "add VideoEndingTask", response = classOf[ReId])
  def addVideoEndingTaskExtend() = apiAction(parse.json[VideoEndingTasExtend]).async { request =>
    val body = request.body
    val read = VideoEndingTaskRead(body.aepPath, body.name, body.videoTime, body.videoValue, body.preViewPath, body.resultPath,
      body.pixelLength, body.pixelHeight, body.frameRate, VideoTaskStatus.TASK_VIDEO_STATUS_CONTENT_NEW, body.language, "")
    videoEndingTaskSpler.addVideoEndingTask(read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }


  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.VideoEndingTag", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "add VideoEndingTask", response = classOf[ReId])
  def addTag() = apiAction(parse.json[VideoEndingTag]).async { request =>
    val read = request.body
    videoEndingTaskSpler.updateVideoEndingTag(read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }


  @ApiOperation(value = "delete VideoEndingTask", response = classOf[ReId])
  def deleteVideoEndingTaskExtend(aepPath: String) = apiAction.async {
    request =>
      videoEndingTaskSpler.deleteVideoEndingTaskExtend(aepPath).map(item => {
        Ok(Json.toJson(ReId(data = item)))
      })
  }


  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.VideoEndingTaskAepPath", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "delete VideoContentTask", response = classOf[ReId])
  def deleteVideoEndingByAepPath() = apiAction(parse.json[VideoEndingTaskAepPath]).async { request =>
    val read = request.body
    videoEndingTaskSpler.deleteVideoEndingTaskExtend(read.aepPath).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiOperation(value = "delete VideoEndingTask", response = classOf[ReId])
  override def deleteVideoEndingTask(id: Int) = apiAction.async {
    request =>
      videoEndingTaskSpler.deleteVideoEndingTask(id).map(item => {
        Ok(Json.toJson(ReId(data = item)))
      })
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.VideoEndingTask.VideoEndingTaskListRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "query VideoEndingTask list", response = classOf[ReVideoEndingTask])
  override def getList() = apiAction(parse.json[VideoEndingTaskListRead]) {
    request =>
      val read = request.body
      val list = videoEndingTaskSpler.findList(read)
      Ok(Json.toJson(ReVideoEndingTask(data = list.toList)))
  }
}
