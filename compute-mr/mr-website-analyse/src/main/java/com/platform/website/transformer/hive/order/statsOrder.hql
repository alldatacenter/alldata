-- 1. 在hive中创建hbase的event_logs对应表
CREATE EXTERNAL TABLE event_logs(rowkey string, pl string, en string, s_time bigint, p_url string, u_ud string, u_sd string, ca string, ac string, oid string, `on` string, cua bigint, cut string, pt string)
ROW FORMAT SERDE 'org.apache.hadoop.hive.hbase.HBaseSerDe'
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
with serdeproperties('hbase.columns.mapping'=':key,info:pl,info:en,info:s_time,info:p_url,info:u_ud,info:u_sd,info:ca,info:ac, info:oid,info:on,info:cua,info:cut,info:pt')
tblproperties('hbase.table.name'='event_logs');

-- 2. 自定义UDF(CurrencyType&PaymentType)
-- 3. 上传transformer-0.0.1.jar到linux集群和hdfs集群上，然后启动DimensionConverterServer服务
-- 4. 创建function
create function currency_type_convert as 'com.platform.website.transformer.hive.CurrencyTypeDimensionUDF' using jar 'hdfs://Master:9000/WEBSITE/transformer/WEBSITE-transfromer2.jar';
create function payment_type_convert as 'com.platform.website.transformer.hive.PaymentTypeDimensionUDF' using jar 'hdfs://Master:9000/WEBSITE/transformer/WEBSITE-transfromer2.jar';
create function order_info as 'com.platform.website.transformer.hive.OrderInfoUDF' using jar 'hdfs://Master:9000/WEBSITE/transformer/WEBSITE-transfromer2.jar';
create function order_total_amount as 'com.platform.website.transformer.hive.OrderTotalAmountUDF' using jar 'hdfs://Master:9000/WEBSITE/transformer/WEBSITE-transfromer2.jar';

-- 5. 创建临时表
create table stats_order_tmp1(pl string, dt string, cut string, pt string, order_values bigint);
CREATE TABLE `stats_order_tmp2` (`platform_dimension_id` bigint ,`date_dimension_id` bigint , `currency_type_dimension_id` bigint ,`payment_type_dimension_id` bigint , `order_values` bigint, `created` string);

-- 6. 保存订单数据到mysql中
	创建hive中间表
CREATE TABLE `order_info` (`order_id` string,`platform` string,`s_time` bigint ,`currency_type` string ,`payment_type` string , `amount` bigint);
	hql插入到hive表
from event_logs
insert overwrite table order_info select oid,pl,s_time,cut,pt,cua
where en='e_crt' and pl is not null and s_time >= unix_timestamp('2015-12-13','yyyy-MM-dd')*1000 and s_time < unix_timestamp('2015-12-14','yyyy-MM-dd')*1000
或者
from (
select oid,pl,s_time,cut,pt,cua from event_logs
where en='e_crt' and pl is not null and s_time >= unix_timestamp('2015-12-13','yyyy-MM-dd')*1000 and s_time < unix_timestamp('2015-12-14','yyyy-MM-dd')*1000
) as tmp
insert overwrite table order_info select oid,pl,s_time,cut,pt,cua 
	sqoop脚本
sqoop export --connect jdbc:mysql://hh:3306/report --username hive --password hive --table order_info --export-dir /hive/bigdater.db/order_info/* --input-fields-terminated-by "\\01" --update-mode allowinsert --update-key order_id


-- 7. 订单数量hql(总的)
from (
select pl,from_unixtime(cast(s_time/1000 as bigint),'yyyy-MM-dd') as date,cut,pt,count(distinct oid) as orders
from event_logs
where en='e_crt' and pl is not null and s_time >= unix_timestamp('2015-12-13','yyyy-MM-dd')*1000 and s_time < unix_timestamp('2015-12-14','yyyy-MM-dd')*1000 
group by pl,from_unixtime(cast(s_time/1000 as bigint),'yyyy-MM-dd'),cut,pt) as tmp
insert overwrite table stats_order_tmp1 select pl,date,cut,pt,orders
insert overwrite table stats_order_tmp2 select platform_convert(pl),date_convert(date),currency_type_convert(cut),payment_type_convert(pt),orders,date

from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert(cut),payment_type_convert(pt),sum(order_values) as orders,dt group by dt,cut,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert('all'),payment_type_convert(pt),sum(order_values) as orders,dt group by dt,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert(cut),payment_type_convert('all'),sum(order_values) as orders,dt group by dt,cut
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert('all'),payment_type_convert('all'),sum(order_values) as orders,dt group by dt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert('all'),payment_type_convert(pt),sum(order_values) as orders,dt group by pl,dt,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert('all'),payment_type_convert('all'),sum(order_values) as orders,dt group by pl,dt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert(cut),payment_type_convert('all'),sum(order_values) as orders,dt group by pl,dt,cut

