package auto.Material

object MaterialTables extends {
  val profile = slick.jdbc.MySQLProfile

  import profile.api._
  import slick.jdbc.{GetResult => GR}

  lazy val schema: profile.SchemaDescription = Material.schema

  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema


  final case class MaterialRow(
      id: Int,
      createTime: java.sql.Timestamp,
      updateTime: java.sql.Timestamp,
      itemId: String,
      favoriteId: Int
                                     )

  /** GetResult implicit for fetching MaterialRow objects using plain SQL queries */
  implicit def GetResultMaterialRow(implicit e0: GR[Int],e1: GR[Option[Int]], e2: GR[java.sql.Timestamp],e3: GR[Option[java.sql.Timestamp]], e4: GR[String], e5: GR[Option[String]]): GR[MaterialRow] = GR {
    prs =>
      import prs._
      MaterialRow.tupled((
                  <<[Int],
                  <<[java.sql.Timestamp],
                  <<[java.sql.Timestamp],
                  <<[String],
                  <<[Int]
              ))
  }

  /** Table description of table Material. Objects of this class serve as prototypes for rows in queries. */
  class Material(_tableTag: Tag) extends profile.api.Table[MaterialRow](_tableTag, "material") {
    def * = (
          id,
          createTime,
          updateTime,
          itemId,
          favoriteId
        ) <> (MaterialRow.tupled, MaterialRow.unapply)

          val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)

          val createTime: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("create_time")

          val updateTime: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("update_time")

          val itemId: Rep[String] = column[String]("item_id", O.Length(32, varying = true))

          val favoriteId: Rep[Int] = column[Int]("favorite_id")

      }

  lazy val Material = new TableQuery(tag => new Material(tag))
}
