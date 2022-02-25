/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.griffin.measure.context.streaming.checkpoint.offset

import scala.collection.JavaConverters._
import scala.util.matching.Regex

import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.framework.imps.CuratorFrameworkState
import org.apache.curator.framework.recipes.locks.InterProcessMutex
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.curator.utils.ZKPaths
import org.apache.zookeeper.CreateMode

import org.apache.griffin.measure.context.streaming.checkpoint.lock.CheckpointLockInZK

/**
 * leverage zookeeper for info cache
 * @param config
 * @param metricName
 */
case class OffsetCheckpointInZK(config: Map[String, Any], metricName: String)
    extends OffsetCheckpoint
    with OffsetOps {

  val Hosts = "hosts"
  val Namespace = "namespace"
  val Mode = "mode"
  val InitClear = "init.clear"
  val CloseClear = "close.clear"
  val LockPath = "lock.path"

  val PersistentRegex: Regex = """^(?i)persist(ent)?$""".r
  val EphemeralRegex: Regex = """^(?i)ephemeral$""".r

  final val separator = ZKPaths.PATH_SEPARATOR

  val hosts: String = config.getOrElse(Hosts, "").toString
  val namespace: String = config.getOrElse(Namespace, "").toString
  val mode: CreateMode = config.get(Mode) match {
    case Some(s: String) =>
      s match {
        case PersistentRegex() => CreateMode.PERSISTENT
        case EphemeralRegex() => CreateMode.EPHEMERAL
        case _ => CreateMode.PERSISTENT
      }
    case _ => CreateMode.PERSISTENT
  }
  val initClear: Boolean = config.get(InitClear) match {
    case Some(b: Boolean) => b
    case _ => true
  }
  val closeClear: Boolean = config.get(CloseClear) match {
    case Some(b: Boolean) => b
    case _ => false
  }
  val lockPath: String = config.getOrElse(LockPath, "lock").toString

  private val cacheNamespace: String =
    if (namespace.isEmpty) metricName else namespace + separator + metricName

  private val builder = CuratorFrameworkFactory
    .builder()
    .connectString(hosts)
    .retryPolicy(new ExponentialBackoffRetry(1000, 3))
    .namespace(cacheNamespace)
  private val client: CuratorFramework = builder.build

  def init(): Unit = {
    client.start()
    info("start zk info cache")
    client.usingNamespace(cacheNamespace)
    info(s"init with namespace: $cacheNamespace")
    delete(lockPath :: Nil)
    if (initClear) {
      clear()
    }
  }

  def available(): Boolean = {
    client.getState match {
      case CuratorFrameworkState.STARTED => true
      case _ => false
    }
  }

  def close(): Unit = {
    if (closeClear) {
      clear()
    }
    info("close zk info cache")
    client.close()
  }

  def cache(kvs: Map[String, String]): Unit = {
    kvs.foreach(kv => createOrUpdate(path(kv._1), kv._2))
  }

  def read(keys: Iterable[String]): Map[String, String] = {
    keys.flatMap { key =>
      read(path(key)) match {
        case Some(v) => Some((key, v))
        case _ => None
      }
    }.toMap
  }

  def delete(keys: Iterable[String]): Unit = {
    keys.foreach { key =>
      delete(path(key))
    }
  }

  def clear(): Unit = {
//    delete("/")
    delete(finalCacheInfoPath :: Nil)
    delete(infoPath :: Nil)
    info("clear info")
  }

  def listKeys(p: String): List[String] = {
    children(path(p))
  }

  def genLock(s: String): CheckpointLockInZK = {
    val lpt = if (s.isEmpty) path(lockPath) else path(lockPath) + separator + s
    CheckpointLockInZK(new InterProcessMutex(client, lpt))
  }

  private def path(k: String): String = {
    if (k.startsWith(separator)) k else separator + k
  }

  private def children(path: String): List[String] = {
    try {
      client.getChildren.forPath(path).asScala.toList
    } catch {
      case e: Throwable =>
        warn(s"list $path warn: ${e.getMessage}")
        Nil
    }
  }

  private def createOrUpdate(path: String, content: String): Boolean = {
    if (checkExists(path)) {
      update(path, content)
    } else {
      create(path, content)
    }
  }

  private def create(path: String, content: String): Boolean = {
    try {
      client
        .create()
        .creatingParentsIfNeeded()
        .withMode(mode)
        .forPath(path, content.getBytes("utf-8"))
      true
    } catch {
      case e: Throwable =>
        error(s"create ( $path -> $content ) error: ${e.getMessage}")
        false
    }
  }

  private def update(path: String, content: String): Boolean = {
    try {
      client.setData().forPath(path, content.getBytes("utf-8"))
      true
    } catch {
      case e: Throwable =>
        error(s"update ( $path -> $content ) error: ${e.getMessage}")
        false
    }
  }

  private def read(path: String): Option[String] = {
    try {
      Some(new String(client.getData.forPath(path), "utf-8"))
    } catch {
      case e: Throwable =>
        warn(s"read $path warn: ${e.getMessage}")
        None
    }
  }

  private def delete(path: String): Unit = {
    try {
      client.delete().guaranteed().deletingChildrenIfNeeded().forPath(path)
    } catch {
      case e: Throwable => error(s"delete $path error: ${e.getMessage}")
    }
  }

  private def checkExists(path: String): Boolean = {
    try {
      client.checkExists().forPath(path) != null
    } catch {
      case e: Throwable =>
        warn(s"check exists $path warn: ${e.getMessage}")
        false
    }
  }

}
