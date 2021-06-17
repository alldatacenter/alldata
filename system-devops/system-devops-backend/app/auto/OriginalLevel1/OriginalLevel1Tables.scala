package auto.OriginalLevel1


object OriginalLevel1Tables extends {
  val profile = slick.jdbc.MySQLProfile

  import profile.api._
  import slick.model.ForeignKeyAction
  import slick.jdbc.{GetResult => GR}

  lazy val schema: profile.SchemaDescription = OriginalLevel1.schema

  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema


  final case class OriginalLevel1Row(
      id: Int,
      createTime: java.sql.Timestamp,
      updateTime: java.sql.Timestamp,
      originalGameId: Int,
      name: String
                                     )

  /** GetResult implicit for fetching OriginalLevel1Row objects using plain SQL queries */
  implicit def GetResultOriginalLevel1Row(implicit e0: GR[Int],e1: GR[Option[Int]], e2: GR[java.sql.Timestamp],e3: GR[Option[java.sql.Timestamp]], e4: GR[String], e5: GR[Option[String]]): GR[OriginalLevel1Row] = GR {
    prs =>
      import prs._
      OriginalLevel1Row.tupled((
                  <<[Int],
                  <<[java.sql.Timestamp],
                  <<[java.sql.Timestamp],
                  <<[Int],
                  <<[String]
              ))
  }

  /** Table description of table OriginalLevel1. Objects of this class serve as prototypes for rows in queries. */
  class OriginalLevel1(_tableTag: Tag) extends profile.api.Table[OriginalLevel1Row](_tableTag, "original_level1") {
    def * = (
          id,
          createTime,
          updateTime,
          originalGameId,
          name
        ) <> (OriginalLevel1Row.tupled, OriginalLevel1Row.unapply)

          val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)

          val createTime: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("create_time")

          val updateTime: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("update_time")

          val originalGameId: Rep[Int] = column[Int]("original_game_id")

          val name: Rep[String] = column[String]("name", O.Length(32, varying = true))

      }

  lazy val OriginalLevel1 = new TableQuery(tag => new OriginalLevel1(tag))
}
