package auto.VideoFinalTask

object VideoFinalTaskTables extends {
  val profile = slick.jdbc.MySQLProfile

  import profile.api._
  import slick.model.ForeignKeyAction
  import slick.jdbc.{GetResult => GR}

  lazy val schema: profile.SchemaDescription = VideoFinalTask.schema

  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema


  final case class VideoFinalTaskRow(
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

  /** GetResult implicit for fetching VideoFinalTaskRow objects using plain SQL queries */
  implicit def GetResultVideoFinalTaskRow(implicit e0: GR[Int],e1: GR[Option[Int]], e2: GR[java.sql.Timestamp],e3: GR[Option[java.sql.Timestamp]], e4: GR[String], e5: GR[Option[String]]): GR[VideoFinalTaskRow] = GR {
    prs =>
      import prs._
      VideoFinalTaskRow.tupled((
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
                  <<[String],
                  <<[String]
              ))
  }

  /** Table description of table VideoFinalTask. Objects of this class serve as prototypes for rows in queries. */
  class VideoFinalTask(_tableTag: Tag) extends profile.api.Table[VideoFinalTaskRow](_tableTag, "video_final_task") {
    def * = (
          id,
          createTime,
          updateTime,
          name,
          idContent,
          idEnding,
          status,
          videoTime,
          preViewPath,
          resultPath,
          videoValue,
          creator,
          urlEnding,
          pixelLength,
          pixelHeight,
          frameRate,
          language,
          urlContent
        ) <> (VideoFinalTaskRow.tupled, VideoFinalTaskRow.unapply)

          val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)

          val createTime: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("create_time")

          val updateTime: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("update_time")

          val name: Rep[String] = column[String]("name", O.Length(255, varying = true))

          val idContent: Rep[String] = column[String]("id_content", O.Length(10, varying = true))

          val idEnding: Rep[String] = column[String]("id_ending", O.Length(10, varying = true))

          val status: Rep[String] = column[String]("status", O.Length(64, varying = true), O.Default(""))

          val videoTime: Rep[String] = column[String]("videoTime", O.Length(64, varying = true), O.Default(""))

          val preViewPath: Rep[String] = column[String]("preViewPath", O.Length(512, varying = true), O.Default(""))

          val resultPath: Rep[String] = column[String]("resultPath", O.Length(1024, varying = true), O.Default(""))

          val videoValue: Rep[String] = column[String]("videoValue", O.Length(512, varying = true), O.Default(""))

          val creator: Rep[String] = column[String]("creator", O.Length(64, varying = true), O.Default(""))

          val urlEnding: Rep[String] = column[String]("urlEnding", O.Length(1024, varying = true))

          val pixelLength: Rep[String] = column[String]("pixel_length", O.Length(64, varying = true))

          val pixelHeight: Rep[String] = column[String]("pixel_height", O.Length(64, varying = true))

          val frameRate: Rep[String] = column[String]("frame_rate", O.Length(64, varying = true))

          val language: Rep[String] = column[String]("language", O.Length(255, varying = true))

          val urlContent: Rep[String] = column[String]("urlContent", O.Length(2048, varying = true))

      }

  lazy val VideoFinalTask = new TableQuery(tag => new VideoFinalTask(tag))
}
