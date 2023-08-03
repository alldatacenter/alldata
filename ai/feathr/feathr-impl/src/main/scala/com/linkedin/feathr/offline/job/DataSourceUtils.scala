package com.linkedin.feathr.offline.job


import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrInputDataException}
import org.apache.avro.Schema
import org.apache.avro.file.DataFileReader
import org.apache.avro.generic.{GenericDatumReader, GenericRecord}
import org.apache.avro.mapred.FsInput
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.mapred.JobConf
import org.apache.logging.log4j.LogManager


object DataSourceUtils {
  val log = LogManager.getLogger(getClass)

  private val AVRO_EXTENSION = "avro"


  /**
   * Load the Avro schema for some Avro data set in HDFS
   */
  def getSchemaForAvroData(conf: Configuration, inputDir: String): Schema = {
    val fs = FileSystem.get(conf)
    val status = fs.listStatus(new Path(inputDir))

    // paths of all the avro files in the directory
    val avroFiles = status.filter(_.getPath.getName.endsWith(".avro"))

    // return null if directory doesn't contain any avro file.
    if (avroFiles.length == 0) {
      throw new FeathrInputDataException(ErrorLabel.FEATHR_USER_ERROR, s"Load the Avro schema for Avro data set in HDFS but no avro files found in $inputDir.")
    }

    // get the first avro file in the directory
    val dataFileName = avroFiles(0).getPath.getName
    val dataFilePath = new Path(inputDir, dataFileName).toString

    // Get the schema of the avro GenericRecord
    getSchemaFromAvroDataFile(dataFilePath, new JobConf(conf))
  }

  /**
   * Retrieve avro schema from a HDFS path to a file or directory
   * If a directory is given, this method will fetch schema from the first file
   * This is suggested to use for private dataset.
   *
   * @param avroPath the hdfs path to the avro file or dir
   * @param jobConf optional job config
   * @return the avro schema to the avro file or dir
   */
  def getSchemaFromAvroDataFile(avroPath: String, jobConf: JobConf = new JobConf): Schema = {
    val fs = FileSystem.get(jobConf)
    // If input is a directory, find first file in the directory
    val avroFilePath = fs.listStatus(new Path(avroPath))
      .filter(_.getPath.getName.endsWith(AVRO_EXTENSION))
    if (avroFilePath.isEmpty) {
      throw new RuntimeException(s"Input path does not have avro files: ${avroPath}")
    }
    val fsInput = new FsInput(avroFilePath(0).getPath, new Configuration)
    val reader = new GenericDatumReader[GenericRecord]
    val dataFileReader = DataFileReader.openReader(fsInput, reader)
    val schema = dataFileReader.getSchema
    try {
      dataFileReader.close
    } catch {
      case e: Exception => {
        // Ignore file close exception
      }
    }
    schema
  }
}
