package com.platform.antlr.parser.postgre.type

import com.platform.antlr.parser.common.type.AbsNumericType

data class SmallIntType(val length: Int) : AbsNumericType() {
    override val name: String = "smallint"
    override val alias: String = "int2"
    companion object {
        const val MIN_VALUE: Int = -32768
        const val MAX_VALUE: Int = 32767
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

data class BigIntType(val length: Int) : AbsNumericType() {
    override val name: String = "bigint"
    override val alias: String = "int8"
}

data class NumericType(
    val precision: Int = 10,
    val scale: Int = 0) : AbsNumericType() {
    override val name: String = "numeric"
    override val alias: String = "decimal"
}

data class FloatType(
    val precision: Int,
    val scale: Int = 0) : AbsNumericType() {
    override val name: String = "float"
}

data class DoubleType(
    val precision: Int,
    val scale: Int = 0) : AbsNumericType() {
    override val name: String = "double precision"
    override val alias: String = "float8"
}

data class RealType(
    val precision: Int,
    val scale: Int = 0) : AbsNumericType() {
    override val name: String = "real"
    override val alias: String = "float4"
}

data class SmallserialType(val length: Int) : AbsNumericType() {
    override val name: String = "smallserial"
    override val alias: String = "serial2"
    companion object {
        const val MIN_VALUE: Int = 1
        const val MAX_VALUE: Int = 32767
    }
}

data class SerialType(val length: Int) : AbsNumericType() {
    override val name: String = "serial"
    override val alias: String = "serial4"
    companion object {
        const val MIN_VALUE: Long = 1
        const val MAX_VALUE: Long = 2147483647L
    }
}

data class BigserialType(val length: Int) : AbsNumericType() {
    override val name: String = "bigserial"
    override val alias: String = "serial8"
}

data class BitType(val length: Int) : AbsNumericType() {
    override val name: String = "bit"
}

data class VarbitType(val length: Int) : AbsNumericType() {
    override val name: String = "bit varying"
    override val alias: String = "varbit"
}