-- 8. 订单数量sqoop(总的)
sqoop export --connect jdbc:mysql://hh:3306/report --username hive --password hive --table stats_order --export-dir /hive/bigdater.db/stats_order_tmp2/* --input-fields-terminated-by "\\01" --update-mode allowinsert --update-key platform_dimension_id,date_dimension_id,currency_type_dimension_id,payment_type_dimension_id --columns platform_dimension_id,date_dimension_id,currency_type_dimension_id,payment_type_dimension_id,orders,created


-- 9. 订单金额hql(总的)
from (
from (select pl,from_unixtime(cast(s_time/1000 as bigint),'yyyy-MM-dd') as date,cut,pt,oid,max(cua) as amount
from event_logs
where en='e_crt' and pl is not null and s_time >= unix_timestamp('2015-12-13','yyyy-MM-dd')*1000 and s_time < unix_timestamp('2015-12-14','yyyy-MM-dd')*1000 
group by pl,from_unixtime(cast(s_time/1000 as bigint),'yyyy-MM-dd'),cut,pt,oid) as tmp
select pl,date,cut,pt,sum(amount) as amount group by pl,date,cut,pt
) as tmp2
insert overwrite table stats_order_tmp1 select pl,date,cut,pt,amount
insert overwrite table stats_order_tmp2 select platform_convert(pl),date_convert(date),currency_type_convert(cut),payment_type_convert(pt),amount,date

from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert(cut),payment_type_convert(pt),sum(order_values) as amount,dt group by dt,cut,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert('all'),payment_type_convert(pt),sum(order_values) as amount,dt group by dt,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert(cut),payment_type_convert('all'),sum(order_values) as amount,dt group by dt,cut
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert('all'),payment_type_convert('all'),sum(order_values) as amount,dt group by dt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert('all'),payment_type_convert(pt),sum(order_values) as amount,dt group by pl,dt,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert('all'),payment_type_convert('all'),sum(order_values) as amount,dt group by pl,dt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert(cut),payment_type_convert('all'),sum(order_values) as amount,dt group by pl,dt,cut

-- 10.订单金额sqoop(总的)
sqoop export --connect jdbc:mysql://hh:3306/report --username hive --password hive --table stats_order --export-dir /hive/bigdater.db/stats_order_tmp2/* --input-fields-terminated-by "\\01" --update-mode allowinsert --update-key platform_dimension_id,date_dimension_id,currency_type_dimension_id,payment_type_dimension_id --columns platform_dimension_id,date_dimension_id,currency_type_dimension_id,payment_type_dimension_id,order_amount,created


-- 11. 成功支付订单数量hql
from (
select order_info(oid,'pl') as pl,from_unixtime(cast(s_time/1000 as bigint),'yyyy-MM-dd') as date,order_info(oid,'cut') as cut,order_info(oid,'pt') as pt,count(distinct oid) as orders
from event_logs
where en='e_cs' and pl is not null and s_time >= unix_timestamp('2015-12-13','yyyy-MM-dd')*1000 and s_time < unix_timestamp('2015-12-14','yyyy-MM-dd')*1000 
group by order_info(oid,'pl'),from_unixtime(cast(s_time/1000 as bigint),'yyyy-MM-dd'),order_info(oid,'cut'),order_info(oid,'pt')
) as tmp
insert overwrite table stats_order_tmp1 select pl,date,cut,pt,orders
insert overwrite table stats_order_tmp2 select platform_convert(pl),date_convert(date),currency_type_convert(cut),payment_type_convert(pt),orders,date

from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert(cut),payment_type_convert(pt),sum(order_values) as orders,dt group by dt,cut,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert('all'),payment_type_convert(pt),sum(order_values) as orders,dt group by dt,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert(cut),payment_type_convert('all'),sum(order_values) as orders,dt group by dt,cut
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert('all'),payment_type_convert('all'),sum(order_values) as orders,dt group by dt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert('all'),payment_type_convert(pt),sum(order_values) as orders,dt group by pl,dt,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert('all'),payment_type_convert('all'),sum(order_values) as orders,dt group by pl,dt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert(cut),payment_type_convert('all'),sum(order_values) as orders,dt group by pl,dt,cut

