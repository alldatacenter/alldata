/*
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

import java.text.SimpleDateFormat
import java.util
import java.util.Calendar

import org.apache.hadoop.conf.Configuration
import org.apache.iceberg.catalog.{Namespace, TableIdentifier}
import org.apache.iceberg.hive.HiveCatalog
import org.apache.iceberg.Snapshot
import org.apache.iceberg.actions.Actions
import org.apache.spark.sql.SparkSession
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map

class CompactIcebergJob {

}

object CompactIcebergJob {
  val LOG = LoggerFactory.getLogger(this.getClass)
  // 任务参数选项
  val OPTION_HELP_FULL = "--help"
  val OPTION_HELP_SIMPLE = "-h"
  val OPTION_DATABASE_FULL = "--database"
  val OPTION_DATABASE_SIMPLE = "-d"
  val OPTION_TBLE_FULL = "--table"
  val OPTION_TABLE_SIMPLE = "-t"
  val OPTION_STEP_FULL = "--step"
  val OPTION_STEP_SIMPLE = "-s"
  val OPTION_RETAIN_SNAPSHOTS_FULL = "--retain-snapshots"
  val OPTION_RETAIN_SNAPSHOTS_SIMPLE = "-rs"
  val OPTION_RETAIN_SNAPSHOTS_MINUTE_FULL = "--retain-snapshots-minute"
  val OPTION_RETAIN_SNAPSHOTS_MINUTE_SIMPLE = "-rsm"
  val OPTION_RETAIN_SNAPSHOTS_HOUR_FULL = "--retain-snapshots-hour"
  val OPTION_RETAIN_SNAPSHOTS_HOUR_SIMPLE = "-rsh"
  val OPTION_RETAIN_SNAPSHOTS_DAY_FULL = "--retain-snapshots-day"
  val OPTION_RETAIN_SNAPSHOTS_DAY_SIMPLE = "-rsd"
  // 执行阶段及说明
  val COMPACT_ALL = "all"
  val COMPACT_REWRITE_DATA_FILE = "compact_data"
  val COMPACT_REWRITE_MANIFEST_FILE = "compact_manifest"
  val COMPACT_EXPIRE_SNAPSHOTS = "expire_snapshots"
  val COMPACT_REMOVE_ORPHAN_FILE = "remove_orphan_file"
  val COMPACT_STEP_LIST = List(COMPACT_ALL, COMPACT_REWRITE_DATA_FILE, COMPACT_REWRITE_MANIFEST_FILE,
    COMPACT_EXPIRE_SNAPSHOTS, COMPACT_REMOVE_ORPHAN_FILE)
  val COMPACT_LOAD_TABLE_DESCRIBE = "加载表阶段"
  val COMPACT_REWRITE_DATA_FILE_DESCRIBE = "重写合并data file小文件阶段"
  val COMPACT_REWRITE_MANIFEST_FILE_DESCRIBE = "重写合并manifest file小文件阶段"
  val COMPACT_EXPIRE_SNAPSHOTS_DESCRIBE = "过期及删除不需要保留的快照阶段"
  val COMPACT_REMOVE_ORPHAN_FILE_DESCRIBE = "删除孤立文件阶段"
  // 默认配置
  val COMPACT_RETAIN_SNAPS_DEFAULT = 20 // 默认保留快照个数
  val COMPACT_FAILED_RETRY_DEFAULT = 5 // 默认各阶段重试次数
  val COMPACT_FAILED_WAITED_DEFAULT = 3000 // 默认重试等待时间ms

  def main(args: Array[String]): Unit = {
    val parameters = parseParameters(args)
    if (parameters == null || parameters.help) {
      printArgsHelp()
    } else if (parameters != null && parameters.error != null && !"".equals(parameters.error)) {
      LOG.error(parameters.error)
    } else {
      if (parameters.table != null && !"".equals(parameters.table)) {
        compactIcebergTable(parameters)
      } else {
        compactIcebergDB(parameters)
      }
    }
  }

  def compactIcebergTable(parameters: ParameterArgs): Unit = {
    val hiveDB = parameters.db
    val table = parameters.table
    val step = parameters.step
    var retainSnapsNum: Int = parameters.retainSnapsNum
    val retainSnapsMs: Long = parameters.retainSnapsMs
    if (retainSnapsNum < -1 && retainSnapsMs < -1) {
      retainSnapsNum = COMPACT_RETAIN_SNAPS_DEFAULT
    }
    val sparkSession = getSparkSession(hiveDB + "." + table)

    // 构建Iceberg Hive Catalog加载Iceberg表
    val config: util.Map[String, String] = new util.HashMap[String, String]
    config.put("type", "iceberg")
    config.put("catalog-type", "hive")
    config.put("property-version", "2");

    val hiveCatalog: HiveCatalog = new HiveCatalog(new Configuration)
    hiveCatalog.initialize("hive", config)

    val errorList: ListBuffer[Tuple4[String, String, Int, Exception]] = new ListBuffer[(String, String, Int, Exception)]

    // 使用Iceberg提供的hiveCatalog扫描hive库下所有Iceberg类型表
    val tableId: TableIdentifier = TableIdentifier.of(Namespace.of(hiveDB), table)
    val icebergTable = try {
      hiveCatalog.loadTable(tableId)
    } catch {
      case e: Exception =>
        errorList.append((hiveDB + "." + table, COMPACT_LOAD_TABLE_DESCRIBE, 0, e))
        null
    }

    if (icebergTable != null) {
      val startTime: Long = System.currentTimeMillis()
      if (COMPACT_ALL.equalsIgnoreCase(step)) {
        // 合并data files
        rewriteDataFiles(hiveCatalog, tableId, sparkSession, 0, errorList)
        // 合并manifest files
        rewriteManifestFiles(hiveCatalog, tableId, sparkSession, 0, errorList)
        // 只保存最近的n个快照，其他的均过期
        expireSnaps(hiveCatalog, tableId, retainSnapsNum, retainSnapsMs, true, 0, errorList)
        // 清除孤立文件
        removeOrphanFiles(hiveCatalog, tableId, sparkSession, 0, errorList)
      } else {
        if (COMPACT_REWRITE_DATA_FILE.equalsIgnoreCase(step)) {
          // 合并data files
          rewriteDataFiles(hiveCatalog, tableId, sparkSession, 0, errorList)
        }
        if (COMPACT_REWRITE_MANIFEST_FILE.equalsIgnoreCase(step)) {
          // 合并manifest files
          rewriteManifestFiles(hiveCatalog, tableId, sparkSession, 0, errorList)
        }
        if (COMPACT_EXPIRE_SNAPSHOTS.equalsIgnoreCase(step)) {
          // 只保存最近的n个快照，其他的均过期
          expireSnaps(hiveCatalog, tableId, retainSnapsNum, retainSnapsMs, true, 0, errorList)
        }
        if (COMPACT_REMOVE_ORPHAN_FILE.equalsIgnoreCase(step)) {
          // 清除孤立文件
          removeOrphanFiles(hiveCatalog, tableId, sparkSession, 0, errorList)
        }
      }

      // 输出执行操作日志信息
      logInformation(hiveCatalog, tableId, hiveDB, step,
        System.currentTimeMillis() - startTime, retainSnapsNum, retainSnapsMs)
    } else {
      LOG.warn("加载到的Hive数据库[" + hiveDB + "]，Iceberg表[" + table + "]为空.")
    }

    LOG.info(s"执行IcebergDB_[${parameters.db}.${parameters.table}" +
      s"]_CompactJob任务的启动参数：${parameters.toString}")
    errorList.foreach(error =>
      LOG.error("Hive数据库[" + hiveDB + "]，Iceberg表[" + error._1 + "]，在执行[" + error._2 + "]发生异常，重试次数为["
        + error._3 + "]，异常信息为[" + error._4.getMessage + "]"))

    sparkSession.close()
  }

  def compactIcebergDB(parameters: ParameterArgs): Unit = {
    val hiveDB = parameters.db
    val step = parameters.step
    var retainSnapsNum: Int = parameters.retainSnapsNum
    val retainSnapsMs: Long = parameters.retainSnapsMs
    if (retainSnapsNum < -1 && retainSnapsMs < -1) {
      retainSnapsNum = COMPACT_RETAIN_SNAPS_DEFAULT
    }
    val sparkSession = getSparkSession(hiveDB)

    // 构建Iceberg Hive Catalog加载Iceberg表
    val config: util.Map[String, String] = new util.HashMap[String, String]
    config.put("type", "iceberg")
    config.put("catalog-type", "hive")
    config.put("property-version", "2");

    val hiveCatalog: HiveCatalog = new HiveCatalog(new Configuration)
    hiveCatalog.initialize("hive", config)

    val errorList: ListBuffer[Tuple4[String, String, Int, Exception]] = new ListBuffer[(String, String, Int, Exception)]

    // 使用Iceberg提供的hiveCatalog扫描hive库下所有Iceberg类型表
    val icebergTableIdList = hiveCatalog.listTables(Namespace.of(hiveDB))
    val costTimeMap: Map[String, Long] = new mutable.HashMap[String, Long]()
    if (icebergTableIdList == null || icebergTableIdList.size() == 0) {
      LOG.warn("Iceberg Compact任务，Hive数据库[" + hiveDB + "]内没有Iceberg相关表.")
    } else {
      // 遍历Iceberg表进行小文件合并删除操作
      icebergTableIdList.forEach((tableId: TableIdentifier) => {
        if (tableId != null) {
          val startTime: Long = System.currentTimeMillis()
          if (COMPACT_ALL.equalsIgnoreCase(step)) {
            // 合并data files
            rewriteDataFiles(hiveCatalog, tableId, sparkSession, 0, errorList)
            // 合并manifest files
            rewriteManifestFiles(hiveCatalog, tableId, sparkSession, 0, errorList)
            // 只保存最近的n个快照，其他的均过期
            expireSnaps(hiveCatalog, tableId, retainSnapsNum, retainSnapsMs, true, 0, errorList)
            // 清除孤立文件
            removeOrphanFiles(hiveCatalog, tableId, sparkSession, 0, errorList)
          } else {
            if (COMPACT_REWRITE_DATA_FILE.equalsIgnoreCase(step)) {
              // 合并data files
              rewriteDataFiles(hiveCatalog, tableId, sparkSession, 0, errorList)
            }
            if (COMPACT_REWRITE_MANIFEST_FILE.equalsIgnoreCase(step)) {
              // 合并manifest files
              rewriteManifestFiles(hiveCatalog, tableId, sparkSession, 0, errorList)
            }
            if (COMPACT_EXPIRE_SNAPSHOTS.equalsIgnoreCase(step)) {
              // 只保存最近的n个快照，其他的均过期
              expireSnaps(hiveCatalog, tableId, retainSnapsNum, retainSnapsMs, true, 0, errorList)
            }
            if (COMPACT_REMOVE_ORPHAN_FILE.equalsIgnoreCase(step)) {
              // 清除孤立文件
              removeOrphanFiles(hiveCatalog, tableId, sparkSession, 0, errorList)
            }
          }
          val endTime = System.currentTimeMillis()
          costTimeMap.put(tableId.name(), endTime - startTime)
        }
      })

      // 输出执行操作日志信息
      icebergTableIdList.forEach((tableId: TableIdentifier) => {
        logInformation(hiveCatalog, tableId, hiveDB, step,
          costTimeMap.get(tableId.name()).getOrElse(0L), retainSnapsNum, retainSnapsMs)
      })

      LOG.info(s"执行IcebergDB_[${parameters.db}]_CompactJob任务的启动参数：${parameters.toString}")
      errorList.foreach(error =>
        LOG.error("Hive数据库[" + hiveDB + "]，Iceberg表[" + error._1 + "]，在执行[" + error._2 + "]发生异常，重试次数为["
          + error._3 + "]，异常信息为[" + error._4.getMessage + "]"))
    }

    sparkSession.close()
  }

  def rewriteDataFiles(hiveCatalog: HiveCatalog, tableId: TableIdentifier,
                       sparkSession: SparkSession, retry: Int,
                       errorList: ListBuffer[Tuple4[String, String, Int, Exception]]): Unit = {
    // 清理掉不需要保留快照的文件
    try {
      // 执行iceberg表data file合并
      Actions.forTable(sparkSession, hiveCatalog.loadTable(tableId))
        .rewriteDataFiles()
        .splitOpenFileCost(2048) // 将尽可能足够小的文件合并成一个大文件
        .splitLookback(10000) // 将尽可能足够多的文件合并成一个大文件
        .targetSizeInBytes(1024 * 1024 * 1024) // 默认合并后的文件最大为1GB
        .execute()
    } catch {
      case e: Exception =>
        errorList.append((tableId.name(), COMPACT_REWRITE_DATA_FILE_DESCRIBE, retry, e))
        if (retry <= COMPACT_FAILED_RETRY_DEFAULT) {
          Thread.sleep(COMPACT_FAILED_WAITED_DEFAULT)
          val nextRetry = retry + 1
          rewriteDataFiles(hiveCatalog, tableId, sparkSession, nextRetry, errorList)
        }
    }
  }

  def rewriteManifestFiles(hiveCatalog: HiveCatalog, tableId: TableIdentifier,
                           sparkSession: SparkSession, retry: Int,
                           errorList: ListBuffer[Tuple4[String, String, Int, Exception]]): Unit = {
    try {
      // 执行iceberg表manifest file合并 （必须使用同步的方式）
      hiveCatalog.loadTable(tableId)
        .rewriteManifests()
        .rewriteIf(m => m.hasAddedFiles)
        .rewriteIf(manifest => manifest.length() < 100 * 1024 * 1024) // 默认合并100MB以内的manifest文件
        .commit()
    } catch {
      case e: Exception =>
        errorList.append((tableId.name(), COMPACT_REWRITE_MANIFEST_FILE_DESCRIBE, retry, e))
        if (retry <= COMPACT_FAILED_RETRY_DEFAULT) {
          Thread.sleep(COMPACT_FAILED_WAITED_DEFAULT)
          val nextRetry = retry + 1
          rewriteManifestFiles(hiveCatalog, tableId, sparkSession, nextRetry, errorList)
        }
    }
  }

  def expireSnaps(hiveCatalog: HiveCatalog, tableId: TableIdentifier, retainSnapsNum: Int,
                  retainSnapsMs: Long, cleanExpireSnap: Boolean, retry: Int,
                  errorList: ListBuffer[Tuple4[String, String, Int, Exception]]): Unit = {
    try {
      // metadata list文件清理只能通过指定表的属性（目前测试来看这个参数会影响孤立文件删除，建议不要开启）
      // write.metadata.delete-after-commit.enabled=true（默认false）和write.metadata.previous-versions-max=10（默认100）
      val icebergTable = hiveCatalog.loadTable(tableId)
      var expireOlderTimestamp = retainSnapsMs
      if (retainSnapsNum > -1) {
        val snapshotList: ListBuffer[Snapshot] = new ListBuffer[Snapshot]
        icebergTable.snapshots()
          .forEach(snap => snapshotList.append(snap))
        val sortSnapshotList = snapshotList.sortBy(snap => snap.timestampMillis())
          .reverse
        val expireOlderSnap = if (sortSnapshotList != null && sortSnapshotList.size > retainSnapsNum
          && retainSnapsNum >= 1) {
          sortSnapshotList(retainSnapsNum - 1)
        } else if (sortSnapshotList != null && sortSnapshotList.size <= retainSnapsNum
          && retainSnapsNum >= 1) {
          sortSnapshotList.last
        } else {
          icebergTable.currentSnapshot()
        }
        if (expireOlderSnap != null) {
          expireOlderTimestamp = expireOlderSnap.timestampMillis()
        }
      }

      // 过期并删除相关快照信息（必须使用同步的方式）
      icebergTable.expireSnapshots()
        .expireOlderThan(expireOlderTimestamp) // 只保存最近的n个快照，其他的均过期删除
        .cleanExpiredFiles(cleanExpireSnap)
        .commit()
    } catch {
      case e: Exception => {
        errorList.append((tableId.name(), COMPACT_EXPIRE_SNAPSHOTS_DESCRIBE, retry, e))
        if (retry <= COMPACT_FAILED_RETRY_DEFAULT) {
          Thread.sleep(COMPACT_FAILED_WAITED_DEFAULT)
          val nextRetry = retry + 1
          expireSnaps(hiveCatalog, tableId, retainSnapsNum, retainSnapsMs, cleanExpireSnap, nextRetry, errorList)
        }
      }
    }
  }

  def removeOrphanFiles(hiveCatalog: HiveCatalog, tableId: TableIdentifier,
                        sparkSession: SparkSession, retry: Int,
                        errorList: ListBuffer[Tuple4[String, String, Int, Exception]]): Unit = {
    // 清理被过期的以及没有被引用的孤立文件
    try {
      Actions.forTable(sparkSession, hiveCatalog.loadTable(tableId))
        .removeOrphanFiles()
        .execute()
    } catch {
      case e: Exception =>
        errorList.append((tableId.name(), COMPACT_REMOVE_ORPHAN_FILE_DESCRIBE, retry, e))
        if (retry <= COMPACT_FAILED_RETRY_DEFAULT) {
          Thread.sleep(COMPACT_FAILED_WAITED_DEFAULT)
          val nextRetry = retry + 1
          removeOrphanFiles(hiveCatalog, tableId, sparkSession, nextRetry, errorList)
        }
    }
  }

  def getSparkSession(compactName: String): SparkSession = {
    // 指定Spark CataLog类型为hive，并配置Spark Iceberg支持
    SparkSession.builder()
      .config("spark.sql.catalog.spark_catalog.type", "hive")
      .config("spark.sql.catalog.spark_catalog", "org.apache.iceberg.spark.SparkSessionCatalog")
      .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
      .appName("IcebergDB_[" + compactName + "]_CompactJob")
      .enableHiveSupport()
      .getOrCreate()
  }

  def logInformation(hiveCatalog: HiveCatalog, tableId: TableIdentifier,
                     hiveDB: String, step: String, costTime: Long, retainSnapsNum: Int, retainSnapsMs: Long): Unit = {
    val startTime = System.currentTimeMillis()
    val icebergTable = hiveCatalog.loadTable(tableId)
    val snapshotList: ListBuffer[Snapshot] = new ListBuffer[Snapshot]
    icebergTable.snapshots()
      .forEach(snap => snapshotList.append(snap))
    val sortSnapshotList = snapshotList.sortBy(snap => snap.timestampMillis())
      .reverse
    val earliestSnapshotTime = if (sortSnapshotList.last != null) {
      sortSnapshotList.last.timestampMillis()
    }
    else {
      System.currentTimeMillis()
    }
    if (LOG.isDebugEnabled) {
      val snapsStr = new StringBuilder("")
      sortSnapshotList.foreach(snap => {
        if (!snapsStr.toString().equals("")) {
          snapsStr.append(",")
        }
        snapsStr.append(snap.snapshotId())
      })
      val costTimeAll = costTime + (System.currentTimeMillis() - startTime)
      LOG.debug(s"Hive数据库[$hiveDB]，Iceberg表[${tableId.name()}]合并删除操作完成，耗时[${costTimeAll}]ms，" +
        s"${
          if (retainSnapsNum > -1) {
            s"应该保留的快照个数[$retainSnapsNum]，"
          }
          else {
            s"应该保留的快照时间[${dateFormat.format(retainSnapsMs)}]，"
          }
        }" +
        s"当前存在的快照数量[${sortSnapshotList.size}]个，当前存在的快照id分别为[${snapsStr.toString()}]，" +
        s"保留最早的快照时间为[${dateFormat.format(earliestSnapshotTime)}]，执行步骤[$step].")
    } else {
      val costTimeAll = costTime + (System.currentTimeMillis() - startTime)
      LOG.info(s"Hive数据库[$hiveDB]，Iceberg表[${tableId.name()}]合并删除操作完成，耗时[${costTimeAll}]ms，" +
        s"${
          if (retainSnapsNum > -1) {
            s"应该保留的快照个数[$retainSnapsNum]，"
          }
          else {
            s"应该保留的快照时间[${dateFormat.format(retainSnapsMs)}]，"
          }
        }" +
        s"当前存在的快照数量[${sortSnapshotList.size}]个，保留最早的" +
        s"快照时间为[${dateFormat.format(earliestSnapshotTime)}]，执行步骤[$step].")
    }
  }

  def parseParameters(args: Array[String]): ParameterArgs = {
    if (args != null && args.length > 0) {
      val optionPairMap: mutable.Map[String, String] = new mutable.HashMap[String, String]()
      var i = 0
      while (i < args.length) {
        if (i < args.length && args(i) != null
          && (OPTION_HELP_FULL.equals(args(i)) || OPTION_HELP_SIMPLE.equals(args(i)))) {
          optionPairMap.put(args(i), "true")
        } else if (i < (args.length - 1) && args(i) != null
          && (args(i).startsWith("--") || args(i).startsWith("-"))) {
          if (args(i + 1) != null && (args(i + 1).startsWith("--") || args(i + 1).startsWith("-"))) {
            optionPairMap.put(args(i), "")
          } else {
            optionPairMap.put(args(i), args(i + 1))
            i = i + 1
          }
        }
        i = i + 1
      }
      val db: String = if ("".equals(matchStr(optionPairMap.get(OPTION_DATABASE_FULL)))) {
        matchStr(optionPairMap.get(OPTION_DATABASE_SIMPLE))
      }
      else {
        matchStr(optionPairMap.get(OPTION_DATABASE_FULL))
      }

      val table: String = if ("".equals(matchStr(optionPairMap.get(OPTION_TBLE_FULL)))) {
        matchStr(optionPairMap.get(OPTION_TABLE_SIMPLE))
      }
      else {
        matchStr(optionPairMap.get(OPTION_TBLE_FULL))
      }

      var step: String = if ("".equals(matchStr(optionPairMap.get(OPTION_STEP_FULL)))) {
        matchStr(optionPairMap.get(OPTION_STEP_SIMPLE))
      }
      else {
        matchStr(optionPairMap.get(OPTION_STEP_FULL))
      }

      val retainSnaps: String = if ("".equals(matchStr(optionPairMap.get(OPTION_RETAIN_SNAPSHOTS_FULL)))) {
        matchStr(optionPairMap.get(OPTION_RETAIN_SNAPSHOTS_SIMPLE))
      }
      else {
        matchStr(optionPairMap.get(OPTION_RETAIN_SNAPSHOTS_FULL))
      }

      val retainSnapsDay: String = if ("".equals(matchStr(optionPairMap.get(OPTION_RETAIN_SNAPSHOTS_DAY_FULL)))) {
        matchStr(optionPairMap.get(OPTION_RETAIN_SNAPSHOTS_DAY_SIMPLE))
      }
      else {
        matchStr(optionPairMap.get(OPTION_RETAIN_SNAPSHOTS_DAY_FULL))
      }

      val retainSnapsHours: String = if ("".equals(matchStr(optionPairMap.get(OPTION_RETAIN_SNAPSHOTS_HOUR_FULL)))) {
        matchStr(optionPairMap.get(OPTION_RETAIN_SNAPSHOTS_HOUR_SIMPLE))
      }
      else {
        matchStr(optionPairMap.get(OPTION_RETAIN_SNAPSHOTS_HOUR_FULL))
      }

      val retainSnapsMinute: String = if ("".equals(matchStr(optionPairMap.get(OPTION_RETAIN_SNAPSHOTS_MINUTE_FULL)))) {
        matchStr(optionPairMap.get(OPTION_RETAIN_SNAPSHOTS_MINUTE_SIMPLE))
      }
      else {
        matchStr(optionPairMap.get(OPTION_RETAIN_SNAPSHOTS_MINUTE_FULL))
      }

      var error = ""
      var retainSnapsNum: Int = -1
      var retainSnapsMs: Long = -1
      val cal = Calendar.getInstance()
      try {
        retainSnapsNum = if (!"".equals(retainSnaps)) {
          Integer.valueOf(retainSnaps)
        } else {
          -1
        }
        retainSnapsMs = if (!"".equals(retainSnapsMinute)) {
          cal.add(Calendar.MINUTE, Integer.valueOf(retainSnapsMinute) * -1)
          cal.getTimeInMillis
        } else if (!"".equals(retainSnapsHours)) {
          cal.add(Calendar.HOUR, Integer.valueOf(retainSnapsHours) * -1)
          cal.getTimeInMillis
        } else if (!"".equals(retainSnapsDay)) {
          cal.add(Calendar.DAY_OF_YEAR, Integer.valueOf(retainSnapsDay) * -1)
          cal.getTimeInMillis
        } else {
          -1
        }
      } catch {
        case e: Exception => error = s"IcebergDB_[$db.$table]_CompactJob任务启动失败，" +
          s"指定保留快照的个数或时间必须是一个数值类型."
      }
      if (retainSnapsNum > -1) {
        retainSnapsMs = -1
      } else if (retainSnapsMs < 0) {
        retainSnapsNum = COMPACT_RETAIN_SNAPS_DEFAULT
      }
      if ("".equals(db)) {
        error = "IcebergDB_[*]_CompactJob任务启动失败，必须要指定需要合并的Iceberg数据库"
      }
      var isCompactStep = false
      COMPACT_STEP_LIST.foreach(compactStep => {
        if (compactStep.equals(step) || "".equals(step)) {
          isCompactStep = true
        }
      })
      if (!isCompactStep) {
        error = s"IcebergDB_[$db]_CompactJob任务启动失败，不支持执行合并step[$step]，" +
          s"step参数详解$OPTION_HELP_STEP_DESC"
      }
      step = if (step != null && !"".equals(step)) {
        step
      } else {
        COMPACT_ALL
      }
      new ParameterArgs(
        if ("true".equals(matchStr(optionPairMap.get(OPTION_HELP_FULL)))
          || "true".equals(matchStr(optionPairMap.get(OPTION_HELP_SIMPLE)))) {
          true
        } else {
          false
        },
        db, table, step, retainSnapsNum, retainSnapsMs, error)
    } else {
      null
    }
  }

  def printArgsHelp(): Unit = {
    LOG.info("\n可输入参数选项：" +
      "\n  [--help/-h]                        打印可输入的全部参数选项" +
      "\n  [--database/-d]                    [必填项]指定需要做小文件合并的Iceberg数据库，只允许输入一个库，如果没有指定[--table/-t]选项则会对库下所有的Iceberg表进行小文件合并" +
      "\n  [--table/-t]                       [选填项]指定需要做小文件合并的Iceberg表，该选项的前提是必须输入了[--database/-d]选项" +
      OPTION_HELP_STEP_DESC +
      "\n  [--retain-snapshots/-rs]           [选填项]保留快照的个数，必须为数值，保留快照优先使用该参数，默认保留最近20个快照" +
      "\n  [--retain-snapshots-minute/-rsm]   [选填项]保留快照的时间，必须为数值，单位分钟，保留快照时间优先级最高" +
      "\n  [--retain-snapshots-hour/-rsh]     [选填项]保留快照的时间，必须为数值，单位小时，保留快照时间优先级较高" +
      "\n  [--retain-snapshots-day/-rsd]      [选填项]保留快照的时间，必须为数值，单位天，保留快照时间优先级最低")
  }

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  val OPTION_HELP_STEP_DESC: String =
    "\n  [--step/-s]                        [选填项]指定需要做Iceberg文件合并的操作步骤，详细步骤如下" +
      "\n                                         compact_data：表示只做数据文件(data file)的合并" +
      "\n                                         compact_manifest：表示只做清单文件(manifest file)的合并" +
      "\n                                         expire_snapshots：表示根据指定过期个数或时间进行快照过期操作" +
      "\n                                         remove_orphan_file：表示只对孤立没有引用文件做删除操作" +
      "\n                                         all：表示以上的合并步骤都执行，默认为all"

  def matchStr(strOption: Option[String]): String = {
    strOption match {
      case Some(v) => v
      case None => ""
    }
  }

  class ParameterArgs(var help: Boolean, var db: String, var table: String, var step: String,
                      var retainSnapsNum: Int, var retainSnapsMs: Long, var error: String) {
    override def toString: String = {
      s"合并数据库[$db] 合并表[${
        if (table == null || "".equals(table)) {
          "all table"
        } else {
          table
        }
      }" +
        s"] 执行合并步骤[$step] ${
          if (retainSnapsNum > -1) {
            s"快照保留最近[$retainSnapsNum]个"
          }
          else {
            s"快照保留至[${dateFormat.format(retainSnapsMs)}]时间点之后"
          }
        }"
    }
  }
}