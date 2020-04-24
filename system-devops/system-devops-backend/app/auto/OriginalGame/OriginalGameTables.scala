package auto.OriginalGame


object OriginalGameTables extends {
  val profile = slick.jdbc.MySQLProfile

  import profile.api._
  import slick.model.ForeignKeyAction
  import slick.jdbc.{GetResult => GR}

  lazy val schema: profile.SchemaDescription = OriginalGame.schema

  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema


  final case class OriginalGameRow(
      id: Int,
      createTime: java.sql.Timestamp,
      updateTime: java.sql.Timestamp,
      name: String
                                     )

  /** GetResult implicit for fetching OriginalGameRow objects using plain SQL queries */
  implicit def GetResultOriginalGameRow(implicit e0: GR[Int],e1: GR[Option[Int]], e2: GR[java.sql.Timestamp],e3: GR[Option[java.sql.Timestamp]], e4: GR[String], e5: GR[Option[String]]): GR[OriginalGameRow] = GR {
    prs =>
      import prs._
      OriginalGameRow.tupled((
                  <<[Int],
                  <<[java.sql.Timestamp],
                  <<[java.sql.Timestamp],
                  <<[String]
              ))
  }

  /** Table description of table OriginalGame. Objects of this class serve as prototypes for rows in queries. */
  class OriginalGame(_tableTag: Tag) extends profile.api.Table[OriginalGameRow](_tableTag, "original_game") {
    def * = (
          id,
          createTime,
          updateTime,
          name
        ) <> (OriginalGameRow.tupled, OriginalGameRow.unapply)

          val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)

          val createTime: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("create_time")

          val updateTime: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("update_time")

          val name: Rep[String] = column[String]("name", O.Length(32, varying = true))

      }

  lazy val OriginalGame = new TableQuery(tag => new OriginalGame(tag))
}
