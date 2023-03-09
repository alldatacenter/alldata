package com.linkedin.feathr.offline.config.location

import com.fasterxml.jackson.annotation.{JsonAlias, JsonIgnoreProperties}
import com.fasterxml.jackson.module.caseclass.annotation.CaseClassDeserialize
import com.linkedin.feathr.common.Header
import com.linkedin.feathr.offline.source.dataloader.jdbc.JdbcUtils
import com.linkedin.feathr.offline.source.dataloader.jdbc.JdbcUtils.DBTABLE_CONF
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.eclipse.jetty.util.StringUtil

@CaseClassDeserialize()
@JsonIgnoreProperties(ignoreUnknown = true)
case class Jdbc(url: String, @JsonAlias(Array("query")) dbtable: String, user: String = "", password: String = "", token: String = "") extends DataLocation {
  override def loadDf(ss: SparkSession, dataIOParameters: Map[String, String] = Map()): DataFrame = {
    println(s"Jdbc.loadDf, location is ${this}")
    var reader = ss.read.format("jdbc")
      .option("url", url)
    if (StringUtil.isBlank(dbtable)) {
      // Fallback to default table name
      reader = reader.option("dbtable", ss.conf.get(DBTABLE_CONF))
    } else {
      val q = dbtable.trim
      if ("\\s".r.findFirstIn(q).nonEmpty) {
        // This is a SQL instead of a table name
        reader = reader.option("query", q)
      } else {
        reader = reader.option("dbtable", q)
      }
    }
    if (!StringUtil.isBlank(token)) {
      reader.option("accessToken", LocationUtils.envSubstitute(token))
        .option("hostNameInCertificate", "*.database.windows.net")
        .option("encrypt", true)
        .load
    } else {
      if (StringUtil.isBlank(user) && StringUtil.isBlank(password)) {
        reader.load()
      } else {
        reader.option("user", LocationUtils.envSubstitute(user))
          .option("password", LocationUtils.envSubstitute(password))
      }.load
    }
  }

  override def writeDf(ss: SparkSession, df: DataFrame, header: Option[Header]): Unit = {
    println(s"Jdbc.writeDf, location is ${this}")
    df.write.format("jdbc")
      .options(getOptions(ss))
      .save()
  }

  override def getPath: String = url

  override def getPathList: List[String] = List(url)

  override def isFileBasedLocation(): Boolean = false

  // These members don't contain actual secrets
  override def toString: String = s"Jdbc(url=$url, dbtable=$dbtable, user=$user, password=$password, token=$token)"

  def getOptions(ss: SparkSession): Map[String, String] = {
    val options = collection.mutable.Map[String, String]()
    options += ("url" -> url)

    if (StringUtil.isBlank(dbtable)) {
      // Fallback to default table name
      options += ("dbtable" -> ss.conf.get(DBTABLE_CONF))
    } else {
      val q = dbtable.trim
      if ("\\s".r.findFirstIn(q).nonEmpty) {
        // This is a SQL instead of a table name
        options += ("query" -> q)
      } else {
        options += ("dbtable" -> q)
      }
    }
    if (!StringUtil.isBlank(token)) {
      options += ("accessToken" -> LocationUtils.envSubstitute(token))
      options += ("hostNameInCertificate" -> "*.database.windows.net")
      options += ("encrypt" -> "true")
    } else {
      if (!StringUtil.isBlank(user)) {
        options += ("user" -> LocationUtils.envSubstitute(user))
      }
      if (!StringUtil.isBlank(password)) {
        options += ("password" -> LocationUtils.envSubstitute(password))
      }
    }
    options.toMap
  }
}

  object Jdbc {
    /**
     * Create JDBC InputLocation with required info and user/password auth
     *
     * @param url
     * @param dbtable
     * @param user
     * @param password
     * @return Newly created InputLocation instance
     */
    def apply(url: String, dbtable: String, user: String, password: String): Jdbc = Jdbc(url, dbtable, user = user, password = password)

    /**
     * Create JDBC InputLocation with required info and OAuth token auth
     *
     * @param url
     * @param dbtable
     * @param token
     * @return Newly created InputLocation instance
     */
    def apply(url: String, dbtable: String, token: String): Jdbc = Jdbc(url, dbtable, token = token)

    /**
     * Create JDBC InputLocation with required info and OAuth token auth
     * In this case, the auth info is taken from default setting passed from CLI/API, details can be found in `Jdbc#loadDf`
     *
     * @see com.linkedin.feathr.offline.source.dataloader.jdbc.JDBCUtils#loadDataFrame
     * @param url
     * @param dbtable
     * @return Newly created InputLocation instance
     */
    def apply(url: String, dbtable: String): Jdbc = Jdbc(url, dbtable)
  }
