package controllers

import javax.inject.{Inject, Singleton}

import auto.OriginalLevel2._
import io.swagger.annotations.{ApiOperation, _}
import models.OriginalLevel2Sqler
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.ControllerComponents
import play.api.{Configuration, Environment}
import third.{EsClient, EsFilter}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

@Api("originalLevel2")
@Singleton
class OriginalLevel2Controller @Inject()(originalLevel2Spler: OriginalLevel2Sqler,
                                         esClient: EsClient,
                                         ws: WSClient, environment: Environment, apiAction: ApiAction,
                                         config: Configuration, cc: ControllerComponents)(implicit executionContext: ExecutionContext)
  extends InnerOriginalLevel2Controller(originalLevel2Spler, ws, environment, apiAction, config, cc)(executionContext)with JsonFormat {


  @ApiOperation(value = "delete OriginalLevel2", response = classOf[ReId])
  override def deleteOriginalLevel2(id: Int) = apiAction {
    request =>
      val m = Map("level2" -> Array(Json.toJson(id)))
      val count = esClient.countOriginalModel(EsFilter(m, 0, 0))
      if (count == 0) {
        val x = originalLevel2Spler.deleteOriginalLevel2(id)
        val item = Await.result(x, 5 seconds)
        Ok(Json.toJson(ReId(data = item)))
      }
      else {
        Ok(Json.toJson(ReMsg(msg = "level2 is not empty")))
      }
  }

}
