package com.qlangtech.tis.realtime.transfer.search4employee4local

import com.qlangtech.tis.realtime.transfer.ruledriven.AllThreadLocal.addThreadLocalVal
import com.qlangtech.tis.realtime.transfer.impl.DefaultPk
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName
import com.google.common.collect.Lists

import com.qlangtech.tis.realtime.transfer.ruledriven.FunctionUtils._

import com.qlangtech.tis.realtime.transfer.ruledriven.MediaResultKey.putMediaResult
import com.qlangtech.tis.realtime.transfer.ruledriven.MediaResultKey.getMediaResult

import com.qlangtech.tis.wangjubao.jingwei.Alias.Builder.alias
import com.qlangtech.tis.wangjubao.jingwei.Alias.Builder.$
import com.qlangtech.tis.wangjubao.jingwei.Alias.Builder
import com.qlangtech.tis.realtime.transfer.ruledriven.BasicRuleDrivenPojoConsumer
import com.qlangtech.tis.realtime.transfer.ruledriven.BasicRuleDrivenWrapper

import java.util.Collections
import java.util.List
import scala.collection.mutable.Map
import java.util.Set
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import com.qlangtech.tis.ibatis.RowMap
import com.qlangtech.tis.realtime.transfer.DTO

import com.qlangtech.tis.realtime.transfer.BasicPojoConsumer
import com.qlangtech.tis.realtime.transfer.IPk
import com.qlangtech.tis.realtime.transfer.IRowValueGetter
import com.qlangtech.tis.realtime.transfer.ITable
import com.qlangtech.tis.realtime.transfer.impl.DefaultPojo
import com.qlangtech.tis.realtime.transfer.ruledriven.BasicRuleDrivenListener
import com.qlangtech.tis.realtime.transfer.ruledriven.FunctionUtils.Case
import com.qlangtech.tis.realtime.transfer.ruledriven.GroupKey
import com.qlangtech.tis.realtime.transfer.ruledriven.GroupValues

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.util.concurrent.RateLimiter
import com.qlangtech.tis.pubhook.common.Nullable
import com.qlangtech.tis.wangjubao.jingwei.Alias.ILazyTransfer

import com.qlangtech.tis.wangjubao.jingwei.AliasList
import com.qlangtech.tis.wangjubao.jingwei.AliasList.BuilderList

import com.qlangtech.tis.realtime.transfer.impl.DefaultTable
import com.qlangtech.tis.realtime.transfer.impl.CompositePK
import S4employee4localListener._

import com.qlangtech.tis.extension.TISExtension;

import scala.collection.JavaConverters._
import scala.util.control.Breaks._

object S4employee4localListener {

private val log: Logger = LoggerFactory.getLogger(classOf[S4employee4localListener])

private val NUM_CONSUME_NUM_5: Int = 5


}

@TISExtension(name = "default", ordinal = 0)
class S4employee4localListener extends BasicRuleDrivenListener {

private var _tabsFocuse : java.util.Set[String] = _

override def createProcessRate(): RateLimiter = RateLimiter.create(600)

override def getShardIdName( tabName : String ) : String = {
val shardIdVal = tabName match {
        case "employees" => "emp_no"
    case _=> throw new IllegalArgumentException("table name:"+ tabName +" is illegal");
}
shardIdVal
}


protected override def processColsMeta(builder: BuilderList): Unit = {
val employeesBuilder:AliasList.Builder = builder.add("employees").setPrimaryTableOfIndex()
employeesBuilder.add( //
("processTime").processTimeVer()
,("gender")
,("emp_no").PK()
,("birth_date")
,("last_name")
,("hire_date")
,("first_name")
);

}




override def createRowsWrapper(): DefaultPojo = new BasicRuleDrivenWrapper(this)

override def getConsumeNum(): Int = NUM_CONSUME_NUM_5

protected override def createPojoConsumer(): BasicPojoConsumer = {
val consumer = new BasicRuleDrivenPojoConsumer(this,this.tabColumnMetaMap.asScala){}
consumer
}

protected override def pushPojo2Queue(pk: IPk, table: ITable): Unit = {
super.pushPojo2Queue(pk, table)
}



/**
* 取得索引宽表主键
* @return
*/
override def getPk( row : ITable) : DefaultPk = {

if (row.getEventType() == DTO.EventType.DELETE) {
return null;
}

val resultPK = row.getTableName() match {
        case "employees" => getEmployeesPK(row)
    case _=> throw new IllegalArgumentException("table name:"+ row.getTableName()+" is illegal");
}
resultPK
}

protected override def getTableFocuse() : java.util.Set[String] = {
if(_tabsFocuse == null){
_tabsFocuse = com.google.common.collect.Sets.newHashSet( "employees");
}
_tabsFocuse
}

        private def getEmployeesPK(row : ITable) : DefaultPk = {

    val columnMeta : AliasList = tabColumnMetaMap.get("employees");
    if (columnMeta == null) {
    throw new IllegalStateException("tableName: employees is not exist in tabColumnMetaMap");
    }
                return new CompositePK(row.getColumn("emp_no")  );
            } // END getEmployeesPK
    // globalScripts


}
