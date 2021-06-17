package auto.VideoContentTask

object VideoContentTaskTables extends {
  val profile = slick.jdbc.MySQLProfile

  import profile.api._
  import slick.model.ForeignKeyAction
  import slick.jdbc.{GetResult => GR}

  lazy val schema: profile.SchemaDescription = VideoContentTask.schema

  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema


  final case class VideoContentTaskRow(
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

  /** GetResult implicit for fetching VideoContentTaskRow objects using plain SQL queries */
  implicit def GetResultVideoContentTaskRow(implicit e0: GR[Int],e1: GR[Option[Int]], e2: GR[java.sql.Timestamp],e3: GR[Option[java.sql.Timestamp]], e4: GR[String], e5: GR[Option[String]]): GR[VideoContentTaskRow] = GR {
    prs =>
      import prs._
      VideoContentTaskRow.tupled((
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
                  <<[String],
                  <<[String],
                  <<[String]
              ))
  }

  /** Table description of table VideoContentTask. Objects of this class serve as prototypes for rows in queries. */
  class VideoContentTask(_tableTag: Tag) extends profile.api.Table[VideoContentTaskRow](_tableTag, "video_content_task") {
    def * = (
          id,
          createTime,
          updateTime,
          aepPath,
          name,
          status,
          videoTime,
          finalTime,
          endingTime,
          preViewPath,
          resultPath,
          pixelLength,
          pixelHeight,
          frameRate,
          language,
          videoValue,
          tag
        ) <> (VideoContentTaskRow.tupled, VideoContentTaskRow.unapply)

          val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)

          val createTime: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("create_time")

          val updateTime: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("update_time")

          val aepPath: Rep[String] = column[String]("aepPath", O.Length(512, varying = true), O.Default(""))

          val name: Rep[String] = column[String]("name", O.Length(512, varying = true), O.Default(""))

          val status: Rep[String] = column[String]("status", O.Length(64, varying = true), O.Default(""))

          val videoTime: Rep[String] = column[String]("videoTime", O.Length(64, varying = true), O.Default(""))

          val finalTime: Rep[String] = column[String]("finalTime", O.Length(64, varying = true))

          val endingTime: Rep[String] = column[String]("endingTime", O.Length(64, varying = true))

          val preViewPath: Rep[String] = column[String]("preViewPath", O.Length(512, varying = true), O.Default(""))

          val resultPath: Rep[String] = column[String]("resultPath", O.Length(1024, varying = true), O.Default(""))

          val pixelLength: Rep[String] = column[String]("pixel_length", O.Length(64, varying = true))

          val pixelHeight: Rep[String] = column[String]("pixel_height", O.Length(64, varying = true))

          val frameRate: Rep[String] = column[String]("frame_rate", O.Length(64, varying = true))

          val language: Rep[String] = column[String]("language", O.Length(255, varying = true))

          val videoValue: Rep[String] = column[String]("videoValue", O.Length(1024, varying = true))

          val tag: Rep[String] = column[String]("tag", O.Length(1024, varying = true))

      }

  lazy val VideoContentTask = new TableQuery(tag => new VideoContentTask(tag))
}
