package models

import javax.inject.Inject

import auto.OriginalGame.InnerOriginalGameSqler
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext

class OriginalGameSqler @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends InnerOriginalGameSqler {

}