package com.linkedin.feathr.offline.source.dataloader.stream

import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrException}
import com.linkedin.feathr.offline.config.datasource.KafkaResourceInfoSetter
import com.linkedin.feathr.offline.config.location.KafkaEndpoint
import org.apache.avro.Schema
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.streaming.DataStreamReader
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.util.UUID;

/**
 * This is used to load Kafka data source into a DataFrame
 *
 */
class KafkaDataLoader(ss: SparkSession, input: KafkaEndpoint) extends StreamDataLoader(ss) {
  val format = "kafka"
  val kafkaSaslConfig = KafkaResourceInfoSetter.SASL_JAAS_CONFIG

  // Construct authentication string for Kafka on Azure
  private def getKafkaAuth(ss: SparkSession): String = {
    // If user set password, then we use password to auth
    ss.conf.getOption(kafkaSaslConfig) match {
      case Some(sasl) =>
          "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\"" +
            " password=\""+ sasl +"\";"
      case _ => {
        throw new RuntimeException(s"Invalid Kafka authentication! ${kafkaSaslConfig} is not set in Spark conf.")
      }
    }
  }

  override def getDFReader(kafkaOptions: Map[String, String]): DataStreamReader = {
    val EH_SASL = getKafkaAuth(ss)
    _ss.readStream
        .format(format)
        .options(kafkaOptions)
        .option("kafka.group.id", UUID.randomUUID().toString)
        .option("kafka.sasl.jaas.config", EH_SASL)
      // These are default settings, we can expose them in the future if required
      .option("kafka.sasl.mechanism", "PLAIN")
        .option("kafka.security.protocol", "SASL_SSL")
        .option("kafka.request.timeout.ms", "60000")
        .option("kafka.session.timeout.ms", "60000")
        .option("failOnDataLoss", "false")
  }

  /**
   * load the source data as dataframe.
   *
   * @return an dataframe
   */
  override def loadDataFrame(): DataFrame = {
    // Spark conf require ',' delimited strings for multi-value config
    val topic = input.topics.mkString(",")
    val bootstrapServers= input.brokers.mkString(",")
    getDFReader(Map())
      .option("subscribe", topic)
      .option("kafka.bootstrap.servers", bootstrapServers)
      .load()
  }

  /**
   * get the schema of the source. It's only used in the deprecated DataSource.getDataSetAndSchema
   *
   * @return an Avro Schema
   */
  override def loadSchema(): Schema = ???
}