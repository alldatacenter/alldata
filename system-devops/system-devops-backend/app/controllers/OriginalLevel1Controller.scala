package controllers

import javax.inject.{Inject, Singleton}

import auto.OriginalLevel1._
import auto.OriginalLevel2.OriginalLevel2ListRead
import io.swagger.annotations._
import models.{OriginalLevel1Sqler, OriginalLevel2Sqler}
import play.api.libs.json.Json
import play.api.libs.ws._
import play.api.mvc.ControllerComponents
import play.api.{Configuration, Environment}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}


@Api("originalLevel1")
@Singleton
class OriginalLevel1Controller @Inject()(originalLevel1Spler: OriginalLevel1Sqler,
                                         originalLevel2Sqler: OriginalLevel2Sqler,
                                         ws: WSClient, environment: Environment, apiAction: ApiAction,
                                         config: Configuration, cc: ControllerComponents)(implicit executionContext: ExecutionContext)
  extends InnerOriginalLevel1Controller(originalLevel1Spler, ws, environment, apiAction, config, cc)(executionContext) with JsonFormat{


  @ApiOperation(value = "delete OriginalLevel1", response = classOf[ReId])
  override def deleteOriginalLevel1(id: Int) = apiAction {
    request =>
      val re = originalLevel2Sqler.findList(OriginalLevel2ListRead(Some(s" original_level1_id = ${id}"), None, 0, 1))
      if (re.isEmpty) {
        val x = originalLevel1Spler.deleteOriginalLevel1(id)
        val item = Await.result(x, 5 seconds)
        Ok(Json.toJson(ReId(data = item)))
      }
      else {
        Ok(Json.toJson(ReMsg(msg = "level1 is not empty")))
      }
  }
}