-- 12. 成功支付订单数量sqoop
sqoop export --connect jdbc:mysql://hh:3306/report --username hive --password hive --table stats_order --export-dir /hive/bigdater.db/stats_order_tmp2/* --input-fields-terminated-by "\\01" --update-mode allowinsert --update-key platform_dimension_id,date_dimension_id,currency_type_dimension_id,payment_type_dimension_id --columns platform_dimension_id,date_dimension_id,currency_type_dimension_id,payment_type_dimension_id,success_orders,created

-- 13. 成功支付订单金额hql
from (from (select order_info(oid,'pl') as pl,from_unixtime(cast(s_time/1000 as bigint),'yyyy-MM-dd') as date,order_info(oid,'cut') as cut,order_info(oid,'pt') as pt,oid,max(order_info(oid)) as amount
from event_logs
where en='e_cs' and pl is not null and s_time >= unix_timestamp('2015-12-13','yyyy-MM-dd')*1000 and s_time < unix_timestamp('2015-12-14','yyyy-MM-dd')*1000 
group by order_info(oid,'pl'),from_unixtime(cast(s_time/1000 as bigint),'yyyy-MM-dd'),order_info(oid,'cut'),order_info(oid,'pt'),oid) as tmp
select pl,date,cut,pt,sum(amount) as amount group by pl,date,cut,pt) as tmp2
insert overwrite table stats_order_tmp1 select pl,date,cut,pt,amount
insert overwrite table stats_order_tmp2 select platform_convert(pl),date_convert(date),currency_type_convert(cut),payment_type_convert(pt),amount,date

from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert(cut),payment_type_convert(pt),sum(order_values) as amount,dt group by dt,cut,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert('all'),payment_type_convert(pt),sum(order_values) as amount,dt group by dt,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert(cut),payment_type_convert('all'),sum(order_values) as amount,dt group by dt,cut
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert('all'),payment_type_convert('all'),sum(order_values) as amount,dt group by dt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert('all'),payment_type_convert(pt),sum(order_values) as amount,dt group by pl,dt,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert('all'),payment_type_convert('all'),sum(order_values) as amount,dt group by pl,dt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert(cut),payment_type_convert('all'),sum(order_values) as amount,dt group by pl,dt,cut

-- 14. 成功支付订单金额sqoop
sqoop export --connect jdbc:mysql://hh:3306/report --username hive --password hive --table stats_order --export-dir /hive/bigdater.db/stats_order_tmp2/* --input-fields-terminated-by "\\01" --update-mode allowinsert --update-key platform_dimension_id,date_dimension_id,currency_type_dimension_id,payment_type_dimension_id --columns platform_dimension_id,date_dimension_id,currency_type_dimension_id,payment_type_dimension_id,revenue_amount,created

-- 15. 成功支付订单迄今为止总金额hql
from (from (select order_info(oid,'pl') as pl,from_unixtime(cast(s_time/1000 as bigint),'yyyy-MM-dd') as date,order_info(oid,'cut') as cut,order_info(oid,'pt') as pt,oid,max(order_info(oid)) as amount
from event_logs
where en='e_cs' and pl is not null and s_time >= unix_timestamp('2015-12-13','yyyy-MM-dd')*1000 and s_time < unix_timestamp('2015-12-14','yyyy-MM-dd')*1000 
group by order_info(oid,'pl'),from_unixtime(cast(s_time/1000 as bigint),'yyyy-MM-dd'),order_info(oid,'cut'),order_info(oid,'pt'),oid) as tmp
select pl,date,cut,pt,sum(amount) as amount group by pl,date,cut,pt) as tmp2
insert overwrite table stats_order_tmp1 select pl,date,cut,pt,amount
insert overwrite table stats_order_tmp2 select platform_convert(pl),date_convert(date),currency_type_convert(cut),payment_type_convert(pt),order_total_amount(platform_convert(pl),date_convert(date),currency_type_convert(cut),payment_type_convert(pt),cast(amount as int),'revenue') as amount,date

from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert(cut),payment_type_convert(pt),order_total_amount(platform_convert('all'),date_convert(dt),currency_type_convert(cut),payment_type_convert(pt),cast(sum(order_values) as int),"revenue") as amount,dt group by dt,cut,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert('all'),payment_type_convert(pt),order_total_amount(platform_convert('all'),date_convert(dt),currency_type_convert('all'),payment_type_convert(pt),cast(sum(order_values) as int),"revenue") as amount,dt group by dt,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert(cut),payment_type_convert('all'),order_total_amount(platform_convert('all'),date_convert(dt),currency_type_convert(cut),payment_type_convert('all'),cast(sum(order_values) as int),"revenue") as amount,dt group by dt,cut
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert('all'),payment_type_convert('all'),order_total_amount(platform_convert('all'),date_convert(dt),currency_type_convert('all'),payment_type_convert('all'),cast(sum(order_values) as int),"revenue") as amount,dt group by dt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert('all'),payment_type_convert(pt),order_total_amount(platform_convert(pl),date_convert(dt),currency_type_convert('all'),payment_type_convert(pt),cast(sum(order_values) as int),"revenue") as amount,dt group by pl,dt,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert('all'),payment_type_convert('all'),order_total_amount(platform_convert(pl),date_convert(dt),currency_type_convert('all'),payment_type_convert('all'),cast(sum(order_values) as int),"revenue") as amount,dt group by pl,dt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert(cut),payment_type_convert('all'),order_total_amount(platform_convert(pl),date_convert(dt),currency_type_convert(cut),payment_type_convert('all'),cast(sum(order_values) as int),"revenue") as amount,dt group by pl,dt,cut

