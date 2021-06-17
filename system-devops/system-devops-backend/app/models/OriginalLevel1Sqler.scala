package models

import javax.inject.Inject

import auto.OriginalLevel1.InnerOriginalLevel1Sqler
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext

class OriginalLevel1Sqler @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends InnerOriginalLevel1Sqler {

}