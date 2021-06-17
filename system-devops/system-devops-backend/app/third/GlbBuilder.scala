package third

import java.io.File
import java.nio.charset.StandardCharsets

import javax.inject.{Inject, Singleton}
import controllers.ReGetGlb
import org.apache.commons.io.{FileUtils, IOUtils}
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import utils.Zip

/**
  * Created by wulinhao on 2020/04/16.
  */
@Singleton
class GlbBuilder @Inject()(ossClient: OssClient, config: Configuration) {
  val logger = LoggerFactory.getLogger("application")
  val glb = config.get[String]("glb.bin")
  val glbTmp = config.get[String]("glb.tmp")


  def change(fbxFile: String, glbFile: String): Option[String] = {
    val cmd = Array(glb, "-i", fbxFile, "-b", "-o", glbFile)
    logger.info(s"run with command ${cmd.mkString(" ")}")
    val process = Runtime.getRuntime.exec(cmd)
    val result: Int = process.waitFor
    if (result == 0) {
      Option(s"${glbFile}.glb")
    }
    else {
      val errorMsg = IOUtils.toString(process.getErrorStream, StandardCharsets.UTF_8)
      logger.error("change error " + errorMsg)
      None
    }
  }


  def downloadAndChange(zip: String, glbKey: String): Option[String] = {
    //开始转换
    val zipPath = ossClient.urlToKey(zip)
    val downloadName = StringUtils.replaceChars(zipPath, "/", "_")
    val tempDir = s"${glbTmp}/${downloadName}"
    new File(tempDir).mkdir()
    val zipFile = s"${tempDir}/data.zip"
    val unzipDir = s"${tempDir}/unzip"
    val glbFile = s"${tempDir}/data_glb"

    ossClient.downFile(zipPath, zipFile)
    Zip.extractFolder(zipFile, unzipDir)
    val files = new File(unzipDir).listFiles()
    val fbxFile = files.filter(one => {
      one.getName.endsWith(".fbx")
    }).head

    val lastGlbFile = change(fbxFile.getAbsolutePath, glbFile)
    if (lastGlbFile.isDefined) {
      logger.info(s"finish change and upload file ${lastGlbFile.get} to $glbKey")
      ossClient.uploadFile(glbKey, lastGlbFile.get)
      val glbUrl = ossClient.signPath(glbKey)
      logger.info(s"clean tmp dir")
      FileUtils.deleteDirectory(new File(tempDir))
      Option(glbUrl)
    }
    else None
  }
}