-- 16. 成功支付订单迄今为止总金额sqoop
sqoop export --connect jdbc:mysql://hh:3306/report --username hive --password hive --table stats_order --export-dir /hive/bigdater.db/stats_order_tmp2/* --input-fields-terminated-by "\\01" --update-mode allowinsert --update-key platform_dimension_id,date_dimension_id,currency_type_dimension_id,payment_type_dimension_id --columns platform_dimension_id,date_dimension_id,currency_type_dimension_id,payment_type_dimension_id,total_revenue_amount,created


-- 17. 退款订单数量hql
from (
select order_info(oid,'pl') as pl,from_unixtime(cast(s_time/1000 as bigint),'yyyy-MM-dd') as date,order_info(oid,'cut') as cut,order_info(oid,'pt') as pt,count(distinct oid) as orders
from event_logs
where en='e_cr' and pl is not null and s_time >= unix_timestamp('2015-12-13','yyyy-MM-dd')*1000 and s_time < unix_timestamp('2015-12-14','yyyy-MM-dd')*1000 
group by order_info(oid,'pl'),from_unixtime(cast(s_time/1000 as bigint),'yyyy-MM-dd'),order_info(oid,'cut'),order_info(oid,'pt')
) as tmp
insert overwrite table stats_order_tmp1 select pl,date,cut,pt,orders
insert overwrite table stats_order_tmp2 select platform_convert(pl),date_convert(date),currency_type_convert(cut),payment_type_convert(pt),orders,date

from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert(cut),payment_type_convert(pt),sum(order_values) as orders,dt group by dt,cut,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert('all'),payment_type_convert(pt),sum(order_values) as orders,dt group by dt,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert(cut),payment_type_convert('all'),sum(order_values) as orders,dt group by dt,cut
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert('all'),payment_type_convert('all'),sum(order_values) as orders,dt group by dt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert('all'),payment_type_convert(pt),sum(order_values) as orders,dt group by pl,dt,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert('all'),payment_type_convert('all'),sum(order_values) as orders,dt group by pl,dt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert(cut),payment_type_convert('all'),sum(order_values) as orders,dt group by pl,dt,cut

-- 18. 退款订单数量sqoop
sqoop export --connect jdbc:mysql://hh:3306/report --username hive --password hive --table stats_order --export-dir /hive/bigdater.db/stats_order_tmp2/* --input-fields-terminated-by "\\01" --update-mode allowinsert --update-key platform_dimension_id,date_dimension_id,currency_type_dimension_id,payment_type_dimension_id --columns platform_dimension_id,date_dimension_id,currency_type_dimension_id,payment_type_dimension_id,refund_orders,created

-- 19. 退款订单金额hql
from (from (select order_info(oid,'pl') as pl,from_unixtime(cast(s_time/1000 as bigint),'yyyy-MM-dd') as date,order_info(oid,'cut') as cut,order_info(oid,'pt') as pt,oid,max(order_info(oid)) as amount
from event_logs
where en='e_cr' and pl is not null and s_time >= unix_timestamp('2015-12-13','yyyy-MM-dd')*1000 and s_time < unix_timestamp('2015-12-14','yyyy-MM-dd')*1000 
group by order_info(oid,'pl'),from_unixtime(cast(s_time/1000 as bigint),'yyyy-MM-dd'),order_info(oid,'cut'),order_info(oid,'pt'),oid) as tmp
select pl,date,cut,pt,sum(amount) as amount group by pl,date,cut,pt) as tmp2
insert overwrite table stats_order_tmp1 select pl,date,cut,pt,amount
insert overwrite table stats_order_tmp2 select platform_convert(pl),date_convert(date),currency_type_convert(cut),payment_type_convert(pt),amount,date

