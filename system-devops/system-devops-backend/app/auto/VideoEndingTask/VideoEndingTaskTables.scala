package auto.VideoEndingTask

object VideoEndingTaskTables extends {
  val profile = slick.jdbc.MySQLProfile

  import profile.api._
  import slick.model.ForeignKeyAction
  import slick.jdbc.{GetResult => GR}

  lazy val schema: profile.SchemaDescription = VideoEndingTask.schema

  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema


  final case class VideoEndingTaskRow(
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

  /** GetResult implicit for fetching VideoEndingTaskRow objects using plain SQL queries */
  implicit def GetResultVideoEndingTaskRow(implicit e0: GR[Int],e1: GR[Option[Int]], e2: GR[java.sql.Timestamp],e3: GR[Option[java.sql.Timestamp]], e4: GR[String], e5: GR[Option[String]]): GR[VideoEndingTaskRow] = GR {
    prs =>
      import prs._
      VideoEndingTaskRow.tupled((
                  <<[Int],
                  <<[java.sql.Timestamp],
                  <<[java.sql.Timestamp],
                  <<[String],
                  <<[String],
                  <<[String],
                  <<[String],
                  <<[String],
                  <<[String],
                  <<[String],
                  <<[String],
                  <<[String],
                  <<[String],
                  <<[String],
                  <<[String]
              ))
  }

  /** Table description of table VideoEndingTask. Objects of this class serve as prototypes for rows in queries. */
  class VideoEndingTask(_tableTag: Tag) extends profile.api.Table[VideoEndingTaskRow](_tableTag, "video_ending_task") {
    def * = (
          id,
          createTime,
          updateTime,
          aepPath,
          name,
          videoTime,
          videoValue,
          preViewPath,
          resultPath,
          pixelLength,
          pixelHeight,
          frameRate,
          status,
          language,
          tag
        ) <> (VideoEndingTaskRow.tupled, VideoEndingTaskRow.unapply)

          val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)

          val createTime: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("create_time")

          val updateTime: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("update_time")

          val aepPath: Rep[String] = column[String]("aepPath", O.Length(512, varying = true), O.Default(""))

          val name: Rep[String] = column[String]("name", O.Length(512, varying = true), O.Default(""))

          val videoTime: Rep[String] = column[String]("videoTime", O.Length(64, varying = true), O.Default(""))

          val videoValue: Rep[String] = column[String]("videoValue", O.Length(512, varying = true), O.Default(""))

          val preViewPath: Rep[String] = column[String]("preViewPath", O.Length(512, varying = true), O.Default(""))

          val resultPath: Rep[String] = column[String]("resultPath", O.Length(1024, varying = true), O.Default(""))

          val pixelLength: Rep[String] = column[String]("pixel_length", O.Length(64, varying = true))

          val pixelHeight: Rep[String] = column[String]("pixel_height", O.Length(64, varying = true))

          val frameRate: Rep[String] = column[String]("frame_rate", O.Length(64, varying = true))

          val status: Rep[String] = column[String]("status", O.Length(64, varying = true))

          val language: Rep[String] = column[String]("language", O.Length(255, varying = true))

          val tag: Rep[String] = column[String]("tag", O.Length(1024, varying = true))

      }

  lazy val VideoEndingTask = new TableQuery(tag => new VideoEndingTask(tag))
}
