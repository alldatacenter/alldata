package com.platform.antlr.parser.postgre.type

import com.platform.antlr.parser.common.type.AbsType

class BooleanType: AbsType() {
    override val name: String = "boolean"
    override val alias: String = "bool"
}