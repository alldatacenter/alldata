package controllers

import javax.inject.{Inject, Singleton}

import io.swagger.annotations._
import models.DictSqler
import play.api.libs.ws._
import play.api.mvc.ControllerComponents
import play.api.{Configuration, Environment}
import auto.Dict._
import io.swagger.annotations._
import play.api.libs.json.Json
import services.DictServices
import third.EsFilter

import scala.concurrent.ExecutionContext


case class ReSuggest(isSuccess: Boolean = true, data: Array[String])

object DictName extends Enumeration {
  val game_category, game_tag, game_keyword, model_tag, model_keyword = Value
}

@Api("dict")
@Singleton
class DictController @Inject()(dictSpler: DictSqler, dictServices: DictServices,
                               ws: WSClient, environment: Environment, apiAction: ApiAction,
                               config: Configuration, cc: ControllerComponents)(implicit executionContext: ExecutionContext)
  extends InnerDictController(dictSpler, ws, environment, apiAction, config, cc)(executionContext) with JsonFormat {

  // game_category -> Action Adventure Arcade Board Card Casino Casual Educational Music Puzzle Racing Role Playing Simulation Sports Strategy Trivia Word
  // game_tag ->
  // game_keyword
  // model_tag ->  材质-金属
  // model_keyword

  val m = Map(
    "Action" -> "动作",
    "Adventure" -> "冒险",
    "Arcade" -> "街机",
    "Board" -> "桌面和棋类",
    "Card" -> "卡牌",
    "Casino" -> "赌场",
    "Casual" -> "休闲",
    "Educational" -> "教育",
    "Music" -> "音乐",
    "Puzzle" -> "益智",
    "Racing" -> "竞速",
    "Role Playing" -> "角色扮演",
    "Simulation" -> "模拟",
    "Sports" -> "体育",
    "Strategy" -> "策略",
    "Trivia" -> "知识问答",
    "Word" -> "文字"
  )

  val modelTags = Array("CG-怪物", "CG-人物", "CG-武器", "CG-道具", "CG-动物", "CG-树木", "CG-地形", "CG-石头", "CG-飞机", "CG-摩托单车", "CG-船", "CG-铁路", "CG-坦克", "CG-潜艇", "CG-航母", "CG-科幻模型", "CG-厨房餐厅", "CG-家具模型", "CG-室外建筑", "CG-太空模型", "CG-医疗道具", "CG-水果蔬菜", "CG-食品饮品", "CG-服饰鞋帽", "CG-数码电器", "CG-文字", "CG-图标", "材质-金属", "材质-玻璃", "材质-塑料", "材质-SSS", "材质-木材", "材质-石头", "材质-液体", "材质-气体")

  @ApiOperation(value = "init Dict", response = classOf[String])
  def init() = apiAction { request =>
    dictServices.add(m.map(one => {
      DictRead(DictName.game_category.toString, one._2, "")
    }).toArray)
    dictServices.add(Array("末日").map(one => {
      DictRead(DictName.game_tag.toString, one, "")
    }))
    dictServices.add(Array("休闲").map(one => {
      DictRead(DictName.game_keyword.toString, one, "")
    }))
    dictServices.add(modelTags.map(one => {
      DictRead(DictName.model_tag.toString, one, "")
    }))
    dictServices.add(Array("休闲").map(one => {
      DictRead(DictName.model_keyword.toString, one, "")
    }))
    Ok("ok")
  }

  @ApiOperation(value = "get suggest word from input", response = classOf[ReSuggest])
  def suggest(dictName: String, input: String) = apiAction.async { request =>

    dictSpler.findSuggestSync(dictName, input).map(item => {
      val re = item.map(_.dictValue).take(10).toArray
      Ok(Json.toJson(ReSuggest(data = re)))
    })

  }
}
