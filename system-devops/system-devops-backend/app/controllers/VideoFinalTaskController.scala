package controllers

import auto.VideoFinalTask._
import io.swagger.annotations._
import javax.inject.{Inject, Singleton}
import models.{VideoContentTaskSqler, VideoEndingTaskSqler, VideoFinalTaskSqler}
import play.api.libs.json.Json
import play.api.libs.ws._
import play.api.mvc.ControllerComponents
import play.api.{Configuration, Environment}
import scala.concurrent.ExecutionContext


case class VideoFinalTaskExtend(
                                 idContent: String,
                                 idEnding: String,
                                 videoValue: String = "",
                                 creator: String = "",
                                 language: String = ""
                               )


case class VideoFinalStatus(status: String = "")

case class VideoFinalUrlContent(urlContent: String = "")

case class VideoStatusAndResultPath(status: String = "", resultPath: String = "")

case class VideoFinalUpdateFields(urlEnding: String)


case class VideoFinalTaskDataExtend(
                                     id: Int,
                                     createTime: java.sql.Timestamp,
                                     updateTime: java.sql.Timestamp,
                                     name: String,
                                     idContent: Int,
                                     idEnding: Int,
                                     nameContent: String,
                                     nameEnding: String,
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

case class ReVideoFinalTaskExtend(isSuccess: Boolean = true, data: List[VideoFinalTaskDataExtend] = List[VideoFinalTaskDataExtend]())

@Api("videoFinalTask")
@Singleton
class VideoFinalTaskController @Inject()(videoFinalTaskSpler: VideoFinalTaskSqler, videoContentTaskSqler: VideoContentTaskSqler, videoEndingTaskSqler: VideoEndingTaskSqler,
                                         ws: WSClient, environment: Environment, apiAction: ApiAction,
                                         config: Configuration, cc: ControllerComponents)(implicit executionContext: ExecutionContext)
  extends InnerVideoFinalTaskController(videoFinalTaskSpler, ws, environment, apiAction, config, cc)(executionContext) with JsonFormat {


  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.VideoFinalStatus", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "update VideoFinalTask", response = classOf[ReId])
  def updateVideoFinalStatus(id: Int) = apiAction(parse.json[VideoFinalStatus]).async { request =>
    val read = request.body
    videoFinalTaskSpler.updateVideoFinalStatus(
      id,
      read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }


  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.VideoFinalUrlContent", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "update VideoFinalTask", response = classOf[ReId])
  def updateUrlContent(id: Int) = apiAction(parse.json[VideoFinalUrlContent]).async { request =>
    val read = request.body
    videoFinalTaskSpler.updateVideoFinalUrlContent(
      id,
      read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }


  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.VideoFinalUpdateFields", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "update VideoFinalUpdateFields", response = classOf[ReId])
  def updateFinalFields(id: Int) = apiAction(parse.json[VideoFinalUpdateFields]).async { request =>
    val read = request.body
    videoFinalTaskSpler.updateVideoFinalFields(
      id,
      read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.VideoStatusAndResultPath", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "update VideoStatusAndResultPath", response = classOf[ReId])
  def updateStatusAndResultPath(id: Int) = apiAction(parse.json[VideoStatusAndResultPath]).async { request =>
    val read = request.body
    videoFinalTaskSpler.updateStatusAndResultPath(
      id,
      read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }


  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.VideoFinalTaskExtend", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "add VideoFinalTask", response = classOf[ReId])
  def addVideoFinalTaskCreate() = apiAction(parse.json[VideoFinalTaskExtend]).async { request =>
    val body = request.body
    val name = System.currentTimeMillis().toString
    val read: VideoFinalTaskRead = VideoFinalTaskRead(name, body.idContent, body.idEnding, "TASK_VIDEO_STATUS_CONTENT_NEW",
      "", "", "", body.videoValue, body.creator, "", "", "", "", body.language, "")
    videoFinalTaskSpler.addVideoFinalTask(read).map(item => {
      Ok(Json.toJson(ReId(data = item)))
    })
  }

  @ApiOperation(value = "if need  render ending mp4", response = classOf[ReNeed])
  def needRenderEnding(id: Int) = apiAction { request =>
    val read = request.body
    val url = videoFinalTaskSpler.needRenderEnding(id)
    var needData = ReNeedData()
    if (url != "" && url != null) {
      needData = ReNeedData(true, url)
    }
    Ok(Json.toJson(ReNeed(data = needData)))
  }

  @ApiOperation(value = "if need  render content mp4", response = classOf[ReNeed])
  def needRenderContent(id: Int) = apiAction { request =>
    val read = request.body
    val url = videoFinalTaskSpler.needRenderContent(id)
    var needData = ReNeedData()
    if (url != "" && url != null) {
      needData = ReNeedData(true, url)
    }
    Ok(Json.toJson(ReNeed(data = needData)))
  }


  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "auto.VideoFinalTask.VideoFinalTaskListRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "query VideoFinalTask list", response = classOf[ReVideoFinalTaskExtend])
  override def getList() = apiAction(parse.json[VideoFinalTaskListRead]) {
    request =>
      val read = request.body
      val list = videoFinalTaskSpler.findList(read)
      val resultList: Seq[VideoFinalTaskDataExtend] = list.map(one => {
        val contentId = one.idContent.toInt
        val contentList = videoContentTaskSqler.findByIdSync(contentId)
        var content: String = ""
        var ending: String = ""
        var idContent: Int = 0
        var idEnding: Int = 0
        if (contentList.isEmpty == false) {
          content = contentList(0).name
          idContent = contentList(0).id
        }
        val endingId = one.idEnding.toInt
        val endingList = videoEndingTaskSqler.findByIdSync(endingId)
        if (endingList.isEmpty == false) {
          ending = endingList(0).name
          idEnding = endingList(0).id
        }
        new VideoFinalTaskDataExtend(one.id,
          one.createTime,
          one.updateTime,
          one.name,
          if (idContent == 0) -1 else idContent,
          if (idEnding == 0) -1 else idEnding,
          if (content == "") one.idContent else content,
          if (ending == "") one.idEnding else ending,
          one.status,
          one.videoTime,
          one.preViewPath,
          one.resultPath,
          one.videoValue,
          one.creator,
          one.urlEnding,
          one.pixelLength,
          one.pixelHeight,
          one.frameRate,
          one.language,
          one.urlContent)
      })
      Ok(Json.toJson(ReVideoFinalTaskExtend(data = resultList.toList)))
  }

}
