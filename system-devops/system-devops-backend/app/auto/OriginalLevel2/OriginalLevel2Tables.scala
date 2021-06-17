package auto.OriginalLevel2


object OriginalLevel2Tables extends {
  val profile = slick.jdbc.MySQLProfile

  import profile.api._
  import slick.model.ForeignKeyAction
  import slick.jdbc.{GetResult => GR}

  lazy val schema: profile.SchemaDescription = OriginalLevel2.schema

  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema


  final case class OriginalLevel2Row(
      id: Int,
      createTime: java.sql.Timestamp,
      updateTime: java.sql.Timestamp,
      originalLevel1Id: Int,
      name: String
                                     )

  /** GetResult implicit for fetching OriginalLevel2Row objects using plain SQL queries */
  implicit def GetResultOriginalLevel2Row(implicit e0: GR[Int],e1: GR[Option[Int]], e2: GR[java.sql.Timestamp],e3: GR[Option[java.sql.Timestamp]], e4: GR[String], e5: GR[Option[String]]): GR[OriginalLevel2Row] = GR {
    prs =>
      import prs._
      OriginalLevel2Row.tupled((
                  <<[Int],
                  <<[java.sql.Timestamp],
                  <<[java.sql.Timestamp],
                  <<[Int],
                  <<[String]
              ))
  }

  /** Table description of table OriginalLevel2. Objects of this class serve as prototypes for rows in queries. */
  class OriginalLevel2(_tableTag: Tag) extends profile.api.Table[OriginalLevel2Row](_tableTag, "original_level2") {
    def * = (
          id,
          createTime,
          updateTime,
          originalLevel1Id,
          name
        ) <> (OriginalLevel2Row.tupled, OriginalLevel2Row.unapply)

          val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)

          val createTime: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("create_time")

          val updateTime: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("update_time")

          val originalLevel1Id: Rep[Int] = column[Int]("original_level1_id")

          val name: Rep[String] = column[String]("name", O.Length(32, varying = true))

      }

  lazy val OriginalLevel2 = new TableQuery(tag => new OriginalLevel2(tag))
}
