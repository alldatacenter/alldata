/*
 * Copyright 2021 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.spline.gateway.kafka

import com.fasterxml.jackson.databind.{ObjectMapper, PropertyNamingStrategies}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.twitter.finatra.jackson.FinatraInternalModules
import org.apache.commons.configuration.ConfigurationConverter
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import org.springframework.context.annotation.{Bean, ComponentScan, Configuration}
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.core.env.{MutablePropertySources, PropertiesPropertySource}
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.{ConsumerFactory, DefaultKafkaConsumerFactory}
import org.springframework.kafka.listener.ContainerProperties.AckMode
import org.springframework.kafka.support.JacksonUtils
import org.springframework.kafka.support.converter.Jackson2JavaTypeMapper.TypePrecedence
import org.springframework.kafka.support.converter.{ByteArrayJsonMessageConverter, DefaultJackson2JavaTypeMapper, RecordMessageConverter}
import za.co.absa.commons.config.ConfTyped
import za.co.absa.commons.config.ConfigurationImplicits.{ConfigurationOptionalWrapper, ConfigurationRequiredWrapper}
import za.co.absa.spline.common.config.DefaultConfigurationStack

import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters._
import scala.concurrent.duration.{Duration, DurationInt}

@EnableKafka
@Configuration
@ComponentScan(basePackageClasses = Array(classOf[listener._package]))
class KafkaGatewayConfig {

  @Bean
  def kafkaListenerContainerFactory: ConcurrentKafkaListenerContainerFactory[_, _] =
    new ConcurrentKafkaListenerContainerFactory[String, AnyRef] {
      setConcurrency(1)
      setConsumerFactory(consumerFactory)
      setMessageConverter(messageConverter)
      getContainerProperties.setAckMode(AckMode.BATCH)
    }

  private def consumerFactory: ConsumerFactory[String, AnyRef] = {
    new DefaultKafkaConsumerFactory(consumerConfigsMerged.asJava)
  }

  private val consumerConfigsMerged: Map[String, AnyRef] = {
    KafkaGatewayConfig.Kafka.OtherConsumerConfig ++ Map[String, AnyRef](
      ConsumerConfig.GROUP_ID_CONFIG -> KafkaGatewayConfig.Kafka.Consumer.GroupId,
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> KafkaGatewayConfig.Kafka.Consumer.BootstrapServers,
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer].getName,
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG -> classOf[ByteArrayDeserializer].getName,
    )
  }

  private def messageConverter: RecordMessageConverter = {
    val typeMapper = new DefaultJackson2JavaTypeMapper {
      setTypePrecedence(TypePrecedence.TYPE_ID)
      setIdClassMapping(typeMappings.asJava)
    }

    new ByteArrayJsonMessageConverter(objectMapper) {
      setTypeMapper(typeMapper)
    }
  }

  private val typeMappings = Map[String, Class[_]](
    "ExecutionPlan" -> classOf[za.co.absa.spline.producer.model.v1_1.ExecutionPlan],
    "ExecutionEvent" -> classOf[za.co.absa.spline.producer.model.v1_1.ExecutionEvent]
  )

  private val objectMapper: ObjectMapper =
    JacksonUtils.enhancedObjectMapper()
      .registerModule(DefaultScalaModule)
      .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
      .registerModule(FinatraInternalModules.caseClassModule)

  @Bean
  def propertySourcesPlaceholderConfigurer: PropertySourcesPlaceholderConfigurer = {
    val properties = ConfigurationConverter.getProperties(KafkaGatewayConfig)

    val sources = new MutablePropertySources()
    sources.addLast(new PropertiesPropertySource("spline-property-source", properties))

    val configurer = new PropertySourcesPlaceholderConfigurer()
    configurer.setPropertySources(sources)
    configurer
  }
}

object KafkaGatewayConfig extends DefaultConfigurationStack with ConfTyped {

  override val rootPrefix: String = "spline"
  private val conf = this

  object Kafka extends Conf("kafka") {

    val Topic: String = conf.getRequiredString(Prop("topic"))

    object Consumer extends Conf("consumer") {
      val GroupId: String = conf.getRequiredString(Prop(ConsumerConfig.GROUP_ID_CONFIG))
      val BootstrapServers: String = conf.getRequiredString(Prop(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG))
    }

    val OtherConsumerConfig: Map[String, AnyRef] = ConfigurationConverter
      .getMap(subset(Prop("consumer")))
      .asScala.toMap
      .asInstanceOf[Map[String, AnyRef]]

    val PlanTimeout: Duration = conf
      .getOptionalLong(Prop("insertPlanTimeout"))
      .map(Duration(_, TimeUnit.MILLISECONDS))
      .getOrElse(1.minute)

    val EventTimeout: Duration = conf
      .getOptionalLong(Prop("insertEventTimeout"))
      .map(Duration(_, TimeUnit.MILLISECONDS))
      .getOrElse(10.seconds)
  }
}
