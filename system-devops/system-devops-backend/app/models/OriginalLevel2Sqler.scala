package models

import javax.inject.Inject

import auto.OriginalLevel2.InnerOriginalLevel2Sqler
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext

class OriginalLevel2Sqler @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends InnerOriginalLevel2Sqler {

}