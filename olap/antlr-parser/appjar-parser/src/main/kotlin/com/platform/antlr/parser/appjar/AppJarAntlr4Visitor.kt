package com.platform.antlr.parser.appjar

import com.platform.antlr.parser.common.relational.Statement
import com.platform.antlr.parser.common.relational.common.SetStatement
import com.platform.antlr.parser.common.relational.common.UnSetStatement
import com.platform.antlr.parser.appjar.antlr4.AppJarParser
import com.platform.antlr.parser.appjar.antlr4.AppJarParserBaseVisitor
import com.platform.antlr.parser.common.util.StringUtil
import org.apache.commons.lang3.StringUtils

/**
 *
 * Created by binsong.li on 2018/3/31 下午1:44
 */
class AppJarAntlr4Visitor : AppJarParserBaseVisitor<Statement>() {

    private var command: String? = null

    private val tableDatas = ArrayList<Statement>()

    override fun visitJobTask(ctx: AppJarParser.JobTaskContext): Statement {
        val tableData = super.visitJobTask(ctx)
        tableDatas.add(tableData)

        return tableData;
    }

    override fun visitJobStatement(ctx: AppJarParser.JobStatementContext): Statement {
        val resourceName = ctx.resourceNameExpr().text
        val className = ctx.classNameExpr().text

        var params: ArrayList<String> = arrayListOf()

        if(ctx.paramsExpr() != null) {
            ctx.paramsExpr().children.forEach{ item ->
                val param = item as AppJarParser.ParamExprContext
                var value = StringUtils.substring(command, param.start.startIndex, param.stop.stopIndex + 1)
                if(StringUtils.startsWith(value, "/")) { //解决连续多个文件路径，不能正确解析
                    value = replaceWhitespace(value)

                    params.addAll(StringUtils.split(value, " "));
                } else {
                    value = StringUtil.cleanQuote(value)
                    params.add(value)
                }
            }
        }

        return AppJarInfo(resourceName, className, params)
    }

    override fun visitSetStatement(ctx: AppJarParser.SetStatementContext): Statement {
        val key = ctx.keyExpr().text
        var value = StringUtils.substring(command, ctx.value.start.startIndex, ctx.value.stop.stopIndex + 1)
        value = StringUtil.cleanQuote(value)

        return SetStatement(key, value)
    }

    override fun visitUnsetStatement(ctx: AppJarParser.UnsetStatementContext): Statement {
        val key = ctx.keyExpr().text
        return UnSetStatement(key)
    }

    private fun replaceWhitespace(str: String?): String? {
        if (str != null) {
            val len = str.length
            if (len > 0) {
                val dest = CharArray(len)
                var destPos = 0
                for (i in 0 until len) {
                    val c = str[i]
                    if (!Character.isWhitespace(c)) {
                        dest[destPos++] = c
                    } else {
                        dest[destPos++] = ' '
                    }
                }
                return String(dest, 0, destPos)
            }
        }
        return str
    }

    fun getTableDatas(): ArrayList<Statement> {
        return tableDatas
    }

    fun setCommand(command: String) {
        this.command = command
    }
}