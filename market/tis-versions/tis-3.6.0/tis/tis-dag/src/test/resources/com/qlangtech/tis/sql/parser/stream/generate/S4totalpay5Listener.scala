package com.qlangtech.tis.realtime.transfer.search4totalpay5

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

import com.qlangtech.tis.realtime.order.pojo.TotalpayinfoColEnum
import com.qlangtech.tis.realtime.order.pojo.TotalpayinfoCriteria
import com.qlangtech.tis.realtime.order.pojo.Totalpayinfo
import com.qlangtech.tis.realtime.order.pojo.InstancedetailColEnum
import com.qlangtech.tis.realtime.order.pojo.InstancedetailCriteria
import com.qlangtech.tis.realtime.order.pojo.Instancedetail
import com.qlangtech.tis.realtime.order.pojo.OrderdetailColEnum
import com.qlangtech.tis.realtime.order.pojo.OrderdetailCriteria
import com.qlangtech.tis.realtime.order.pojo.Orderdetail

import com.qlangtech.tis.realtime.order.dao.IOrder2DAOFacade
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
import S4totalpay5Listener._

import com.qlangtech.tis.extension.TISExtension;

import scala.collection.JavaConverters._
import scala.util.control.Breaks._

object S4totalpay5Listener {

private val log: Logger = LoggerFactory.getLogger(classOf[S4totalpay5Listener])

val PK_TOTALPAY_ID: String = "totalpay_id"

//private val NULL_PK: NullCompositePK = new NullCompositePK()

private val payinfosThreadLocal: ThreadLocal[Map[GroupKey, GroupValues]] = addThreadLocalVal()

private val instancedetailsThreadLocal: ThreadLocal[Map[GroupKey, GroupValues]] = addThreadLocalVal()

private val specialfeesThreadLocal: ThreadLocal[Map[GroupKey, GroupValues]] = addThreadLocalVal()

private val NUM_CONSUME_NUM_5: Int = 5

private val ENTITY_ID: String = "entity_id"

}

@TISExtension(name = "default", ordinal = 0)
class S4totalpay5Listener extends BasicRuleDrivenListener {

private var _tabsFocuse : java.util.Set[String] = _

override def createProcessRate(): RateLimiter = RateLimiter.create(600)

protected override def processColsMeta(builder: BuilderList): Unit = {
  val totalpayinfoBuilder:AliasList.Builder = builder.add("totalpayinfo")

TotalpayinfoColEnum.getPKs().forEach((r) =>{
    totalpayinfoBuilder.add(r.getName().PK())
}
)
totalpayinfoBuilder.add( // 
("result_amount")
,("recieve_amount")
,("totalpay_id")
,("modify_time").notCopy().timestampVer() //gencode9 
);
totalpayinfoBuilder.addParentTabRef(EntityName.parse("order.orderdetail"),Lists.newArrayList(("totalpay_id","totalpay_id")))
totalpayinfoBuilder.setGetterRowsFromOuterPersistence(/*gencode5*/
 (rowTabName, rvals, pk ) =>{
     null    
}
) // end setGetterRowsFromOuterPersistence

val instancedetailBuilder:AliasList.Builder = builder.add("instancedetail")

InstancedetailColEnum.getPKs().forEach((r) =>{
    instancedetailBuilder.add(r.getName().PK())
}
)
instancedetailBuilder.add( // 
("batch_msg", "customer_ids").t((row, fieldValue) => {
    val instancedetails:Map[GroupKey /*order_id*/, GroupValues]  = mapInstancedetailData(row)

    var result:Any = null
    breakable {
    for ((k:GroupKey, v:GroupValues)  <- instancedetails){
        result = concat_ws(",",collect_set(v, (r) => { 
getArrayIndexProp(split(r.getColumn("batch_msg"),"\\|"),3)}))
        break

    }
    } //end breakable
     result //return

})/*end .t()*/
,("order_id").notCopy()  // FK or primay key
,("modify_time").notCopy().timestampVer() //gencode9 
);
instancedetailBuilder.addParentTabRef(EntityName.parse("order.orderdetail"),Lists.newArrayList(("order_id","order_id")))
instancedetailBuilder.setGetterRowsFromOuterPersistence(/*gencode5*/
 (rowTabName, rvals, pk ) =>{
     null    
}
) // end setGetterRowsFromOuterPersistence

val orderdetailBuilder:AliasList.Builder = builder.add("orderdetail").setPrimaryTableOfIndex()

OrderdetailColEnum.getPKs().forEach((r) =>{
    orderdetailBuilder.add(r.getName().PK())
}
)
orderdetailBuilder.add( // 
("end_time")
,("open_time")
,("totalpay_id").notCopy()  // FK or primay key
,("order_id").notCopy()  // FK or primay key
,("modify_time").notCopy().timestampVer() //gencode9 
);
orderdetailBuilder.addChildTabRef(EntityName.parse("order.totalpayinfo"),Lists.newArrayList(("totalpay_id","totalpay_id")))
orderdetailBuilder.addChildTabRef(EntityName.parse("order.instancedetail"),Lists.newArrayList(("order_id","order_id")))
orderdetailBuilder.setGetterRowsFromOuterPersistence(/*gencode5*/
 (rowTabName, rvals, pk ) =>{
    var orderdetails: List[RowMap]  = null    
    var orderdetailCriteria :OrderdetailCriteria = new OrderdetailCriteria()    
    orderdetailCriteria.createCriteria()    .andOrderIdEqualTo(pk.getValue())
    orderdetailCriteria.addSelCol(OrderdetailColEnum.END_TIME ,OrderdetailColEnum.OPEN_TIME ,OrderdetailColEnum.TOTALPAY_ID ,OrderdetailColEnum.ORDER_ID)    
    orderdetails = this.orderDAOFacade.getOrderdetailDAO().selectColsByExample(orderdetailCriteria,1,100)    
    orderdetails    
}
) // end setGetterRowsFromOuterPersistence


}

    
val instancedetailsThreadLocal : ThreadLocal[Map[GroupKey, GroupValues]]  = addThreadLocalVal()

private def mapInstancedetailData( instancedetail : IRowValueGetter) : scala.collection.mutable.Map[GroupKey, GroupValues] ={
    var result :scala.collection.mutable.Map[GroupKey, GroupValues] = instancedetailsThreadLocal.get()
    var instancedetails: List[RowMap]  = null
    if (result != null){
         return result        
    }
    val orderId:String = instancedetail.getColumn("order_id")    
    var instancedetailCriteria :InstancedetailCriteria = new InstancedetailCriteria()    
    instancedetailCriteria.createCriteria().andOrderIdEqualTo(orderId)
    instancedetailCriteria.addSelCol(InstancedetailColEnum.BATCH_MSG ,InstancedetailColEnum.ORDER_ID)
    instancedetails = this.orderDAOFacade.getInstancedetailDAO().selectColsByExample(instancedetailCriteria,1,100)
    result = scala.collection.mutable.Map[GroupKey, GroupValues]()
    var vals : Option[GroupValues] = null
    var groupKey: GroupKey = null
    for ( ( r:RowMap) <- instancedetails.asScala){
        groupKey = GroupKey.createCacheKey("order_id",r.getColumn("order_id"))
        vals = result.get(groupKey)        
        if (vals.isEmpty) {        
          result +=(groupKey.clone() -> new GroupValues(r))         
        }else{        
         vals.get.addVal(r)        
        }        
    }
    instancedetailsThreadLocal.set(result);    
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

private var order2DAOFacade : IOrder2DAOFacade = _

@Autowired
def setOrder2DAOFacade(order2DAOFacade: IOrder2DAOFacade): Unit = {
this.order2DAOFacade = order2DAOFacade
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
        case "totalpayinfo" => getTotalpayinfoPK(row)
            case "instancedetail" => getInstancedetailPK(row)
            case "orderdetail" => getOrderdetailPK(row)
        case _=> throw new IllegalArgumentException("table name:"+ row.getTableName()+" is illegal");
  }
resultPK
}

protected override def getTableFocuse() : java.util.Set[String] = {
  if(_tabsFocuse == null){
     _tabsFocuse = com.google.common.collect.Sets.newHashSet( "totalpayinfo","instancedetail","orderdetail");
  }
  _tabsFocuse
}

