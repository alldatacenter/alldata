package com.netease.arctic.spark.sql.utils

import com.netease.arctic.spark.sql.utils.ArcticAuthUtil.ArcticActionType.ArcticActionType
import com.netease.arctic.spark.sql.utils.ArcticAuthUtil.ArcticCommandType.ArcticCommandType
import com.netease.arctic.spark.sql.utils.ArcticAuthUtil.ArcticOperationType.ArcticOperationType


object ArcticAuthUtil {

  def operationType(command: String): ArcticOperationType = {
    command match {
      case "ReplaceArcticData" |
           "AppendArcticData" |
           "OverwriteArcticData" |
           "OverwriteArcticDataByExpression" => ArcticOperationType.QUERY
      case "CreateArcticTableAsSelect" => ArcticOperationType.CREATETABLE_AS_SELECT
    }
  }

  def actionType(command: String): ArcticActionType = {
    command match {
      case "ReplaceArcticData" |
           "OverwriteArcticData" |
           "OverwriteArcticDataByExpression" => ArcticActionType.UPDATE
      case "AppendArcticData" => ArcticActionType.INSERT
      case "CreateArcticTableAsSelect" => ArcticActionType.OTHER
    }
  }

  def commandType(command: String): Seq[ArcticCommandType]  = {
    command match {
      case "ReplaceArcticData" | "AppendArcticData" =>
        Seq(ArcticCommandType.HasTableAsIdentifierOption,
        ArcticCommandType.HasQueryAsLogicalPlan)
      case "OverwriteArcticData" |
           "OverwriteArcticDataByExpression" =>
        Seq(ArcticCommandType.HasTableAsIdentifierOption,
        ArcticCommandType.HasQueryAsLogicalPlan)
      case "CreateArcticTableAsSelect" =>
        Seq(ArcticCommandType.HasTableNameAsIdentifier,
        ArcticCommandType.HasQueryAsLogicalPlan)
    }
  }

  def isArcticCommand(command: String) : Boolean ={
    command.contains("arctic")
  }

  object ArcticCommandType extends Enumeration {
    type ArcticCommandType = Value
    val HasChildAsIdentifier, HasQueryAsLogicalPlan, HasTableAsIdentifier,
    HasTableAsIdentifierOption, HasTableNameAsIdentifier = Value
  }

  object ArcticActionType extends Enumeration {
    type ArcticActionType = Value

    val OTHER, INSERT, INSERT_OVERWRITE, UPDATE, DELETE = Value
  }

  object ArcticOperationType extends Enumeration {
    type ArcticOperationType = Value

    val QUERY, CREATETABLE_AS_SELECT = Value
  }

}


