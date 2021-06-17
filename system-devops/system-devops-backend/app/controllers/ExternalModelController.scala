package controllers

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, InputStream}
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64

import javax.imageio.ImageIO
import javax.inject.{Inject, Singleton}
import auto.Dict.DictRead
import models.OriginalGameSqler
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import services.DictServices
import third.{EsClient, EsFilter, GlbBuilder, OssClient}
import utils.Zip

/**
  * Created by wulinhao on 2020/03/15.
  */

case class Search3dModelRead(gameFilter: Map[String, Array[JsValue]], gameLimit: Int, modelFilter: Map[String, Array[JsValue]], offset: Int, limit: Int)

//
//case class External3dModelEsRecord(name: String, pn: String, unpackts: Int, version: String, create_time: String,
//                                   update_time: String, package_size: Int,
//                                   tag: Map[String, Double],
//                                   keyword: Map[String, Double],
//                                   oss_url: String)

case class External3dModel(id: String, record: JsObject, zip: String, thumbnail: String, rollThumbnail: String, glbKey: String)

case class ReSearch3dModelData(total: Int, list: Array[External3dModel])

case class ReSearch3dModel(isSuccess: Boolean = true, data: ReSearch3dModelData)

case class ReGetOne3dModel(isSuccess: Boolean = true, data: External3dModel)

case class External3dModelTagRead(tag: Map[String, Double], keyword: Map[String, Double])

case class GetGlbRead(zip: String, glbKey: String)

case class ReGetGlb(isSuccess: Boolean = true, data: String)


case class BuildImgRead(pn: String, version: String, unpackts: String, name: String,
                        width: Int, height: Int, frontImg: String, imgs: Array[String])

