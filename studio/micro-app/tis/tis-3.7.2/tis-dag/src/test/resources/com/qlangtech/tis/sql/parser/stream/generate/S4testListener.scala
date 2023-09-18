package com.qlangtech.tis.realtime.transfer.search4test

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

import com.qlangtech.tis.realtime.employees.pojo.DeptEmpColEnum
import com.qlangtech.tis.realtime.employees.pojo.DeptEmpCriteria
import com.qlangtech.tis.realtime.employees.pojo.DeptEmp

import com.qlangtech.tis.realtime.employees.dao.IEmployeesDAOFacade
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
import S4testListener._

import com.qlangtech.tis.extension.TISExtension;

import scala.collection.JavaConverters._
import scala.util.control.Breaks._

object S4testListener {

private val log: Logger = LoggerFactory.getLogger(classOf[S4testListener])

val PK_TOTALPAY_ID: String = "totalpay_id"

//private val NULL_PK: NullCompositePK = new NullCompositePK()

private val payinfosThreadLocal: ThreadLocal[Map[GroupKey, GroupValues]] = addThreadLocalVal()

private val instancedetailsThreadLocal: ThreadLocal[Map[GroupKey, GroupValues]] = addThreadLocalVal()

private val specialfeesThreadLocal: ThreadLocal[Map[GroupKey, GroupValues]] = addThreadLocalVal()

private val NUM_CONSUME_NUM_5: Int = 5

private val ENTITY_ID: String = "entity_id"

}

@TISExtension(name = "default", ordinal = 0)
class S4testListener extends BasicRuleDrivenListener {

private var _tabsFocuse : java.util.Set[String] = _

override def createProcessRate(): RateLimiter = RateLimiter.create(600)

protected override def processColsMeta(builder: BuilderList): Unit = {
  val deptEmpBuilder:AliasList.Builder = builder.add("dept_emp").setPrimaryTableOfIndex()
deptEmpBuilder.add( // 
("dept_no")
,("from_date").timestampVer().t((row, fieldValue) => {
    val deptEmps:Map[GroupKey /*emp_no*/, GroupValues]  = mapDeptEmpData(row)

    var result:Any = null
    breakable {
    for ((k:GroupKey, v:GroupValues)  <- deptEmps){
        result = max(v, (r) => { 
r.getColumn("from_date")})
        break

    }
    } //end breakable
     result //return

})/*end .t()*/
,("to_date").t((row, fieldValue) => {
    val deptEmps:Map[GroupKey /*emp_no*/, GroupValues]  = mapDeptEmpData(row)

    var result:Any = null
    breakable {
    for ((k:GroupKey, v:GroupValues)  <- deptEmps){
        result = max(v, (r) => { 
r.getColumn("to_date")})
        break

    }
    } //end breakable
     result //return

})/*end .t()*/
,("emp_no").PK()
);
deptEmpBuilder.setGetterRowsFromOuterPersistence(/*gencode5*/
 (rowTabName, rvals, pk ) =>{
    var deptEmps: List[RowMap]  = null    
    var deptEmpCriteria :DeptEmpCriteria = new DeptEmpCriteria()    
    deptEmpCriteria.createCriteria()    .andEmpNoEqualTo(pk.getValue())
    deptEmpCriteria.addSelCol(DeptEmpColEnum.DEPT_NO ,DeptEmpColEnum.FROM_DATE ,DeptEmpColEnum.TO_DATE ,DeptEmpColEnum.EMP_NO)    
    deptEmps = this.employeesDAOFacade.getDeptEmpDAO().selectColsByExample(deptEmpCriteria,1,100)    
    deptEmps    
}
) // end setGetterRowsFromOuterPersistence


}

  
val deptEmpsThreadLocal : ThreadLocal[Map[GroupKey, GroupValues]]  = addThreadLocalVal()

private def mapDeptEmpData( deptEmp : IRowValueGetter) : scala.collection.mutable.Map[GroupKey, GroupValues] ={
    var result :scala.collection.mutable.Map[GroupKey, GroupValues] = deptEmpsThreadLocal.get()
    var deptEmps: List[RowMap]  = null
    if (result != null){
         return result        
    }
    val empNo:String = deptEmp.getColumn("emp_no")    
    var deptEmpCriteria :DeptEmpCriteria = new DeptEmpCriteria()    
    deptEmpCriteria.createCriteria().andEmpNoEqualTo(empNo)
    deptEmpCriteria.addSelCol(DeptEmpColEnum.DEPT_NO ,DeptEmpColEnum.FROM_DATE ,DeptEmpColEnum.TO_DATE ,DeptEmpColEnum.EMP_NO)
    deptEmps = this.employeesDAOFacade.getDeptEmpDAO().selectColsByExample(deptEmpCriteria,1,100)
    result = scala.collection.mutable.Map[GroupKey, GroupValues]()
    var vals : Option[GroupValues] = null
    var groupKey: GroupKey = null
    for ( ( r:RowMap) <- deptEmps.asScala){
        groupKey = GroupKey.createCacheKey("emp_no",r.getColumn("emp_no"))
        vals = result.get(groupKey)        
        if (vals.isEmpty) {        
          result +=(groupKey.clone() -> new GroupValues(r))         
        }else{        
         vals.get.addVal(r)        
        }        
    }
    deptEmpsThreadLocal.set(result);    
    return result;    
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

private var employeesDAOFacade : IEmployeesDAOFacade = _

@Autowired
def setEmployeesDAOFacade(employeesDAOFacade: IEmployeesDAOFacade): Unit = {
this.employeesDAOFacade = employeesDAOFacade
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
        case "dept_emp" => getDeptEmpPK(row)
        case _=> throw new IllegalArgumentException("table name:"+ row.getTableName()+" is illegal");
  }
resultPK
}

protected override def getTableFocuse() : java.util.Set[String] = {
  if(_tabsFocuse == null){
     _tabsFocuse = com.google.common.collect.Sets.newHashSet( "dept_emp");
  }
  _tabsFocuse
}

    private def getDeptEmpPK(row : ITable) : DefaultPk = {

  val columnMeta : AliasList = tabColumnMetaMap.get("dept_emp");
  if (columnMeta == null) {
     throw new IllegalStateException("tableName: dept_emp is not exist in tabColumnMetaMap");
  }
      return new CompositePK(row.getColumn("emp_no")  );
    } // END getDeptEmpPK
    // globalScripts


}
