package controllers

import javax.inject.{Inject, Singleton}

import auto.OriginalGame._
import auto.OriginalLevel1.OriginalLevel1ListRead
import io.swagger.annotations._
import models.{OriginalGameSqler, OriginalLevel1Sqler}
import play.api.libs.json.Json
import play.api.libs.ws._
import play.api.mvc.ControllerComponents
import play.api.{Configuration, Environment}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


@Api("originalGame")
@Singleton
class OriginalGameController @Inject()(originalGameSpler: OriginalGameSqler,
                                       originalLevel1Sqler: OriginalLevel1Sqler,
                                       ws: WSClient, environment: Environment, apiAction: ApiAction,
                                       config: Configuration, cc: ControllerComponents)(implicit executionContext: ExecutionContext)
  extends InnerOriginalGameController(originalGameSpler, ws, environment, apiAction, config, cc)(executionContext) with JsonFormat {


  @ApiOperation(value = "delete OriginalGame", response = classOf[ReId])
  override def deleteOriginalGame(id: Int) = apiAction {
    request =>
      val re = originalLevel1Sqler.findList(OriginalLevel1ListRead(Some(s" original_game_id = ${id}"), None, 0, 1))
      if (re.isEmpty) {
        val x = originalGameSpler.deleteOriginalGame(id)
        val item = Await.result(x, 5 seconds)
        Ok(Json.toJson(ReId(data = item)))
      }
      else {
        Ok(Json.toJson(ReMsg(msg = "level1 is not empty")))
      }
  }
}
