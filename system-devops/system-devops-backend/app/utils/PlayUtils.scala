package utils

import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

/**
  * Created by wulinhao on 2020/04/12.
  */
object PlayUtils {
  def waitFirst[T](f: Future[Seq[T]], duration: Duration): Option[T] = {
    val x = Await.result(f, duration)
    if (x.nonEmpty) Some(x.head)
    else None
  }

  def waitList[T](f: Future[Seq[T]], duration: Duration): Seq[T] = {
    Await.result(f, duration)
  }

  def parseFullTs(x: String): String = {
    val from = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    val to = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val d = from.parse(x.substring(0, 19))
    to.format(d)
  }

  def getNow(): String = {
    val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    df.format(new Date())
  }

}