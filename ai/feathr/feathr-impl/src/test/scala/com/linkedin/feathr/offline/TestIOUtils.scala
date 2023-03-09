package com.linkedin.feathr.offline

import java.io.{File, PrintWriter}

object TestIOUtils {

  /**
   * Write a given input string to a given output path
   *
   * @param text       the text to write to the output path
   * @param outputPath the output path
   */
  def writeToFile(text: String, outputPath: String): Unit = {
    val outputFolder = new File(outputPath)
    if (!outputFolder.getParentFile.exists) outputFolder.getParentFile.mkdirs

    val pw = new PrintWriter(new File(outputPath))
    pw.write(text)
    pw.close()
  }

}
