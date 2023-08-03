package com.platform.antlr.parser.postgre.type

import com.platform.antlr.parser.common.type.AbsStringType

data class CharType(val length: Int) : AbsStringType() {
    override val name: String = "char"
    override val alias: String = "character"
    companion object {
        const val MAX_LENGTH: Int = 255
    }
}

data class VarcharType(val length: Int) : AbsStringType() {
    override val name: String = "varchar"
    override val alias: String = "character varying"
    companion object {
        const val MAX_LENGTH: Int = 65535
    }
}

class TextType : AbsStringType() {
    override val name: String = "text"
}

class ByteaType : AbsStringType() {
    override val name: String = "bytea"
}

data class EnumType(val typeName: String, val values: List<String>) : AbsStringType() {
    override val name: String = "enum"
}
