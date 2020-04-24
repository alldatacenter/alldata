package controllers

import java.sql.Timestamp
import java.text.SimpleDateFormat

import auto.Favorites.FavoritesCreate
import auto.Material.MaterialCreate
import auto.VideoFinalTask._
import org.joda.time.DateTime
import play.api.libs.json._
import third.EsFilter


/**
  *
  */
case class ReId(isSuccess: Boolean = true, data: Int = -1)


case class ReNeedData(need: Boolean = false, url: String = "")

case class ReNeed(isSuccess: Boolean = true, data: ReNeedData)

case class ReIdArray(isSuccess: Boolean = true, data: Array[Int])

case class ReMsg(isSuccess: Boolean = false, msg: String)

trait BeforeInnerJsonFormat {
  def timestampToDateTime(t: Timestamp): DateTime = new DateTime(t.getTime)

  def dateTimeToTimestamp(dt: DateTime): Timestamp = new Timestamp(dt.getMillis)

  implicit object timestampFormat extends Format[Timestamp] {
    val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    def reads(json: JsValue) = {
      val str = json.as[String]
      JsSuccess(new Timestamp(format.parse(str).getTime))
    }

    def writes(ts: Timestamp) = JsString(format.format(ts))
  }

  implicit val ReIdFormat = Json.format[ReId]
  implicit val ReIdArrayFormat = Json.format[ReIdArray]

}

trait JsonFormat extends BeforeInnerJsonFormat {
  implicit val ReMsgFormat = Json.format[ReMsg]
  implicit val EsFilterFormat = Json.format[EsFilter]

  implicit val ExternalGameFormat = Json.format[ExternalGame]
  implicit val ReSearchDataFormat = Json.format[ReSearchData]
  implicit val ReSearchGameFormat = Json.format[ReSearchGame]
  implicit val ExternalGameDetailFormat = Json.format[ExternalGameDetail]
  implicit val ReGetOneExternalGameFormat = Json.format[ReGetOneExternalGame]
  implicit val ExternalGameTagReadFormat = Json.format[ExternalGameTagRead]

  implicit val Search3dModelReadFormat = Json.format[Search3dModelRead]
  implicit val External3dModelFormat = Json.format[External3dModel]
  implicit val ReSearch3dModelDataFormat = Json.format[ReSearch3dModelData]
  implicit val ReSearch3dModelFormat = Json.format[ReSearch3dModel]
  implicit val ReGetOne3dModelFormat = Json.format[ReGetOne3dModel]
  implicit val External3dModelTagReadFormat = Json.format[External3dModelTagRead]
  implicit val BuildImgReadFormat = Json.format[BuildImgRead]

  implicit val GetGlbReadFormat = Json.format[GetGlbRead]
  implicit val ReGetGlbFormat = Json.format[ReGetGlb]
  implicit val ReSuggestFormat = Json.format[ReSuggest]
  implicit val ReSuggestGameFormat = Json.format[ReSuggestGame]

  implicit val AddOriginalModelRecordFormat = Json.format[AddOriginalModelRecord]
  implicit val UpdateOriginalModelRecordFormat = Json.format[UpdateOriginalModelRecord]

  implicit val OriginalModelDataFormat = Json.format[OriginalModelData]
  implicit val OriginalModelSearchFormat = Json.format[OriginalModelSearch]
  implicit val ReOriginalSearchFormat = Json.format[ReOriginalSearch]
  implicit val ReDeleteOriginModelFormat = Json.format[ReDeleteOriginModel]

  implicit val FavoriteDataFormat = Json.format[FavoriteData]
  implicit val FavoriteResultFormat = Json.format[FavoriteResult]
  implicit val ReSearchFavoritesFormat = Json.format[ReSearchFavorites]
  implicit val FavoritesUpdateReadFormat = Json.format[FavoritesUpdateRead]
  implicit val FavoritesCreateFormat = Json.format[FavoritesCreate]
  implicit val MaterialCreateFormat = Json.format[MaterialCreate]

  implicit val VideoContentTaskStatusFormat = Json.format[VideoContentTaskStatus]
  implicit val VideoFinalStatusFormat = Json.format[VideoFinalStatus]
  implicit val VideoContentUpdateFieldsFormat = Json.format[VideoContentUpdateFields]
  implicit val VideoFinalUpdateFieldsFormat = Json.format[VideoFinalUpdateFields]
  implicit val VideoEndingUpdateFieldsFormat = Json.format[VideoEndingUpdateFields]
  implicit val VideoStatusAndResultPathFormat = Json.format[VideoStatusAndResultPath]
  implicit val VideoFinalTaskExtendFormat = Json.format[VideoFinalTaskExtend]
  implicit val VideoEndingStatusFormat = Json.format[VideoEndingStatus]
  implicit val VideoEndingVideoValueFormat = Json.format[VideoEndingVideoValue]

  implicit val ReNeedDataFormat = Json.format[ReNeedData]
  implicit val ReNeedFormat = Json.format[ReNeed]
  implicit val VideoFinalUrlContentFormat = Json.format[VideoFinalUrlContent]
  implicit val VideoFinalAndEndingTimeFormat = Json.format[VideoFinalAndEndingTime]

  implicit val VideoEndingTaskAepPathFormat = Json.format[VideoEndingTaskAepPath]
  implicit val VideoContentTaskAepPathFormat = Json.format[VideoContentTaskAepPath]
  implicit val VideoEndingTasExtendTimeFormat = Json.format[VideoEndingTasExtend]

  implicit val VideoEndingTagFormat = Json.format[VideoEndingTag]
  implicit val VideoContentTagFormat = Json.format[VideoContentTag]

  implicit val VideoFinalTaskDataExtendFormat = Json.format[VideoFinalTaskDataExtend]
  implicit val ReVideoFinalTaskExtendFormat = Json.format[ReVideoFinalTaskExtend]


}