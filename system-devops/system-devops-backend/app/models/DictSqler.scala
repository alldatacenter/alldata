package models

import javax.inject.Inject

import auto.Dict.DictTables.Dict
import auto.Dict.{DictData, InnerDictSqler}
import play.api.db.slick.DatabaseConfigProvider

import java.sql.Timestamp

import auto.Dict._
import auto.Dict.DictTables._
import controllers._
import org.slf4j.{Logger, LoggerFactory}
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import scala.concurrent.{Await, ExecutionContext}

class DictSqler @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends InnerDictSqler {

  def findSuggestSync(dictName: String, input: String): Future[Seq[DictRow]] = {
    val action = Dict.filter(_.dictName === dictName).filter(_.dictValue.like(s"%${input}%")).result
    db.run(action)
  }

  def all(): Seq[DictData] = {
    val action = Dict.result
    val list = Await.result(db.run(action), 5 seconds)
    list
  }


  def addWithoutException(read: DictRead): Unit = {
    try {
      val ts = new Timestamp(System.currentTimeMillis())
      val row = DictRow(
        0, ts, ts,
        read.dictName,
        read.dictValue,
        read.dictDesc
      )
      val action = (Dict returning Dict.map(_.id)).+=(row)
      Await.result(db.run(action), 5 seconds)
    }
    catch {
      case e: Exception => {
        //ignore
      }
    }
  }
}