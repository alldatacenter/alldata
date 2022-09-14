/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.launcher.service.tools

import com.webank.wedatasphere.streamis.jobmanager.launcher.entity.{JobConfDefinition, JobConfValue}
import org.apache.commons.lang.StringUtils

import scala.collection.JavaConverters._
import java.util
/**
 * TODO dive into JobConfValueSerializer and JobConfValueDeserializer
 */
class JobConfValueUtils {

}

object JobConfValueUtils{
  /**
   * Serialize the job conf values
   * @return
   */
  def serialize(configValues: util.List[JobConfValue], definitions: util.List[JobConfDefinition]): util.Map[String, Any] = {
     // First to build a definition map
     val definitionMap: util.Map[String, JobConfDefinition] = definitions.asScala.map(definition => {
       (definition.getId.toString, definition)
     }).toMap.asJava
     // Init a value map to store relation of config values
     val relationMap: util.Map[String, Any] = new util.HashMap[String, Any]()
     configValues.asScala.foreach(keyValue => {
       val refDefId = keyValue.getReferDefId
       if (null != refDefId) {
           Option(relationMap.get(refDefId.toString)) match {
             case Some(value: util.Map[String, Any]) => {
               // Put the value into relation
               value.put(keyValue.getKey, keyValue.getValue)
             }
             case Some(value: String) => {
                // Overwrite it's value
                relationMap.put(refDefId.toString, keyValue.getValue)
             }
             case _ =>
               // Set the value/relation recursively
               var definition = definitionMap.get(refDefId.toString)
               var value: Any = if (null != definition && (StringUtils.isBlank(definition.getType) ||
                      definition.getType.equalsIgnoreCase("NONE"))) {
                  val relation = new util.HashMap[String, Any]()
                  relation.put(keyValue.getKey, keyValue.getValue)
                  relation
               } else {
                 keyValue.getValue
               }
               while (null != definition){
                   value = Option(relationMap.get(definition.getId.toString)) match {
                     case Some(existV: util.Map[String, Any]) => {
                        value match {
                          case map: util.Map[String, Any] =>
                            existV.putAll(map)
                            existV
                          case _ =>
                            relationMap.put(definition.getId.toString, value)
                            value
                        }
                     }
                     case _ =>
                       relationMap.put(definition.getId.toString, value)
                       value
                  }
                  Option(definition.getParentRef) match {
                    case Some(parentRef) =>
                      val newValue: util.Map[String, Any] = new util.HashMap[String, Any]()
                      newValue.put(definition.getKey, value)
                      definition = definitionMap.get(parentRef.toString)
                      value = newValue
                    case _ => definition = null
                  }
               }
           }
         }
       })
     // Filter the root configuration
     relationMap.asScala
       .filter(entry=> definitionMap.get(entry._1).getLevel == 0).map{
       case (defId, value) => (definitionMap.get(defId).getKey, value)
     }.asJava

  }

  /**
   * Deserialize
   * @param valueMap value map
   * @param definitions definitions
   * @return
   */
  def deserialize(valueMap: util.Map[String, Any], definitions: util.List[JobConfDefinition]):util.List[JobConfValue] = {
    // First to build a definition map
    val definitionMap: util.Map[String, JobConfDefinition] = definitions.asScala.map(definition => {
      (definition.getKey, definition)
    }).toMap.asJava
    // Configuration value list
    val configValues: util.List[JobConfValue] = new util.ArrayList[JobConfValue]()
    valueMap.asScala.foreach{
      case (key, value) => {
        Option(definitionMap.get(key)) match {
          case Some(definition) => if (definition.getLevel == 0){
            configValues.addAll(deserializeInnerObj(key, value, null, definitionMap))
          }
          case _ =>
        }
      }
    }
    configValues
  }

  private def deserializeInnerObj(key: String, value: Any, parentRef: String,
                                  definitionMap: util.Map[String, JobConfDefinition]): util.List[JobConfValue] = {
    val result: util.List[JobConfValue] = new util.ArrayList[JobConfValue]()
    if (null != value) {
      value match {
        case innerMap: util.Map[String, Any] =>
          Option(definitionMap.get(key)) match {
            case Some(definition) =>
              innerMap.asScala.foreach{
                case (childK, childV) => {
                  val childResult = deserializeInnerObj(childK, childV,
                    definition.getId.toString, definitionMap)
                  childResult.asScala.foreach(confValue => if (confValue.getReferDefId == null){
                    confValue.setReferDefId(definition.getId)
                  })
                  result.addAll(childResult)
                }
              }
            case _ => //ignore
          }

        case other: Any =>
          Option(definitionMap.get(key)) match {
            case Some(definition) =>
              if (StringUtils.isBlank(parentRef) || parentRef.equals(String.valueOf(definition.getParentRef))){
                result.add(new JobConfValue(key, String.valueOf(other), definition.getId))
              }
            case _ => result.add(new JobConfValue(key, String.valueOf(other), null))
          }
      }
    }
    result
  }
//  def main(args: Array[String]): Unit = {
//    val definitions: util.List[JobConfDefinition] = new util.ArrayList[JobConfDefinition]()
//    val configValues: util.List[JobConfValue] = new util.ArrayList[JobConfValue]()
//    definitions.add(new JobConfDefinition(0, "wds.linkis.flink.resource", "None", null, 0))
//    definitions.add(new JobConfDefinition(1, "wds.linkis.flink.custom", "None", null, 0))
//    definitions.add(new JobConfDefinition(2, "wds.linkis.flink.taskmanager.num", "NUMBER", 0, 1))
//    definitions.add(new JobConfDefinition(3, "wds.linkis.flink.jobmanager.memeory", "NUMBER", 0, 1))
//    configValues.add(new JobConfValue("wds.linkis.flink.taskmanager.num", "1", 2))
//    configValues.add(new JobConfValue("env.java.opts", "-DHADOOP_USER_NAME=hadoop", 1))
//    configValues.add(new JobConfValue("security.kerberos.login.principal", "hadoop@WEBANK.com", 1))
//    configValues.add(new JobConfValue("wds.linkis.flink.jobmanager.memeory", "1024", 3))
//    val result = serialize(configValues, definitions)
//    println(DWSHttpClient.jacksonJson.writeValueAsString(result))
//    println(DWSHttpClient.jacksonJson.writeValueAsString(deserialize(result, definitions)))
//  }
}
