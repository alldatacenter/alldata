package auto.VideoEndingTask

import auto.VideoEndingTask.VideoEndingTaskTables._
import controllers.BeforeInnerJsonFormat
import play.api.libs.json._
import controllers._

trait VideoEndingTaskJsonFormat extends BeforeInnerJsonFormat {

  implicit def changeVideoEndingTaskDataToVideoEndingTaskRow(read: VideoEndingTaskData): VideoEndingTaskRow = {
    VideoEndingTaskRow(
      read.id,
      read.createTime,
      read.updateTime,
      read.aepPath,
      read.name,
      read.videoTime,
      read.videoValue,
      read.preViewPath,
      read.resultPath,
      read.pixelLength,
      read.pixelHeight,
      read.frameRate,
      read.status,
      read.language,
      read.tag
    )
  }

  implicit def changeVideoEndingTaskRowToVideoEndingTaskData(read: VideoEndingTaskRow): VideoEndingTaskData = {
     VideoEndingTaskData(
              read.id,
              read.createTime,
              read.updateTime,
              read.aepPath,
              read.name,
              read.videoTime,
              read.videoValue,
              read.preViewPath,
              read.resultPath,
              read.pixelLength,
              read.pixelHeight,
              read.frameRate,
              read.status,
              read.language,
              read.tag
          )
  }

  implicit def changeVideoEndingTaskRowSeqToVideoEndingTaskDataSeq(data: Seq[VideoEndingTaskRow]): Seq[VideoEndingTaskData] = {
    data.map(a => changeVideoEndingTaskRowToVideoEndingTaskData(a))
  }

  implicit def changeVideoEndingTaskDataSeqToVideoEndingTaskRowSeq(data: Seq[VideoEndingTaskData]): Seq[VideoEndingTaskRow] = {
    data.map(a => changeVideoEndingTaskDataToVideoEndingTaskRow(a))
  }


  implicit val VideoEndingTaskDataFormat = Json.format[VideoEndingTaskData]
  implicit val VideoEndingTaskReadFormat = Json.using[Json.WithDefaultValues].format[VideoEndingTaskRead]
  implicit val VideoEndingTaskListReadFormat = Json.format[VideoEndingTaskListRead]
  implicit val ReVideoEndingTaskFormat = Json.format[ReVideoEndingTask]
}