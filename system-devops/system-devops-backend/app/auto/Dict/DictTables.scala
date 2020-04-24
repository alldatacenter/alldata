package auto.Dict


object DictTables extends {
  val profile = slick.jdbc.MySQLProfile

  import profile.api._
  import slick.model.ForeignKeyAction
  import slick.jdbc.{GetResult => GR}

  lazy val schema: profile.SchemaDescription = Dict.schema

  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema


  final case class DictRow(
      id: Int,
      createTime: java.sql.Timestamp,
      updateTime: java.sql.Timestamp,
      dictName: String,
      dictValue: String,
      dictDesc: String
                                     )

  /** GetResult implicit for fetching DictRow objects using plain SQL queries */
  implicit def GetResultDictRow(implicit e0: GR[Int],e1: GR[Option[Int]], e2: GR[java.sql.Timestamp],e3: GR[Option[java.sql.Timestamp]], e4: GR[String], e5: GR[Option[String]]): GR[DictRow] = GR {
    prs =>
      import prs._
      DictRow.tupled((
                  <<[Int],
                  <<[java.sql.Timestamp],
                  <<[java.sql.Timestamp],
                  <<[String],
                  <<[String],
                  <<[String]
              ))
  }

  /** Table description of table Dict. Objects of this class serve as prototypes for rows in queries. */
  class Dict(_tableTag: Tag) extends profile.api.Table[DictRow](_tableTag, "dict") {
    def * = (
          id,
          createTime,
          updateTime,
          dictName,
          dictValue,
          dictDesc
        ) <> (DictRow.tupled, DictRow.unapply)

          val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)

          val createTime: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("create_time")

          val updateTime: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("update_time")

          val dictName: Rep[String] = column[String]("dict_name", O.Length(32, varying = true))

          val dictValue: Rep[String] = column[String]("dict_value", O.Length(64, varying = true))

          val dictDesc: Rep[String] = column[String]("dict_desc", O.Length(64, varying = true))

      }

  lazy val Dict = new TableQuery(tag => new Dict(tag))
}