from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert(cut),payment_type_convert(pt),sum(order_values) as amount,dt group by dt,cut,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert('all'),payment_type_convert(pt),sum(order_values) as amount,dt group by dt,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert(cut),payment_type_convert('all'),sum(order_values) as amount,dt group by dt,cut
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert('all'),payment_type_convert('all'),sum(order_values) as amount,dt group by dt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert('all'),payment_type_convert(pt),sum(order_values) as amount,dt group by pl,dt,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert('all'),payment_type_convert('all'),sum(order_values) as amount,dt group by pl,dt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert(cut),payment_type_convert('all'),sum(order_values) as amount,dt group by pl,dt,cut

-- 20. 退款订单金额sqoop
sqoop export --connect jdbc:mysql://hh:3306/report --username hive --password hive --table stats_order --export-dir /hive/bigdater.db/stats_order_tmp2/* --input-fields-terminated-by "\\01" --update-mode allowinsert --update-key platform_dimension_id,date_dimension_id,currency_type_dimension_id,payment_type_dimension_id --columns platform_dimension_id,date_dimension_id,currency_type_dimension_id,payment_type_dimension_id,refund_amount,created

-- 21. 退款订单迄今为止总金额hql
from (from (select order_info(oid,'pl') as pl,from_unixtime(cast(s_time/1000 as bigint),'yyyy-MM-dd') as date,order_info(oid,'cut') as cut,order_info(oid,'pt') as pt,oid,max(order_info(oid)) as amount
from event_logs
where en='e_cr' and pl is not null and s_time >= unix_timestamp('2015-12-13','yyyy-MM-dd')*1000 and s_time < unix_timestamp('2015-12-14','yyyy-MM-dd')*1000 
group by order_info(oid,'pl'),from_unixtime(cast(s_time/1000 as bigint),'yyyy-MM-dd'),order_info(oid,'cut'),order_info(oid,'pt'),oid) as tmp
select pl,date,cut,pt,sum(amount) as amount group by pl,date,cut,pt) as tmp2
insert overwrite table stats_order_tmp1 select pl,date,cut,pt,amount
insert overwrite table stats_order_tmp2 select platform_convert(pl),date_convert(date),currency_type_convert(cut),payment_type_convert(pt),order_total_amount(platform_convert(pl),date_convert(date),currency_type_convert(cut),payment_type_convert(pt),cast(amount as int),'refund') as amount,date

from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert(cut),payment_type_convert(pt),order_total_amount(platform_convert('all'),date_convert(dt),currency_type_convert(cut),payment_type_convert(pt),cast(sum(order_values) as int),'refund') as amount,dt group by dt,cut,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert('all'),payment_type_convert(pt),order_total_amount(platform_convert('all'),date_convert(dt),currency_type_convert('all'),payment_type_convert(pt),cast(sum(order_values) as int),'refund') as amount,dt group by dt,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert(cut),payment_type_convert('all'),order_total_amount(platform_convert('all'),date_convert(dt),currency_type_convert(cut),payment_type_convert('all'),cast(sum(order_values) as int),'refund') as amount,dt group by dt,cut
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert('all'),date_convert(dt),currency_type_convert('all'),payment_type_convert('all'),order_total_amount(platform_convert('all'),date_convert(dt),currency_type_convert('all'),payment_type_convert('all'),cast(sum(order_values) as int),'refund') as amount,dt group by dt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert('all'),payment_type_convert(pt),order_total_amount(platform_convert(pl),date_convert(dt),currency_type_convert('all'),payment_type_convert(pt),cast(sum(order_values) as int),'refund') as amount,dt group by pl,dt,pt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert('all'),payment_type_convert('all'),order_total_amount(platform_convert(pl),date_convert(dt),currency_type_convert('all'),payment_type_convert('all'),cast(sum(order_values) as int),'refund') as amount,dt group by pl,dt
from stats_order_tmp1 insert into table stats_order_tmp2 select platform_convert(pl),date_convert(dt),currency_type_convert(cut),payment_type_convert('all'),order_total_amount(platform_convert(pl),date_convert(dt),currency_type_convert(cut),payment_type_convert('all'),cast(sum(order_values) as int),'refund') as amount,dt group by pl,dt,cut

-- 22. 退款订单迄今为止总金额sqoop
sqoop export --connect jdbc:mysql://hh:3306/report --username hive --password hive --table stats_order --export-dir /hive/bigdater.db/stats_order_tmp2/* --input-fields-terminated-by "\\01" --update-mode allowinsert --update-key platform_dimension_id,date_dimension_id,currency_type_dimension_id,payment_type_dimension_id --columns platform_dimension_id,date_dimension_id,currency_type_dimension_id,payment_type_dimension_id,total_refund_amount,created



