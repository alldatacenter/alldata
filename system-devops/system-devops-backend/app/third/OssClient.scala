package third

import java.io.{ByteArrayInputStream, File, InputStream}
import java.net.URL
import java.util.Date
import javax.inject.{Inject, Singleton}

import com.aliyun.oss.{HttpMethod, OSSClientBuilder}
import com.aliyun.oss.model.{GeneratePresignedUrlRequest, GetObjectRequest}
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient

import scala.collection.JavaConverters._

/**
  * Created by wulinhao on 2020/03/15.
  */
@Singleton
class OssClient @Inject()(ws: WSClient, config: Configuration) {
  val logger = LoggerFactory.getLogger("application")
  val endpoint = config.get[String]("oss.endpoint")
  val bucket = config.get[String]("oss.bucket")
  val accessId = config.get[String]("oss.id")
  val accessKey = config.get[String]("oss.key")
  val pngOption = config.get[Map[String, String]]("oss.pngoption")
  val client = new OSSClientBuilder().build(endpoint, accessId, accessKey)

  //  val pngOption = Map("x-oss-process" -> "image/format,jpeg/quality,q_50")

  /*
icon地址: $bucket/$pn/info/icon.png
某个游戏的缩略图目录: $bucket/$pn/preview/$version/1.png 2.png ...

某个游戏的某个版本的总目录: $bucket/$pn/package/$version/
某个游戏的某个版本的包: $bucket/$pn/package/$version/xxx.apk
素材zip包地址: $bucket/$pn/package/$version/resource/$unpackts/3dmodel/${name}/data.zip
该素材zip包的图片: $bucket/$pn/package/$version/resource/$unpackts/3dmodel/${name}/thumbnail.jpg
该素材zip包的滚动预览图片: $bucket/$pn/package/$version/resource/$unpackts/3dmodel/${name}/rollThumbnail.jpg

对应的变量 pn包名 version版本名称 unpackts解压时间戳 name模型名字

   */

  def urlToKey(url: String): String = {
    new URL(url).getPath.substring(1)
  }

  def signPath(path: String): String = {
    signPath(path, Map[String, String]())
  }

  def signPath(path: String, params: Map[String, String]): String = {
    val expiration = new Date(new Date().getTime() + 3600 * 1000); // 生成URL
    val request = new GeneratePresignedUrlRequest(bucket, path)
    request.setExpiration(expiration)
    request.setMethod(HttpMethod.GET)
    params.foreach(item => {
      request.addQueryParameter(item._1, item._2)
    })
    val url = client.generatePresignedUrl(request)
    url.toString
  }

  def getIcon(pn: String): String = {
    val path = s"${pn}/info/icon.png"
    signPath(path, pngOption)
  }

  def getAllPreviewList(pn: String): Map[String, Array[String]] = {
    val prefix = s"${pn}/preview/"
    val response = client.listObjects(bucket, prefix)
    val versionPreview = response.getObjectSummaries.asScala.flatMap(item => {
      val key = item.getKey
      val temp = key.split("/")
      if (temp.length == 4) {
        val version = temp(2)
        val url = signPath(key, pngOption)
        Option((version, url))
      }
      else {
        None
      }
    }).toArray.groupBy(_._1).mapValues(item => {
      item.map(_._2)
    })
    versionPreview
  }

  def getModelPath(one: JsObject): (String, String, String, String) = {
    logger.info(one.toString())
    val pn = (one \ "pn").as[String]
    val version = (one \ "version").as[String]
    val unpackts = (one \ "unpackts").as[String]
    val name = (one \ "name").as[String]
    getModelPath(pn, version, unpackts, name)
  }

  def getModelKey(pn: String, version: String, unpackts: String, name: String): (String, String, String, String) = {
    val path = s"${pn}/package/${version}/resource/${unpackts}/3dmodel/${name}"
    (s"${path}/data.zip", s"${path}/thumbnail.jpeg",
      s"${path}/rollThumbnail.jpeg",
      s"${path}/data.glb")
  }

  def getModelPath(pn: String, version: String, unpackts: String, name: String): (String, String, String, String) = {
    val path = s"${pn}/package/${version}/resource/${unpackts}/3dmodel/${name}"
    (signPath(s"${path}/data.zip"), signPath(s"${path}/thumbnail.jpeg"),
      signPath(s"${path}/rollThumbnail.jpeg"),
      signPath(s"${path}/data.glb"))
  }

  def isExists(key: String): Boolean = {
    client.doesObjectExist(bucket, key)
  }

  def downFile(path: String, oFile: String): Unit = {
    logger.info(s"download from $path $oFile")
    client.getObject(new GetObjectRequest(bucket, path), new File(oFile))
  }

  def uploadFile(path: String, iFile: String): Unit = {
    logger.info(s"upload to $path ${iFile}")
    client.putObject(bucket, path, new File(iFile))
  }

  def uploadByts(path: String, content: Array[Byte]): Unit = {
    try {
      logger.info(s"upload to $path : ${content.length}")
      val is = new ByteArrayInputStream(content)
      client.putObject(bucket, path, is)
    }
    catch {
      case e: Exception => {
        logger.error("upload with exception", e)
      }
    }
  }

}
