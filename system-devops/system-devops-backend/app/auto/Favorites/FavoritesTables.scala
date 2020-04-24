package auto.Favorites

object FavoritesTables extends {
  val profile = slick.jdbc.MySQLProfile

  import profile.api._
  import slick.model.ForeignKeyAction
  import slick.jdbc.{GetResult => GR}

  lazy val schema: profile.SchemaDescription = Favorites.schema

  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema


  final case class FavoritesRow(
      id: Int,
      createTime: java.sql.Timestamp,
      updateTime: java.sql.Timestamp,
      favorite: String,
      description: String,
      username: String
                                     )

  /** GetResult implicit for fetching FavoritesRow objects using plain SQL queries */
  implicit def GetResultFavoritesRow(implicit e0: GR[Int],e1: GR[Option[Int]], e2: GR[java.sql.Timestamp],e3: GR[Option[java.sql.Timestamp]], e4: GR[String], e5: GR[Option[String]]): GR[FavoritesRow] = GR {
    prs =>
      import prs._
      FavoritesRow.tupled((
                  <<[Int],
                  <<[java.sql.Timestamp],
                  <<[java.sql.Timestamp],
                  <<[String],
                  <<[String],
                  <<[String]
              ))
  }

  /** Table description of table Favorites. Objects of this class serve as prototypes for rows in queries. */
  class Favorites(_tableTag: Tag) extends profile.api.Table[FavoritesRow](_tableTag, "favorites") {
    def * = (
          id,
          createTime,
          updateTime,
          favorite,
          description,
          username
        ) <> (FavoritesRow.tupled, FavoritesRow.unapply)

          val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)

          val createTime: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("create_time")

          val updateTime: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("update_time")

          val favorite: Rep[String] = column[String]("favorite", O.Length(255, varying = true))

          val description: Rep[String] = column[String]("description", O.Length(1024, varying = true))

          val username: Rep[String] = column[String]("username", O.Length(32, varying = true))

      }

  lazy val Favorites = new TableQuery(tag => new Favorites(tag))
}
