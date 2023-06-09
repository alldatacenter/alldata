package com.qlangtech.tis.realtime.transfer.search4totalpay

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

import com.qlangtech.tis.realtime.member.pojo.CardColEnum
import com.qlangtech.tis.realtime.member.pojo.CardCriteria
import com.qlangtech.tis.realtime.member.pojo.Card
import com.qlangtech.tis.realtime.order.pojo.ServicebillinfoColEnum
import com.qlangtech.tis.realtime.order.pojo.ServicebillinfoCriteria
import com.qlangtech.tis.realtime.order.pojo.Servicebillinfo
import com.qlangtech.tis.realtime.order.pojo.TakeoutOrderExtraColEnum
import com.qlangtech.tis.realtime.order.pojo.TakeoutOrderExtraCriteria
import com.qlangtech.tis.realtime.order.pojo.TakeoutOrderExtra
import com.qlangtech.tis.realtime.order.pojo.TotalpayinfoColEnum
import com.qlangtech.tis.realtime.order.pojo.TotalpayinfoCriteria
import com.qlangtech.tis.realtime.order.pojo.Totalpayinfo
import com.qlangtech.tis.realtime.member.pojo.CustomerColEnum
import com.qlangtech.tis.realtime.member.pojo.CustomerCriteria
import com.qlangtech.tis.realtime.member.pojo.Customer
import com.qlangtech.tis.realtime.order.pojo.OrderdetailColEnum
import com.qlangtech.tis.realtime.order.pojo.OrderdetailCriteria
import com.qlangtech.tis.realtime.order.pojo.Orderdetail
import com.qlangtech.tis.realtime.order.pojo.SpecialfeeColEnum
import com.qlangtech.tis.realtime.order.pojo.SpecialfeeCriteria
import com.qlangtech.tis.realtime.order.pojo.Specialfee
import com.qlangtech.tis.realtime.cardcenter.pojo.EntExpenseColEnum
import com.qlangtech.tis.realtime.cardcenter.pojo.EntExpenseCriteria
import com.qlangtech.tis.realtime.cardcenter.pojo.EntExpense
import com.qlangtech.tis.realtime.order.pojo.PayinfoColEnum
import com.qlangtech.tis.realtime.order.pojo.PayinfoCriteria
import com.qlangtech.tis.realtime.order.pojo.Payinfo
import com.qlangtech.tis.realtime.order.pojo.OrderBillColEnum
import com.qlangtech.tis.realtime.order.pojo.OrderBillCriteria
import com.qlangtech.tis.realtime.order.pojo.OrderBill
import com.qlangtech.tis.realtime.order.pojo.InstancedetailColEnum
import com.qlangtech.tis.realtime.order.pojo.InstancedetailCriteria
import com.qlangtech.tis.realtime.order.pojo.Instancedetail
import com.qlangtech.tis.realtime.cardcenter.pojo.EntExpenseOrderColEnum
import com.qlangtech.tis.realtime.cardcenter.pojo.EntExpenseOrderCriteria
import com.qlangtech.tis.realtime.cardcenter.pojo.EntExpenseOrder
import com.qlangtech.tis.realtime.shop.pojo.MallShopColEnum
import com.qlangtech.tis.realtime.shop.pojo.MallShopCriteria
import com.qlangtech.tis.realtime.shop.pojo.MallShop

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
import S4totalpayListener._

import com.qlangtech.tis.extension.TISExtension;

import scala.collection.JavaConverters._
import scala.util.control.Breaks._

object S4totalpayListener {

private val log: Logger = LoggerFactory.getLogger(classOf[S4totalpayListener])

val PK_TOTALPAY_ID: String = "totalpay_id"

//private val NULL_PK: NullCompositePK = new NullCompositePK()

private val payinfosThreadLocal: ThreadLocal[Map[GroupKey, GroupValues]] = addThreadLocalVal()

private val instancedetailsThreadLocal: ThreadLocal[Map[GroupKey, GroupValues]] = addThreadLocalVal()

private val specialfeesThreadLocal: ThreadLocal[Map[GroupKey, GroupValues]] = addThreadLocalVal()

private val NUM_CONSUME_NUM_5: Int = 5

private val ENTITY_ID: String = "entity_id"

}

