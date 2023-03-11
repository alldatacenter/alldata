package com.netease.arctic.spark.sql.catalyst.plans

import org.apache.spark.sql.catalyst.analysis.PartitionSpec
import org.apache.spark.sql.catalyst.plans.logical.{Command, LogicalPlan}

case class AlterArcticTableDropPartition(
                                          child: LogicalPlan,
                                          parts: Seq[PartitionSpec],
                                          ifExists: Boolean,
                                          purge: Boolean,
                                          retainData: Boolean) extends Command {

  override def children: Seq[LogicalPlan] = child :: Nil
}
