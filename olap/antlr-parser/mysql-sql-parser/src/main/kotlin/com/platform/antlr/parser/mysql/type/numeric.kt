package com.platform.antlr.parser.mysql.type

import com.platform.antlr.parser.common.type.AbsNumericType

data class TinyintType(
    val length: Int,
    val unsigned: Boolean = false,
    val zerofill: Boolean = false) : AbsNumericType() {
    override val name: String = "tinyint"
    override val alias: String = "int1"
    companion object {
        const val MIN_VALUE: Short = -128
        const val MAX_VALUE: Short = 127
    }
}

data class SmallIntType(
    val length: Int,
    val unsigned: Boolean = false,
    val zerofill: Boolean = false) : AbsNumericType() {
    override val name: String = "smallint"
    override val alias: String = "int2"
    companion object {
        const val MIN_VALUE: Int = -32768
        const val MAX_VALUE: Int = 32767
    }
}

data class MediumIntType(
    val length: Int,
    val unsigned: Boolean = false,
    val zerofill: Boolean = false) : AbsNumericType() {
    override val name: String = "mediumint"
    override val alias: String = "int3"
    companion object {
        const val MIN_VALUE: Int = -8388608
        const val MAX_VALUE: Int = 8388607
    }
}

data class IntegerType(val length: Int) : AbsNumericType() {
    override val name: String = "integer"
    override val alias: String = "int"
    override val alias2: String = "int4"
    companion object {
        const val MIN_VALUE: Long = -2147483648L
        const val MAX_VALUE: Long = 2147483647L
    }
}

data class BigIntType(
    val length: Int,
    val unsigned: Boolean = false,
    val zerofill: Boolean = false) : AbsNumericType() {
    override val name: String = "bigint"
    override val alias: String = "int8"
}

data class DecimalType(
    val precision: Int = 10,
    val scale: Int = 0) : AbsNumericType() {
    override val name: String = "decimal"
}

data class FloatType(
    val precision: Int,
    val scale: Int = 0) : AbsNumericType() {
    override val name: String = "float"
}

data class DoubleType(
    val precision: Int,
    val scale: Int = 0) : AbsNumericType() {
    override val name: String = "double"
    override val alias: String = "float8"
}

data class RealType(
    val precision: Int,
    val scale: Int = 0) : AbsNumericType() {
    override val name: String = "real"
    override val alias: String = "float4"
}

data class BitType(val length: Int) : AbsNumericType() {
    override val name: String = "bit"
}


