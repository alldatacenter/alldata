package com.platform.antlr.parser.common.relational

import com.platform.antlr.parser.common.relational.Statement

abstract class AbsTableStatement: Statement() {
    abstract val tableId: TableId
}