@TISExtension(name = "default", ordinal = 0)
class S4totalpayListener extends BasicRuleDrivenListener {

private var _tabsFocuse : java.util.Set[String] = _

override def createProcessRate(): RateLimiter = RateLimiter.create(600)

protected override def processColsMeta(builder: BuilderList): Unit = {
  val cardBuilder:AliasList.Builder = builder.add("card").setIgnoreIncrTrigger()

CardColEnum.getPKs().forEach((r) =>{
    cardBuilder.add(r.getName().PK())
}
)
cardBuilder.add( // 
("id", "is_enterprise_card").c((row) => {
    //===================================
    putMediaResult("is_enterprise_card", //
    caseIfFunc(0,
    new Case(rlike(row.getColumn("id"),"^E_"),1))
    )
    //===================================
    //===================================
    
    caseIfFunc(0,
    new Case((getMediaResult("is_enterprise_card",row)>0) || (getMediaResult("is_enterprise_card_pay",row)>0),1))
    //===================================
})/*end .t()*/
,("customer_id", "card_customer_id")
,("inner_code", "card_inner_code")
,("code", "card_code")
,("entity_id").notCopy()  // FK or primay key
);
cardBuilder.addParentTabRef(EntityName.parse("member.customer"),Lists.newArrayList(("entity_id","entity_id"),("id","customer_id")))
cardBuilder.addChildTabRef(EntityName.parse("order.totalpayinfo"),Lists.newArrayList(("entity_id","entity_id"),("id","card_id")))
cardBuilder.setGetterRowsFromOuterPersistence(/*gencode5*/
 (rowTabName, rvals, pk ) =>{
    var cards: List[RowMap]  = null    
    var cardCriteria :CardCriteria = new CardCriteria()    
    cardCriteria.addSelCol(CardColEnum.CODE ,CardColEnum.INNER_CODE ,CardColEnum.ID ,CardColEnum.CUSTOMER_ID ,CardColEnum.ENTITY_ID)    
    rowTabName match {

        case "totalpayinfo" =>{
            cardCriteria.createCriteria()            .andEntityIdEqualTo(rvals.getColumn("entity_id")).andIdEqualTo(rvals.getColumn("card_id"))
            cards = this.memberDAOFacade.getCardDAO().selectColsByExample(cardCriteria,1,100)            
            cards            
        }
        case unexpected => null        
    }

}
) // end setGetterRowsFromOuterPersistence

val servicebillinfoBuilder:AliasList.Builder = builder.add("servicebillinfo")

ServicebillinfoColEnum.getPKs().forEach((r) =>{
    servicebillinfoBuilder.add(r.getName().PK())
}
)
servicebillinfoBuilder.add( // 
("final_amount", "service_bill_final_amount")
,("servicebill_id").notCopy()  // FK or primay key
,("entity_id").notCopy()  // FK or primay key
,("modify_time").notCopy().timestampVer() //gencode9 
);
servicebillinfoBuilder.addParentTabRef(EntityName.parse("order.totalpayinfo"),Lists.newArrayList(("totalpay_id","servicebill_id"),("entity_id","entity_id")))
servicebillinfoBuilder.setGetterRowsFromOuterPersistence(/*gencode5*/
 (rowTabName, rvals, pk ) =>{
     null    
}
) // end setGetterRowsFromOuterPersistence

val takeoutOrderExtraBuilder:AliasList.Builder = builder.add("takeout_order_extra").setIgnoreIncrTrigger()

TakeoutOrderExtraColEnum.getPKs().forEach((r) =>{
    takeoutOrderExtraBuilder.add(r.getName().PK())
}
)
takeoutOrderExtraBuilder.add( // 
("courier_phone")
,("out_id")
,("entity_id").notCopy()  // FK or primay key
,("order_id").notCopy()  // FK or primay key
);
takeoutOrderExtraBuilder.addParentTabRef(EntityName.parse("order.orderdetail"),Lists.newArrayList(("order_id","order_id"),("entity_id","entity_id")))
takeoutOrderExtraBuilder.setGetterRowsFromOuterPersistence(/*gencode5*/
 (rowTabName, rvals, pk ) =>{
     null    
}
) // end setGetterRowsFromOuterPersistence

val totalpayinfoBuilder:AliasList.Builder = builder.add("totalpayinfo").setPrimaryTableOfIndex()

TotalpayinfoColEnum.getPKs().forEach((r) =>{
    totalpayinfoBuilder.add(r.getName().PK())
}
)
totalpayinfoBuilder.add( // 
("discount_amount")
,("op_time")
,("operator")
,("is_servicefee_ratio")
,("is_hide")
,("op_user_id")
,("entity_id")
,("card_id")
,("is_full_ratio")
,("is_valid")
,("is_minconsume_ratio")
,("over_status")
,("coupon_discount")
,("card")
,("status")
,("curr_date")
,("recieve_amount")
,("modify_time").timestampVer()
,("totalpay_id")
,("load_time")
,("invoice_memo")
,("operate_date")
,("outfee")
,("source_amount")
,("card_entity_id")
,("invoice_code")
,("discount_plan_id")
,("last_ver")
,("result_amount")
,("invoice")
,("ratio")
);
totalpayinfoBuilder.addParentTabRef(EntityName.parse("member.card"),Lists.newArrayList(("entity_id","entity_id"),("id","card_id")))
totalpayinfoBuilder.addParentTabRef(EntityName.parse("shop.mall_shop"),Lists.newArrayList(("shop_entity_id","entity_id")))
totalpayinfoBuilder.addChildTabRef(EntityName.parse("order.orderdetail"),Lists.newArrayList(("totalpay_id","totalpay_id"),("entity_id","entity_id")))
totalpayinfoBuilder.addChildTabRef(EntityName.parse("cardcenter.ent_expense_order"),Lists.newArrayList(("totalpay_id","totalpay_id"),("entity_id","entity_id")))
totalpayinfoBuilder.addChildTabRef(EntityName.parse("order.payinfo"),Lists.newArrayList(("totalpay_id","totalpay_id"),("entity_id","entity_id")))
totalpayinfoBuilder.addChildTabRef(EntityName.parse("order.specialfee"),Lists.newArrayList(("totalpay_id","totalpay_id"),("entity_id","entity_id")))
totalpayinfoBuilder.addChildTabRef(EntityName.parse("order.servicebillinfo"),Lists.newArrayList(("totalpay_id","servicebill_id"),("entity_id","entity_id")))
totalpayinfoBuilder.setGetterRowsFromOuterPersistence(/*gencode5*/
 (rowTabName, rvals, pk ) =>{
    var totalpayinfos: List[RowMap]  = null    
    var totalpayinfoCriteria :TotalpayinfoCriteria = new TotalpayinfoCriteria()    
    val entityId = pk.getRouterVal("entity_id")    
    totalpayinfoCriteria.createCriteria()    .andTotalpayIdEqualTo(pk.getValue()).andEntityIdEqualTo(entityId)
    totalpayinfoCriteria.addSelCol(TotalpayinfoColEnum.CURR_DATE ,TotalpayinfoColEnum.DISCOUNT_AMOUNT ,TotalpayinfoColEnum.RECIEVE_AMOUNT ,TotalpayinfoColEnum.MODIFY_TIME ,TotalpayinfoColEnum.OP_TIME ,TotalpayinfoColEnum.TOTALPAY_ID ,TotalpayinfoColEnum.OPERATOR ,TotalpayinfoColEnum.IS_SERVICEFEE_RATIO ,TotalpayinfoColEnum.LOAD_TIME ,TotalpayinfoColEnum.INVOICE_MEMO ,TotalpayinfoColEnum.IS_HIDE ,TotalpayinfoColEnum.OP_USER_ID ,TotalpayinfoColEnum.OPERATE_DATE ,TotalpayinfoColEnum.OUTFEE ,TotalpayinfoColEnum.SOURCE_AMOUNT ,TotalpayinfoColEnum.ENTITY_ID ,TotalpayinfoColEnum.CARD_ID ,TotalpayinfoColEnum.CARD_ENTITY_ID ,TotalpayinfoColEnum.INVOICE_CODE ,TotalpayinfoColEnum.DISCOUNT_PLAN_ID ,TotalpayinfoColEnum.LAST_VER ,TotalpayinfoColEnum.IS_FULL_RATIO ,TotalpayinfoColEnum.IS_VALID ,TotalpayinfoColEnum.IS_MINCONSUME_RATIO ,TotalpayinfoColEnum.RESULT_AMOUNT ,TotalpayinfoColEnum.OVER_STATUS ,TotalpayinfoColEnum.INVOICE ,TotalpayinfoColEnum.COUPON_DISCOUNT ,TotalpayinfoColEnum.CARD ,TotalpayinfoColEnum.STATUS ,TotalpayinfoColEnum.RATIO)    
    totalpayinfos = this.orderDAOFacade.getTotalpayinfoDAO().selectColsByExample(totalpayinfoCriteria,1,100)    
    totalpayinfos    
}
) // end setGetterRowsFromOuterPersistence

val customerBuilder:AliasList.Builder = builder.add("customer").setIgnoreIncrTrigger()

CustomerColEnum.getPKs().forEach((r) =>{
    customerBuilder.add(r.getName().PK())
}
)
customerBuilder.add( // 
("mobile", "card_customer_moble")
,("name", "card_customer_name")
,("phone", "card_customer_phone")
,("spell", "card_customer_spell")
,("id").notCopy()  // FK or primay key
,("entity_id").notCopy()  // FK or primay key
);
customerBuilder.addChildTabRef(EntityName.parse("member.card"),Lists.newArrayList(("entity_id","entity_id"),("id","customer_id")))
customerBuilder.setGetterRowsFromOuterPersistence(/*gencode5*/
 (rowTabName, rvals, pk ) =>{
    var customers: List[RowMap]  = null    
    var customerCriteria :CustomerCriteria = new CustomerCriteria()    
    customerCriteria.addSelCol(CustomerColEnum.SPELL ,CustomerColEnum.PHONE ,CustomerColEnum.MOBILE ,CustomerColEnum.NAME ,CustomerColEnum.ID ,CustomerColEnum.ENTITY_ID)    
    rowTabName match {

        case "card" =>{
            customerCriteria.createCriteria()            .andEntityIdEqualTo(rvals.getColumn("entity_id")).andIdEqualTo(rvals.getColumn("customer_id"))
            customers = this.memberDAOFacade.getCustomerDAO().selectColsByExample(customerCriteria,1,100)            
            customers            
        }
        case unexpected => null        
    }

}
) // end setGetterRowsFromOuterPersistence

val orderdetailBuilder:AliasList.Builder = builder.add("orderdetail")

OrderdetailColEnum.getPKs().forEach((r) =>{
    orderdetailBuilder.add(r.getName().PK())
}
)
orderdetailBuilder.add( // 
("open_time")
,("area_id")
,("people_count")
,("status", "order_status")
,("is_valid", "is_valido")
,("customerregister_id")
,("inner_code")
,("order_id")
,("mobile", "order_mobile")
,("code")
,("seat_id")
,("seat_code")
,("address", "order_address")
,("end_time")
,("order_from")
,("order_kind")
,("totalpay_id").notCopy()  // FK or primay key
,("entity_id").notCopy()  // FK or primay key
,("modify_time").notCopy().timestampVer() //gencode9 
);
orderdetailBuilder.addParentTabRef(EntityName.parse("order.totalpayinfo"),Lists.newArrayList(("totalpay_id","totalpay_id"),("entity_id","entity_id")))
orderdetailBuilder.addChildTabRef(EntityName.parse("order.instancedetail"),Lists.newArrayList(("order_id","order_id"),("entity_id","entity_id")))
orderdetailBuilder.addChildTabRef(EntityName.parse("order.order_bill"),Lists.newArrayList(("order_id","order_id"),("entity_id","entity_id")))
orderdetailBuilder.addChildTabRef(EntityName.parse("order.takeout_order_extra"),Lists.newArrayList(("order_id","order_id"),("entity_id","entity_id")))
orderdetailBuilder.setGetterRowsFromOuterPersistence(/*gencode5*/
 (rowTabName, rvals, pk ) =>{
    var orderdetails: List[RowMap]  = null    
    var orderdetailCriteria :OrderdetailCriteria = new OrderdetailCriteria()    
    orderdetailCriteria.addSelCol(OrderdetailColEnum.CODE ,OrderdetailColEnum.ADDRESS ,OrderdetailColEnum.SEAT_ID ,OrderdetailColEnum.MOBILE ,OrderdetailColEnum.END_TIME ,OrderdetailColEnum.OPEN_TIME ,OrderdetailColEnum.SEAT_CODE ,OrderdetailColEnum.AREA_ID ,OrderdetailColEnum.PEOPLE_COUNT ,OrderdetailColEnum.ORDER_FROM ,OrderdetailColEnum.ORDER_KIND ,OrderdetailColEnum.CUSTOMERREGISTER_ID ,OrderdetailColEnum.IS_VALID ,OrderdetailColEnum.INNER_CODE ,OrderdetailColEnum.ORDER_ID ,OrderdetailColEnum.STATUS ,OrderdetailColEnum.TOTALPAY_ID ,OrderdetailColEnum.ENTITY_ID)    
    rowTabName match {

        case "instancedetail" =>{
            orderdetailCriteria.createCriteria()            .andOrderIdEqualTo(rvals.getColumn("order_id")).andEntityIdEqualTo(rvals.getColumn("entity_id"))
            orderdetails = this.orderDAOFacade.getOrderdetailDAO().selectColsByExample(orderdetailCriteria,1,100)            
            orderdetails            
        }

        case "order_bill" =>{
            orderdetailCriteria.createCriteria()            .andOrderIdEqualTo(rvals.getColumn("order_id")).andEntityIdEqualTo(rvals.getColumn("entity_id"))
            orderdetails = this.orderDAOFacade.getOrderdetailDAO().selectColsByExample(orderdetailCriteria,1,100)            
            orderdetails            
        }

        case "takeout_order_extra" =>{
            orderdetailCriteria.createCriteria()            .andOrderIdEqualTo(rvals.getColumn("order_id")).andEntityIdEqualTo(rvals.getColumn("entity_id"))
            orderdetails = this.orderDAOFacade.getOrderdetailDAO().selectColsByExample(orderdetailCriteria,1,100)            
            orderdetails            
        }
        case unexpected => null        
    }

}
) // end setGetterRowsFromOuterPersistence

val specialfeeBuilder:AliasList.Builder = builder.add("specialfee")

SpecialfeeColEnum.getPKs().forEach((r) =>{
    specialfeeBuilder.add(r.getName().PK())
}
)
specialfeeBuilder.add( // 
("kind", "special_fee_summary").t((row, fieldValue) => {
    val specialfees:Map[GroupKey /*entity_id,totalpay_id*/, GroupValues]  = mapSpecialfeeData(row)

    var result:Any = null
    breakable {
    for ((k:GroupKey, v:GroupValues)  <- specialfees){
        result = concat_ws(";",collect_list(v, (r) => { 
concat_ws("_",typeCast("string",r.getColumn("kind")),typeCast("string",r.getColumn("fee")))}))
        break

    }
    } //end breakable
     result //return

})/*end .t()*/
,("fee", "special_fee_summary").notCopy()
,("totalpay_id").notCopy()  // FK or primay key
,("entity_id").notCopy()  // FK or primay key
,("modify_time").notCopy().timestampVer() //gencode9 
);
specialfeeBuilder.addParentTabRef(EntityName.parse("order.totalpayinfo"),Lists.newArrayList(("totalpay_id","totalpay_id"),("entity_id","entity_id")))
specialfeeBuilder.setGetterRowsFromOuterPersistence(/*gencode5*/
 (rowTabName, rvals, pk ) =>{
     null    
}
) // end setGetterRowsFromOuterPersistence

val entExpenseBuilder:AliasList.Builder = builder.add("ent_expense")

EntExpenseColEnum.getPKs().forEach((r) =>{
    entExpenseBuilder.add(r.getName().PK())
}
)
entExpenseBuilder.add( // 
("expense_status")
,("enterprise_id").notCopy()  // FK or primay key
,("expense_id").notCopy()  // FK or primay key
,("op_time").notCopy().timestampVer() //gencode9 
);
entExpenseBuilder.addChildTabRef(EntityName.parse("cardcenter.ent_expense_order"),Lists.newArrayList(("enterprise_id","enterprise_id"),("expense_id","expense_id")))
entExpenseBuilder.setGetterRowsFromOuterPersistence(/*gencode5*/
 (rowTabName, rvals, pk ) =>{
    var entExpenses: List[RowMap]  = null    
    var entExpenseCriteria :EntExpenseCriteria = new EntExpenseCriteria()    
    entExpenseCriteria.addSelCol(EntExpenseColEnum.EXPENSE_STATUS ,EntExpenseColEnum.ENTERPRISE_ID ,EntExpenseColEnum.EXPENSE_ID)    
    rowTabName match {

        case "ent_expense_order" =>{
            entExpenseCriteria.createCriteria()            .andEnterpriseIdEqualTo(rvals.getColumn("enterprise_id")).andExpenseIdEqualTo(rvals.getColumn("expense_id"))
            entExpenses = this.cardcenterDAOFacade.getEntExpenseDAO().selectColsByExample(entExpenseCriteria,1,100)            
            entExpenses            
        }
        case unexpected => null        
    }

}
) // end setGetterRowsFromOuterPersistence

val payinfoBuilder:AliasList.Builder = builder.add("payinfo")

PayinfoColEnum.getPKs().forEach((r) =>{
    payinfoBuilder.add(r.getName().PK())
}
)
payinfoBuilder.add( // 
("type", "is_enterprise_card").c((row) => {
    val payinfos:Map[GroupKey /*totalpay_id,kindpay_id*/, GroupValues]  = mapPayinfoData(row)

    
    for ((k:GroupKey, v:GroupValues)  <- payinfos){
        v.putMediaData( // 
        "is_enterprise_card_pay_inner" // 
        , //
sum(v, (r) => { 
        caseIfFunc(0,
        new Case((r.getColumn("type")== String.valueOf(103)),1))})
        ) // end putMediaData

    }
    
val innertabAas: Map[GroupKey /* totalpay_id */, GroupValues]  = reduceData(payinfos, "totalpay_id")
    breakable {
    for ((k:GroupKey, v:GroupValues)  <- innertabAas){
        putMediaResult("is_enterprise_card_pay", //
        caseIfFunc(0,
        new Case((sum(v, (r) => { 
r.getColumn("is_enterprise_card_pay_inner")})>0),1))
        ) // end putMediaResult
        break
    }
    } //end breakable
    //===================================
    
    caseIfFunc(0,
    new Case((getMediaResult("is_enterprise_card",row)>0) || (getMediaResult("is_enterprise_card_pay",row)>0),1))
    //===================================
})/*end .t()*/
,("ext", "pay_customer_ids").t((row, fieldValue) => {
    val payinfos:Map[GroupKey /*totalpay_id,kindpay_id*/, GroupValues]  = mapPayinfoData(row)

    
    for ((k:GroupKey, v:GroupValues)  <- payinfos){
        v.putMediaData( // 
        "pay_customer_ids" // 
        , //
concat_ws(",",collect_list(v, (r) => { 
get_json_object(r.getColumn("ext"),"$.customerRegisterId")}))
        ) // end putMediaData

    }
    
val innertabAas: Map[GroupKey /* totalpay_id */, GroupValues]  = reduceData(payinfos, "totalpay_id")
    var result:Any = null
    breakable {
    for ((k:GroupKey, v:GroupValues)  <- innertabAas){
        result = concat_ws(",",collect_list(v, (r) => { 
r.getColumn("pay_customer_ids")}))
        break

    }
    } //end breakable
     result //return

})/*end .t()*/
,("coupon_cost", "kindpay").t((row, fieldValue) => {
    val payinfos:Map[GroupKey /*totalpay_id,kindpay_id*/, GroupValues]  = mapPayinfoData(row)

    
    for ((k:GroupKey, v:GroupValues)  <- payinfos){
        v.putMediaData( // 
        "aakindpay" // 
        , //
concat_ws("_",defaultVal(k.getKeyVal("kindpay_id"),"0"),typeCast("string",count(v, (r) => { 
1})),typeCast("string",round(sum(v, (r) => { 
r.getColumn("fee")}),2)),typeCast("string",sum(v, (r) => { 
((defaultDoubleVal(r.getColumn("coupon_fee"),0)-defaultDoubleVal(r.getColumn("coupon_cost"),0))*defaultDoubleVal(r.getColumn("coupon_num"),0))})),defaultVal(typeCast("string",min(v, (r) => { 
r.getColumn("pay_id")})),"0"))
        ) // end putMediaData

    }
    
val innertabAas: Map[GroupKey /* totalpay_id */, GroupValues]  = reduceData(payinfos, "totalpay_id")
    var result:Any = null
    breakable {
    for ((k:GroupKey, v:GroupValues)  <- innertabAas){
        result = concat_ws(";",collect_list(v, (r) => { 
r.getColumn("aakindpay")}))
        break

    }
    } //end breakable
     result //return

})/*end .t()*/
,("kindpay_id", "kindpay").notCopy()
,("coupon_fee", "kindpay").notCopy()
,("coupon_num", "kindpay").notCopy()
,("fee", "kindpay").notCopy()
,("pay_id", "kindpay").notCopy()
,("fee", "all_pay_fee").t((row, fieldValue) => {
    val payinfos:Map[GroupKey /*totalpay_id,kindpay_id*/, GroupValues]  = mapPayinfoData(row)

    
    for ((k:GroupKey, v:GroupValues)  <- payinfos){
        v.putMediaData( // 
        "fee" // 
        , //
sum(v, (r) => { 
r.getColumn("fee")})
        ) // end putMediaData

    }
    
val innertabAas: Map[GroupKey /* totalpay_id */, GroupValues]  = reduceData(payinfos, "totalpay_id")
    var result:Any = null
    breakable {
    for ((k:GroupKey, v:GroupValues)  <- innertabAas){
        result = sum(v, (r) => { 
r.getColumn("fee")})
        break

    }
    } //end breakable
     result //return

})/*end .t()*/
,("totalpay_id").notCopy()  // FK or primay key
,("entity_id").notCopy()  // FK or primay key
,("modify_time").notCopy().timestampVer() //gencode9 
);
payinfoBuilder.addParentTabRef(EntityName.parse("order.totalpayinfo"),Lists.newArrayList(("totalpay_id","totalpay_id"),("entity_id","entity_id")))
payinfoBuilder.setGetterRowsFromOuterPersistence(/*gencode5*/
 (rowTabName, rvals, pk ) =>{
     null    
}
) // end setGetterRowsFromOuterPersistence

val orderBillBuilder:AliasList.Builder = builder.add("order_bill")

OrderBillColEnum.getPKs().forEach((r) =>{
    orderBillBuilder.add(r.getName().PK())
}
)
orderBillBuilder.add( // 
("final_amount", "bill_info_final_amount")
,("entity_id").notCopy()  // FK or primay key
,("order_id").notCopy()  // FK or primay key
,("op_time").notCopy().timestampVer() //gencode9 
);
orderBillBuilder.addParentTabRef(EntityName.parse("order.orderdetail"),Lists.newArrayList(("order_id","order_id"),("entity_id","entity_id")))
orderBillBuilder.setGetterRowsFromOuterPersistence(/*gencode5*/
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
("draw_status", "has_fetch").t((row, fieldValue) => {
    val instancedetails:Map[GroupKey /*entity_id,order_id*/, GroupValues]  = mapInstancedetailData(row)

    
    for ((k:GroupKey, v:GroupValues)  <- instancedetails){
        v.putMediaData( // 
        "has_fetch" // 
        , //
        caseIfFunc(0,
        new Case((sum(v, (r) => { 
op_and(r.getColumn("draw_status"),8)})>0),1))
        ) // end putMediaData

    }
    
    //===================================
    
    var result:Any = null
    breakable {
    for((k:GroupKey, v:GroupValues) <- instancedetails){
result = defaultVal(
        v.getMediaProp("has_fetch"),0)
        break
    }
     }
    result
    //===================================
})/*end .t()*/
,("batch_msg", "customer_ids").t((row, fieldValue) => {
    val instancedetails:Map[GroupKey /*entity_id,order_id*/, GroupValues]  = mapInstancedetailData(row)

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
,("entity_id").notCopy()  // FK or primay key
,("order_id").notCopy()  // FK or primay key
,("modify_time").notCopy().timestampVer() //gencode9 
);
instancedetailBuilder.addParentTabRef(EntityName.parse("order.orderdetail"),Lists.newArrayList(("order_id","order_id"),("entity_id","entity_id")))
instancedetailBuilder.setGetterRowsFromOuterPersistence(/*gencode5*/
 (rowTabName, rvals, pk ) =>{
     null    
}
) // end setGetterRowsFromOuterPersistence

val entExpenseOrderBuilder:AliasList.Builder = builder.add("ent_expense_order")

EntExpenseOrderColEnum.getPKs().forEach((r) =>{
    entExpenseOrderBuilder.add(r.getName().PK())
}
)
entExpenseOrderBuilder.add( // 
("create_time", "expense_create_time")
,("totalpay_id").notCopy()  // FK or primay key
,("enterprise_id").notCopy()  // FK or primay key
,("entity_id").notCopy()  // FK or primay key
,("expense_id").notCopy()  // FK or primay key
,("op_time").notCopy().timestampVer() //gencode9 
);
entExpenseOrderBuilder.addParentTabRef(EntityName.parse("cardcenter.ent_expense"),Lists.newArrayList(("enterprise_id","enterprise_id"),("expense_id","expense_id")))
entExpenseOrderBuilder.addParentTabRef(EntityName.parse("order.totalpayinfo"),Lists.newArrayList(("totalpay_id","totalpay_id"),("entity_id","entity_id")))
entExpenseOrderBuilder.setGetterRowsFromOuterPersistence(/*gencode5*/
 (rowTabName, rvals, pk ) =>{
     null    
}
) // end setGetterRowsFromOuterPersistence

val mallShopBuilder:AliasList.Builder = builder.add("mall_shop").setIgnoreIncrTrigger()

MallShopColEnum.getPKs().forEach((r) =>{
    mallShopBuilder.add(r.getName().PK())
}
)
mallShopBuilder.add( // 
("mall_entity_id", "mall_id")
,("shop_entity_id").notCopy()  // FK or primay key
);
mallShopBuilder.addChildTabRef(EntityName.parse("order.totalpayinfo"),Lists.newArrayList(("shop_entity_id","entity_id")))
mallShopBuilder.setGetterRowsFromOuterPersistence(/*gencode5*/
 (rowTabName, rvals, pk ) =>{
    var mallShops: List[RowMap]  = null    
    var mallShopCriteria :MallShopCriteria = new MallShopCriteria()    
    mallShopCriteria.addSelCol(MallShopColEnum.MALL_ENTITY_ID ,MallShopColEnum.SHOP_ENTITY_ID)    
    rowTabName match {

        case "totalpayinfo" =>{
            mallShopCriteria.createCriteria()            .andShopEntityIdEqualTo(rvals.getColumn("entity_id"))
            mallShops = this.shopDAOFacade.getMallShopDAO().selectColsByExample(mallShopCriteria,1,100)            
            mallShops            
        }
        case unexpected => null        
    }

}
) // end setGetterRowsFromOuterPersistence


}

    
val payinfosThreadLocal : ThreadLocal[Map[GroupKey, GroupValues]]  = addThreadLocalVal()

private def mapPayinfoData( payinfo : IRowValueGetter) : scala.collection.mutable.Map[GroupKey, GroupValues] ={
    var result :scala.collection.mutable.Map[GroupKey, GroupValues] = payinfosThreadLocal.get()
    var payinfos: List[RowMap]  = null
    if (result != null){
         return result        
    }
    val totalpayId:String = payinfo.getColumn("totalpay_id")    
    val entityId:String = payinfo.getColumn("entity_id")    
    var payinfoCriteria :PayinfoCriteria = new PayinfoCriteria()    
    payinfoCriteria.createCriteria().andTotalpayIdEqualTo(totalpayId).andEntityIdEqualTo(entityId)
    payinfoCriteria.addSelCol(PayinfoColEnum.EXT ,PayinfoColEnum.COUPON_COST ,PayinfoColEnum.KINDPAY_ID ,PayinfoColEnum.COUPON_FEE ,PayinfoColEnum.COUPON_NUM ,PayinfoColEnum.FEE ,PayinfoColEnum.TOTALPAY_ID ,PayinfoColEnum.TYPE ,PayinfoColEnum.PAY_ID)
    payinfos = this.orderDAOFacade.getPayinfoDAO().selectColsByExample(payinfoCriteria,1,100)
    result = scala.collection.mutable.Map[GroupKey, GroupValues]()
    var vals : Option[GroupValues] = null
    var groupKey: GroupKey = null
    for ( ( r:RowMap) <- payinfos.asScala){
        groupKey = GroupKey.createCacheKey("totalpay_id",r.getColumn("totalpay_id"),"kindpay_id",r.getColumn("kindpay_id"))
        vals = result.get(groupKey)        
        if (vals.isEmpty) {        
          result +=(groupKey.clone() -> new GroupValues(r))         
        }else{        
         vals.get.addVal(r)        
        }        
    }
    payinfosThreadLocal.set(result);    
    return result;    
}

    
val instancedetailsThreadLocal : ThreadLocal[Map[GroupKey, GroupValues]]  = addThreadLocalVal()

private def mapInstancedetailData( instancedetail : IRowValueGetter) : scala.collection.mutable.Map[GroupKey, GroupValues] ={
    var result :scala.collection.mutable.Map[GroupKey, GroupValues] = instancedetailsThreadLocal.get()
    var instancedetails: List[RowMap]  = null
    if (result != null){
         return result        
    }
    val orderId:String = instancedetail.getColumn("order_id")    
    val entityId:String = instancedetail.getColumn("entity_id")    
    var instancedetailCriteria :InstancedetailCriteria = new InstancedetailCriteria()    
    instancedetailCriteria.createCriteria().andOrderIdEqualTo(orderId).andEntityIdEqualTo(entityId)
    instancedetailCriteria.addSelCol(InstancedetailColEnum.BATCH_MSG ,InstancedetailColEnum.DRAW_STATUS ,InstancedetailColEnum.ENTITY_ID ,InstancedetailColEnum.ORDER_ID)
    instancedetails = this.orderDAOFacade.getInstancedetailDAO().selectColsByExample(instancedetailCriteria,1,100)
    result = scala.collection.mutable.Map[GroupKey, GroupValues]()
    var vals : Option[GroupValues] = null
    var groupKey: GroupKey = null
    for ( ( r:RowMap) <- instancedetails.asScala){
        groupKey = GroupKey.createCacheKey("entity_id",r.getColumn("entity_id"),"order_id",r.getColumn("order_id"))
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

    
val specialfeesThreadLocal : ThreadLocal[Map[GroupKey, GroupValues]]  = addThreadLocalVal()

private def mapSpecialfeeData( specialfee : IRowValueGetter) : scala.collection.mutable.Map[GroupKey, GroupValues] ={
    var result :scala.collection.mutable.Map[GroupKey, GroupValues] = specialfeesThreadLocal.get()
    var specialfees: List[RowMap]  = null
    if (result != null){
         return result        
    }
    val totalpayId:String = specialfee.getColumn("totalpay_id")    
    val entityId:String = specialfee.getColumn("entity_id")    
    var specialfeeCriteria :SpecialfeeCriteria = new SpecialfeeCriteria()    
    specialfeeCriteria.createCriteria().andTotalpayIdEqualTo(totalpayId).andEntityIdEqualTo(entityId)
    specialfeeCriteria.addSelCol(SpecialfeeColEnum.KIND ,SpecialfeeColEnum.FEE ,SpecialfeeColEnum.TOTALPAY_ID ,SpecialfeeColEnum.ENTITY_ID)
    specialfees = this.orderDAOFacade.getSpecialfeeDAO().selectColsByExample(specialfeeCriteria,1,100)
    result = scala.collection.mutable.Map[GroupKey, GroupValues]()
    var vals : Option[GroupValues] = null
    var groupKey: GroupKey = null
    for ( ( r:RowMap) <- specialfees.asScala){
        groupKey = GroupKey.createCacheKey("entity_id",r.getColumn("entity_id"),"totalpay_id",r.getColumn("totalpay_id"))
        vals = result.get(groupKey)        
        if (vals.isEmpty) {        
          result +=(groupKey.clone() -> new GroupValues(r))         
        }else{        
         vals.get.addVal(r)        
        }        
    }
    specialfeesThreadLocal.set(result);    
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
            case "servicebillinfo" => getServicebillinfoPK(row)
                case "totalpayinfo" => getTotalpayinfoPK(row)
                case "orderdetail" => getOrderdetailPK(row)
            case "specialfee" => getSpecialfeePK(row)
            case "ent_expense" => getEntExpensePK(row)
            case "payinfo" => getPayinfoPK(row)
            case "order_bill" => getOrderBillPK(row)
            case "instancedetail" => getInstancedetailPK(row)
            case "ent_expense_order" => getEntExpenseOrderPK(row)
            case _=> throw new IllegalArgumentException("table name:"+ row.getTableName()+" is illegal");
  }
resultPK
}

protected override def getTableFocuse() : java.util.Set[String] = {
  if(_tabsFocuse == null){
     _tabsFocuse = com.google.common.collect.Sets.newHashSet( "servicebillinfo","totalpayinfo","orderdetail","specialfee","ent_expense","payinfo","order_bill","instancedetail","ent_expense_order");
  }
  _tabsFocuse
}

        private def getServicebillinfoPK(row : ITable) : DefaultPk = {

  val columnMeta : AliasList = tabColumnMetaMap.get("servicebillinfo");
  if (columnMeta == null) {
     throw new IllegalStateException("tableName: servicebillinfo is not exist in tabColumnMetaMap");
  }
            
                
                // kkk  servicebillinfo hasParent true
// llll totalpayinfo
        
        return  new CompositePK(columnMeta.getColMeta("servicebill_id").getVal(row,true) ,"entity_id",row.getColumn("entity_id"))/*gencode2*/;
        } // END getServicebillinfoPK
            private def getTotalpayinfoPK(row : ITable) : DefaultPk = {

  val columnMeta : AliasList = tabColumnMetaMap.get("totalpayinfo");
  if (columnMeta == null) {
     throw new IllegalStateException("tableName: totalpayinfo is not exist in tabColumnMetaMap");
  }
      return new CompositePK(row.getColumn("totalpay_id")  ,"entity_id",row.getColumn("entity_id"));
    } // END getTotalpayinfoPK
            private def getOrderdetailPK(row : ITable) : DefaultPk = {

  val columnMeta : AliasList = tabColumnMetaMap.get("orderdetail");
  if (columnMeta == null) {
     throw new IllegalStateException("tableName: orderdetail is not exist in tabColumnMetaMap");
  }
            
                
                // kkk  orderdetail hasParent true
// llll totalpayinfo
        
        return  new CompositePK(columnMeta.getColMeta("totalpay_id").getVal(row,true) ,"entity_id",row.getColumn("entity_id"))/*gencode2*/;
        } // END getOrderdetailPK
        private def getSpecialfeePK(row : ITable) : DefaultPk = {

  val columnMeta : AliasList = tabColumnMetaMap.get("specialfee");
  if (columnMeta == null) {
     throw new IllegalStateException("tableName: specialfee is not exist in tabColumnMetaMap");
  }
            
                
                // kkk  specialfee hasParent true
// llll totalpayinfo
        
        return  new CompositePK(columnMeta.getColMeta("totalpay_id").getVal(row,true) ,"entity_id",row.getColumn("entity_id"))/*gencode2*/;
        } // END getSpecialfeePK
        private def getEntExpensePK(row : ITable) : DefaultPk = {

  val columnMeta : AliasList = tabColumnMetaMap.get("ent_expense");
  if (columnMeta == null) {
     throw new IllegalStateException("tableName: ent_expense is not exist in tabColumnMetaMap");
  }
            
                
                // kkk  ent_expense hasParent false
    //qqq childTabRef.size():1
                    //xxx ent_expense_order is is not ptable
        
                    
                        
            // kkk  ent_expense_order hasParent true
// llll totalpayinfo
        
        
val enterpriseId = row.getColumn("enterprise_id")
val expenseId = row.getColumn("expense_id")
val entExpenseOrders: List[RowMap]  = this.queryEntExpenseOrderByEntExpense(enterpriseId,expenseId) 

val entExpenseOrderMeta : AliasList = tabColumnMetaMap.get("ent_expense_order");
for ( ( r:RowMap) <- entExpenseOrders.asScala){
    pushPojo2Queue( new CompositePK(entExpenseOrderMeta.getColMeta("totalpay_id").getVal(r,true) ,"entity_id",r.getColumn("entity_id")), row)/*gencode3*/
}
 null;
                        } // END getEntExpensePK
        private def getPayinfoPK(row : ITable) : DefaultPk = {

  val columnMeta : AliasList = tabColumnMetaMap.get("payinfo");
  if (columnMeta == null) {
     throw new IllegalStateException("tableName: payinfo is not exist in tabColumnMetaMap");
  }
            
                
                // kkk  payinfo hasParent true
// llll totalpayinfo
        
        return  new CompositePK(columnMeta.getColMeta("totalpay_id").getVal(row,true) ,"entity_id",row.getColumn("entity_id"))/*gencode2*/;
        } // END getPayinfoPK
        private def getOrderBillPK(row : ITable) : DefaultPk = {

  val columnMeta : AliasList = tabColumnMetaMap.get("order_bill");
  if (columnMeta == null) {
     throw new IllegalStateException("tableName: order_bill is not exist in tabColumnMetaMap");
  }
            
                
                // kkk  order_bill hasParent true
// llll orderdetail
    // bbb orderdetail is not ptable
    
            
                
        // kkk  orderdetail hasParent true
// llll totalpayinfo
        
        
val orderId = row.getColumn("order_id")
val entityId = row.getColumn("entity_id")
val orderdetail: RowMap  = this.queryOrderdetailByOrderBill(orderId,entityId) 

val orderdetailMeta : AliasList = tabColumnMetaMap.get("orderdetail");
if(orderdetail != null){
    return  new CompositePK(orderdetailMeta.getColMeta("totalpay_id").getVal(orderdetail,true) ,"entity_id",orderdetail.getColumn("entity_id"))/*gencode4*/
}
 null;
            } // END getOrderBillPK
        private def getInstancedetailPK(row : ITable) : DefaultPk = {

  val columnMeta : AliasList = tabColumnMetaMap.get("instancedetail");
  if (columnMeta == null) {
     throw new IllegalStateException("tableName: instancedetail is not exist in tabColumnMetaMap");
  }
            
                
                // kkk  instancedetail hasParent true
// llll orderdetail
    // bbb orderdetail is not ptable
    
            
                
        // kkk  orderdetail hasParent true
// llll totalpayinfo
        
        
val orderId = row.getColumn("order_id")
val entityId = row.getColumn("entity_id")
val orderdetail: RowMap  = this.queryOrderdetailByInstancedetail(orderId,entityId) 

val orderdetailMeta : AliasList = tabColumnMetaMap.get("orderdetail");
if(orderdetail != null){
    return  new CompositePK(orderdetailMeta.getColMeta("totalpay_id").getVal(orderdetail,true) ,"entity_id",orderdetail.getColumn("entity_id"))/*gencode4*/
}
 null;
            } // END getInstancedetailPK
        private def getEntExpenseOrderPK(row : ITable) : DefaultPk = {

  val columnMeta : AliasList = tabColumnMetaMap.get("ent_expense_order");
  if (columnMeta == null) {
     throw new IllegalStateException("tableName: ent_expense_order is not exist in tabColumnMetaMap");
  }
            
                
                // kkk  ent_expense_order hasParent true
// llll totalpayinfo
        
        return  new CompositePK(columnMeta.getColMeta("totalpay_id").getVal(row,true) ,"entity_id",row.getColumn("entity_id"))/*gencode2*/;
        } // END getEntExpenseOrderPK
        // globalScripts

private def queryOrderdetailByInstancedetail(orderId : String,entityId : String) : RowMap ={
    var orderdetails: List[RowMap]  = null
    var orderdetailCriteria :OrderdetailCriteria = new OrderdetailCriteria()
    orderdetailCriteria.createCriteria().andOrderIdEqualTo(orderId).andEntityIdEqualTo(entityId)
    orderdetailCriteria.addSelCol(OrderdetailColEnum.TOTALPAY_ID ,OrderdetailColEnum.ENTITY_ID ,OrderdetailColEnum.ORDER_ID)
    orderdetails = this.orderDAOFacade.getOrderdetailDAO().selectColsByExample(orderdetailCriteria,1,100)
    for ( ( r:RowMap) <- orderdetails.asScala){
        return r
    }
    null
}


private def queryOrderdetailByOrderBill(orderId : String,entityId : String) : RowMap ={
    var orderdetails: List[RowMap]  = null
    var orderdetailCriteria :OrderdetailCriteria = new OrderdetailCriteria()
    orderdetailCriteria.createCriteria().andOrderIdEqualTo(orderId).andEntityIdEqualTo(entityId)
    orderdetailCriteria.addSelCol(OrderdetailColEnum.TOTALPAY_ID ,OrderdetailColEnum.ENTITY_ID ,OrderdetailColEnum.ORDER_ID)
    orderdetails = this.orderDAOFacade.getOrderdetailDAO().selectColsByExample(orderdetailCriteria,1,100)
    for ( ( r:RowMap) <- orderdetails.asScala){
        return r
    }
    null
}


private def queryEntExpenseOrderByEntExpense(enterpriseId : String,expenseId : String) : List[RowMap] ={
    var entExpenseOrders: List[RowMap]  = null
    var entExpenseOrderCriteria :EntExpenseOrderCriteria = new EntExpenseOrderCriteria()
    entExpenseOrderCriteria.createCriteria().andEnterpriseIdEqualTo(enterpriseId).andExpenseIdEqualTo(expenseId)
    entExpenseOrderCriteria.addSelCol(EntExpenseOrderColEnum.TOTALPAY_ID ,EntExpenseOrderColEnum.ENTERPRISE_ID ,EntExpenseOrderColEnum.ENTITY_ID ,EntExpenseOrderColEnum.EXPENSE_ID)
    entExpenseOrders = this.cardcenterDAOFacade.getEntExpenseOrderDAO().selectColsByExample(entExpenseOrderCriteria,1,100)
    return entExpenseOrders
}



}
