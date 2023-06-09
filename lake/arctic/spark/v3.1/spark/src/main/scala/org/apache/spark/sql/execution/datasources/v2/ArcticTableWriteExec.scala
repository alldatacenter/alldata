package org.apache.spark.sql.execution.datasources.v2

import com.netease.arctic.spark.table.ArcticSparkTable
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.Attribute
import org.apache.spark.sql.connector.catalog.CatalogV2Implicits.IdentifierHelper
import org.apache.spark.sql.connector.catalog._
import org.apache.spark.sql.connector.write.{BatchWrite, LogicalWriteInfoImpl, PhysicalWriteInfoImpl, WriterCommitMessage}
import org.apache.spark.sql.execution.{BinaryExecNode, SparkPlan}
import org.apache.spark.sql.util.CaseInsensitiveStringMap
import org.apache.spark.util.{LongAccumulator, Utils}
import org.apache.spark.{SparkException, TaskContext}

import java.util.UUID
import scala.util.control.NonFatal

/**
 * This trait will be removed in 0.5.0,
 * using [[ org.apache.spark.sql.arctic.execution.ExtendedV2ExistingTableWriteExec ]] instead.
 */
@deprecated("will be removed after 0.5.0", "0.4.1")
trait ArcticTableWriteExec extends V2CommandExec with BinaryExecNode {
  def table: ArcticSparkTable

  def queryInsert: SparkPlan

  def validateQuery: SparkPlan

  var count: Long = 0L


  override def output: Seq[Attribute] = Nil

  var commitProgress: Option[StreamWriterCommitProgress] = None

  protected def validateData(): Seq[InternalRow] = {
    val rdd: RDD[InternalRow] = {
      val tempRdd = validateQuery.execute()
      // SPARK-23271 If we are attempting to write a zero partition rdd, create a dummy single
      // partition rdd to make sure we at least set up one write task to write the metadata.
      if (tempRdd.partitions.length == 0) {
        sparkContext.parallelize(Array.empty[InternalRow], 1)
      } else {
        tempRdd
      }
    }
    count = rdd.count()
    Nil
  }

  protected def writeInsert(batchWrite: BatchWrite): Seq[InternalRow] = {
    val rdd: RDD[InternalRow] = {
      val tempRdd = queryInsert.execute()
      // SPARK-23271 If we are attempting to write a zero partition rdd, create a dummy single
      // partition rdd to make sure we at least set up one write task to write the metadata.
      if (tempRdd.partitions.length == 0) {
        sparkContext.parallelize(Array.empty[InternalRow], 1)
      } else {
        tempRdd
      }
    }
    val writerFactory = batchWrite.createBatchWriterFactory(
      PhysicalWriteInfoImpl(rdd.getNumPartitions))
    val useCommitCoordinator = batchWrite.useCommitCoordinator
    val messages = new Array[WriterCommitMessage](rdd.partitions.length)
    val totalNumRowsAccumulator = new LongAccumulator()

    if (rdd.count() != count) {
      throw new UnsupportedOperationException(s"${table.table().asKeyedTable().primaryKeySpec().toString} " +
        s"can not be duplicate")
    }

    logInfo(s"Start processing data source write support: $batchWrite. " +
      s"The input RDD has ${messages.length} partitions.")

    try {
      sparkContext.runJob(
        rdd,
        (context: TaskContext, iter: Iterator[InternalRow]) => {
          DataWritingSparkTask.run(writerFactory, context, iter, useCommitCoordinator)
        },
        rdd.partitions.indices,
        (index, result: DataWritingSparkTaskResult) => {
          val commitMessage = result.writerCommitMessage
          messages(index) = commitMessage
          totalNumRowsAccumulator.add(result.numRows)
          batchWrite.onDataWriterCommit(commitMessage)
        }
      )

      logInfo(s"Data source write support $batchWrite is committing.")
      batchWrite.commit(messages)
      logInfo(s"Data source write support $batchWrite committed.")
      commitProgress = Some(StreamWriterCommitProgress(totalNumRowsAccumulator.value))
    } catch {
      case cause: Throwable =>
        logError(s"Data source write support $batchWrite is aborting.")
        try {
          batchWrite.abort(messages)
        } catch {
          case t: Throwable =>
            logError(s"Data source write support $batchWrite failed to abort.")
            cause.addSuppressed(t)
            throw new SparkException("Writing job failed.", cause)
        }
        logError(s"Data source write support $batchWrite aborted.")
        cause match {
          // Only wrap non fatal exceptions.
          case NonFatal(e) => throw new SparkException("Writing job aborted.", e)
          case _ => throw cause
        }
    }

    Nil
  }

  protected def writeToTable(
    catalog: TableCatalog,
    table: Table,
    writeOptions: CaseInsensitiveStringMap,
    ident: Identifier
  ): Seq[InternalRow] = {
    Utils.tryWithSafeFinallyAndFailureCallbacks({
      table match {
        case table: SupportsWrite =>
          val info = LogicalWriteInfoImpl(
            queryId = UUID.randomUUID().toString,
            queryInsert.schema,
            writeOptions)
          val writeBuilder = table.newWriteBuilder(info)

          val writtenRows = writeBuilder match {
            case v2 => writeInsert(v2.buildForBatch())
          }

          table match {
            case st: StagedTable => st.commitStagedChanges()
            case _ =>
          }
          writtenRows

        case _ =>
          // Table does not support writes - staged changes are also rolled back below if table
          // is staging.
          throw new SparkException(
            s"Table implementation does not support writes: ${ident.quoted}")
      }
    })(catchBlock = {
      table match {
        // Failure rolls back the staged writes and metadata changes.
        case st: StagedTable => st.abortStagedChanges()
        case _ => catalog.dropTable(ident)
      }
    })
  }

}