    private def getTotalpayinfoPK(row : ITable) : DefaultPk = {

  val columnMeta : AliasList = tabColumnMetaMap.get("totalpayinfo");
  if (columnMeta == null) {
     throw new IllegalStateException("tableName: totalpayinfo is not exist in tabColumnMetaMap");
  }
            
                
                // kkk  totalpayinfo hasParent true
// llll orderdetail
        
        
val totalpayId = row.getColumn("totalpay_id")
val orderdetail: RowMap  = this.queryOrderdetailByTotalpayinfo(totalpayId) 

val orderdetailMeta : AliasList = tabColumnMetaMap.get("orderdetail");
if(orderdetail != null){
    return  new CompositePK(orderdetailMeta.getColMeta("order_id").getVal(orderdetail,true) )/*gencode4*/
}
 null;
        } // END getTotalpayinfoPK
        private def getInstancedetailPK(row : ITable) : DefaultPk = {

  val columnMeta : AliasList = tabColumnMetaMap.get("instancedetail");
  if (columnMeta == null) {
     throw new IllegalStateException("tableName: instancedetail is not exist in tabColumnMetaMap");
  }
            
                
                // kkk  instancedetail hasParent true
// llll orderdetail
        
        return  new CompositePK(columnMeta.getColMeta("order_id").getVal(row,true) )/*gencode2*/;
        } // END getInstancedetailPK
        private def getOrderdetailPK(row : ITable) : DefaultPk = {

  val columnMeta : AliasList = tabColumnMetaMap.get("orderdetail");
  if (columnMeta == null) {
     throw new IllegalStateException("tableName: orderdetail is not exist in tabColumnMetaMap");
  }
      return new CompositePK(row.getColumn("order_id")  );
    } // END getOrderdetailPK
    // globalScripts

private def queryOrderdetailByTotalpayinfo(totalpayId : String) : RowMap ={
    var orderdetails: List[RowMap]  = null
    var orderdetailCriteria :OrderdetailCriteria = new OrderdetailCriteria()
    orderdetailCriteria.createCriteria().andTotalpayIdEqualTo(totalpayId)
    orderdetailCriteria.addSelCol(OrderdetailColEnum.TOTALPAY_ID)
    orderdetails = this.orderDAOFacade.getOrderdetailDAO().selectColsByExample(orderdetailCriteria,1,100)
    for ( ( r:RowMap) <- orderdetails.asScala){
        return r
    }
    null
}



}