@Api("externalModel")
@Singleton
class ExternalModelController @Inject()(esClient: EsClient, ossClient: OssClient, glbBuilder: GlbBuilder,
                                        dictServices: DictServices,
                                        originalGameSpler: OriginalGameSqler,
                                        ws: WSClient, environment: Environment, apiAction: ApiAction,
                                        config: Configuration, cc: ControllerComponents)(implicit executionContext: ExecutionContext) extends AbstractController(cc) with JsonFormat {
  val logger = LoggerFactory.getLogger("application")
  //  search3dModel()
  //  GET / v1 / 3d Model / get /: id controllers.ExternalModelController.get3dModel(id: String)
  //  POST / v1 / 3d Model / tag /: id controllers.ExternalModelController.tag(id: String)


  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.Search3dModelRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "search 3dmodel", response = classOf[ReSearch3dModel])
  def search3dModel() = apiAction(parse.json[Search3dModelRead]) { request =>
    val read = request.body
//    logger.info(read.toString)
    val gameFilter = read.gameFilter.filter(_._2.nonEmpty)
    val modelFilter = read.modelFilter.filter(_._2.nonEmpty)
    val q = if (gameFilter.nonEmpty) {
      val q = gameFilter ++ Map("field_exists" -> Array(Json.toJson("version_item_number")))
      val v = esClient.searchExternalGame(EsFilter(q, 0, read.gameLimit))
      val pns = v.records.map(item => {
        val pn = (item._2 \ "pn").as[String]
        Json.toJson(pn)
      })
      if (pns.nonEmpty) {
        val f = modelFilter ++ Map("pn" -> pns)
        Option(f)
      }
      else None
    }
    else Option(modelFilter)

    if (q.isDefined) {
      val result = esClient.searchExternalModel(EsFilter(q.get, read.offset, read.limit))
      val datas = result.records.map(item => {
        val paths = ossClient.getModelPath(item._2)
        External3dModel(item._1, item._2, paths._1, paths._2, paths._3, paths._4)
      })
      Ok(Json.toJson(ReSearch3dModel(data = ReSearch3dModelData(result.total, datas))))
    }
    else {
      val datas = Array[External3dModel]()
      Ok(Json.toJson(ReSearch3dModel(data = ReSearch3dModelData(0, datas))))
    }
  }

  @ApiOperation(value = "get one 3dmodel preview", response = classOf[ReGetOne3dModel])
  def get3dModel(id: String) = apiAction { request =>
    val joOpiton = esClient.getExternalModel(id)
    if (joOpiton.isDefined) {
      val jo = joOpiton.get
//      logger.info(jo.toString())
      val paths = ossClient.getModelPath(jo)
      val m = External3dModel(id, jo, paths._1, paths._2, paths._3, paths._4)
      Ok(Json.toJson(ReGetOne3dModel(data = m)))
    }
    else {
      Ok(Json.toJson(ReMsg(msg = "object not found")))
    }
  }

  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.External3dModelTagRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "tag one ExternalGame", response = classOf[ReId])
  def tag(id: String) = apiAction(parse.json[External3dModelTagRead]) { request =>
    val read = request.body
    val re = esClient.tagExternalModel(id, read.tag, read.keyword)

    val tagDict = read.tag.map(item => {
      DictRead(DictName.model_tag.toString, item._1, "")
    }).toArray
    dictServices.add(tagDict)
    val keywordDict = read.keyword.map(item => {
      DictRead(DictName.model_keyword.toString, item._1, "")
    }).toArray
    dictServices.add(keywordDict)

    Ok(Json.toJson(ReId(data = re)))
  }

  //  @ApiImplicitParams(Array(
  //    new ApiImplicitParam(
  //      dataType = "controllers.GetGlbRead", value = "body", required = true, paramType = "body"
  //    )
  //  ))
  //  @ApiOperation(value = "build glb and return the download url", response = classOf[ReGetGlb])
  //  def getGlb() = apiAction(parse.json[GetGlbRead]) { request =>
  //    val read = request.body
  //    val zip = read.zip
  //    val glbKey = read.glbKey
  //    if (ossClient.isExists(glbKey)) {
  //      val url = ossClient.signPath(glbKey)
  //      Ok(Json.toJson(ReGetGlb(data = url)))
  //    }
  //    else {
  //      val glbUrl = glbBuilder.downloadAndChange(zip, glbKey)
  //      if (glbUrl.isDefined) {
  //        Ok(Json.toJson(ReGetGlb(data = glbUrl.get)))
  //      }
  //      else {
  //        Ok(Json.toJson(ReMsg(msg = "change glb fail")))
  //      }
  //    }
  //
  //  }


  /**
    * img format: data:image/png;base64,xxx
    *
    * @return
    */
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      dataType = "controllers.BuildImgRead", value = "body", required = true, paramType = "body"
    )
  ))
  @ApiOperation(value = "recive the snapchat img and send to glb", response = classOf[ReMsg])
  def buildImg() = apiAction(parse.json[BuildImgRead]) { request =>
    val read = request.body

    val thumbnailBytes = getPngFrombase64(read.frontImg)
    val keys = ossClient.getModelKey(read.pn, read.version, read.unpackts, read.name)
    ossClient.uploadByts(keys._2, thumbnailBytes)

    val rollThumbnailBytes = combinePng(read.width, read.height, read.imgs)
    ossClient.uploadByts(keys._3, rollThumbnailBytes)

    Ok(Json.toJson(ReMsg(true, s"upload success with  ${keys._2} : ${thumbnailBytes.length}  and ${keys._3} : ${rollThumbnailBytes.length}")))
  }

  def getPngFrombase64(s: String): Array[Byte] = {
    val temp = s.split(",").last
    Base64.getDecoder.decode(temp)

  }

  def combinePng(width: Int, height: Int, imgs: Array[String]): Array[Byte] = {
    val result = new BufferedImage(width * imgs.length, height, BufferedImage.TYPE_3BYTE_BGR)
    val g = result.getGraphics

    imgs.indices.foreach(index => {
      val item = imgs(index)
      val content = getPngFrombase64(item)
      val is = new ByteArrayInputStream(content)
      val bi = ImageIO.read(is)
      val x = width * index
      g.drawImage(bi, x, 0, width, height, null)
    })
    val baos = new ByteArrayOutputStream()
    ImageIO.write(result, "jpeg", baos)
    baos.flush()
    val b = baos.toByteArray
    g.dispose()
    b
  }

}

