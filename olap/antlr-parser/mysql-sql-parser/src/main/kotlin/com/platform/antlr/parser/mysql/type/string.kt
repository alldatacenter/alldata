package com.platform.antlr.parser.mysql.type

import com.platform.antlr.parser.common.type.AbsStringType

data class CharType(val length: Int) : AbsStringType() {
    override val name: String = "char"
    companion object {
        const val MAX_LENGTH: Int = 255
    }
}

data class VarcharType(val length: Int) : AbsStringType() {
    override val name: String = "varchar"
    companion object {
        const val MAX_LENGTH: Int = 65535
    }
}

data class BinaryType(val length: Int) : AbsStringType() {
    override val name: String = "binary"
    companion object {
        const val MAX_LENGTH: Int = 255
    }
}

data class VarbinaryType(val length: Int) : AbsStringType() {
    override val name: String = "varbinary"
    companion object {
        const val MAX_LENGTH: Int = 65535
    }
}

class TinyblobType : AbsStringType() {
    override val name: String = "tinyblob"
}
class BlobType : AbsStringType() {
    override val name: String = "blob"
}
class MediumblobType : AbsStringType() {
    override val name: String = "mediumblob"
}
class LongblobType : AbsStringType() {
    override val name: String = "longblob"
}

class TinytextType : AbsStringType() {
    override val name: String = "tinytext"
}
class TextType : AbsStringType() {
    override val name: String = "text"
}
class MediumtextType : AbsStringType() {
    override val name: String = "mediumtext"
}
class LongtextType : AbsStringType() {
    override val name: String = "longtext"
}

data class EnumType(val values: List<String>) : AbsStringType() {
    override val name: String = "enum"
}

data class SetType(val values: List<String>) : AbsStringType() {
    override val name: String = "set"
}
