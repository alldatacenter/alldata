#!/bin/bash

startDate=''
endDate=''

until [ $# -eq 0 ]
do
	if [ $1'x' = '-sdx' ]; then
		shift
		startDate=$1
	elif [ $1'x' = '-edx' ]; then
		shift
		endDate=$1
	fi
	shift
done

if [ -n "$startDate" ] && [ -n "$endDate" ]; then
	echo "use the arguments of the date"
else
	echo "use the default date"
	startDate=$(date -d last-day +%Y-%m-%d)
	endDate=$(date +%Y-%m-%d)
fi
echo "run of arguments. start date is:$startDate, end date is:$endDate"
echo "start run of view depth job "

## insert overwrite
echo "start insert user data to hive tmp table"
hive  -e "from (select pl, from_unixtime(cast(s_time/1000 as bigint),'yyyy-MM-dd') as day, u_ud, (case when count(p_url) = 1 then 'pv1' when count(p_url) = 2 then 'pv2' when count(p_url) = 3 then 'pv3' when count(p_url) = 4 then 'pv4' when count(p_url) >= 5 and count(p_url) <10 then 'pv5_10' when count(p_url) >= 10 and count(p_url) <30 then 'pv10_30' when count(p_url) >=30 and count(p_url) <60 then 'pv30_60'  else 'pv60_plus' end) as pv from event_logs where en='e_pv' and p_url is not null and pl is not null and s_time >= unix_timestamp('$startDate','yyyy-MM-dd')*1000 and s_time < unix_timestamp('$endDate','yyyy-MM-dd')*1000 group by pl, from_unixtime(cast(s_time/1000 as bigint),'yyyy-MM-dd'), u_ud) as tmp insert overwrite table stats_view_depth_tmp select pl,day,pv,count(distinct u_ud) as ct where u_ud is not null group by pl,day,pv"

echo "start insert user data to hive table"
hive  -e "with tmp as (select pl,date,ct as pv1,0 as pv2,0 as pv3,0 as pv4,0 as pv5_10,0 as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv1' union all select pl,date,0 as pv1,ct as pv2,0 as pv3,0 as pv4,0 as pv5_10,0 as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv2' union all select pl,date,0 as pv1,0 as pv2,ct as pv3,0 as pv4,0 as pv5_10,0 as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv3' union all select pl,date,0 as pv1,0 as pv2,0 as pv3,ct as pv4,0 as pv5_10,0 as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv4' union all select pl,date,0 as pv1,0 as pv2,0 as pv3,0 as pv4,ct as pv5_10,0 as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv5_10' union all select pl,date,0 as pv1,0 as pv2,0 as pv3,0 as pv4,0 as pv5_10,ct as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv10_30' union all select pl,date,0 as pv1,0 as pv2,0 as pv3,0 as pv4,0 as pv5_10,0 as pv10_30,ct as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv30_60' union all select pl,date,0 as pv1,0 as pv2,0 as pv3,0 as pv4,0 as pv5_10,0 as pv10_30,0 as pv30_60,ct as pv60_plus from stats_view_depth_tmp where col='pv60_plus' union all select 'all' as pl,date,ct as pv1,0 as pv2,0 as pv3,0 as pv4,0 as pv5_10,0 as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv1' union all select 'all' as pl,date,0 as pv1,ct as pv2,0 as pv3,0 as pv4,0 as pv5_10,0 as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv2' union all select 'all' as pl,date,0 as pv1,0 as pv2,ct as pv3,0 as pv4,0 as pv5_10,0 as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv3' union all select 'all' as pl,date,0 as pv1,0 as pv2,0 as pv3,ct as pv4,0 as pv5_10,0 as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv4' union all select 'all' as pl,date,0 as pv1,0 as pv2,0 as pv3,0 as pv4,ct as pv5_10,0 as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv5_10' union all select 'all' as pl,date,0 as pv1,0 as pv2,0 as pv3,0 as pv4,0 as pv5_10,ct as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv10_30' union all select 'all' as pl,date,0 as pv1,0 as pv2,0 as pv3,0 as pv4,0 as pv5_10,0 as pv10_30,ct as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv30_60' union all select 'all' as pl,date,0 as pv1,0 as pv2,0 as pv3,0 as pv4,0 as pv5_10,0 as pv10_30,0 as pv30_60,ct as pv60_plus from stats_view_depth_tmp where col='pv60_plus' ) from tmp insert overwrite table stats_view_depth select platform_convert(pl),date_convert(date),5,sum(pv1),sum(pv2),sum(pv3),sum(pv4),sum(pv5_10),sum(pv10_30),sum(pv30_60),sum(pv60_plus),date group by pl,date"

echo "start insert session date to hive tmp table"
hive  -e "from (select pl, from_unixtime(cast(s_time/1000 as bigint),'yyyy-MM-dd') as day, u_sd, (case when count(p_url) = 1 then 'pv1' when count(p_url) = 2 then 'pv2' when count(p_url) = 3 then 'pv3' when count(p_url) = 4 then 'pv4' when count(p_url) >= 5 and count(p_url) <10 then 'pv5_10' when count(p_url) >= 10 and count(p_url) <30 then 'pv10_30' when count(p_url) >=30 and count(p_url) <60 then 'pv30_60'  else 'pv60_plus' end) as pv from event_logs where en='e_pv' and p_url is not null and pl is not null and s_time >= unix_timestamp('$startDate','yyyy-MM-dd')*1000 and s_time < unix_timestamp('$endDate','yyyy-MM-dd')*1000 group by pl, from_unixtime(cast(s_time/1000 as bigint),'yyyy-MM-dd'), u_sd ) as tmp insert overwrite table stats_view_depth_tmp select pl,day,pv,count(distinct u_sd) as ct where u_sd is not null group by pl,day,pv"

## insert into 
echo "start insert session data to hive table"
hive  -e "with tmp as (select pl,day,ct as pv1,0 as pv2,0 as pv3,0 as pv4,0 as pv5_10,0 as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv1' union all select pl,day,0 as pv1,ct as pv2,0 as pv3,0 as pv4,0 as pv5_10,0 as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv2' union all select pl,day,0 as pv1,0 as pv2,ct as pv3,0 as pv4,0 as pv5_10,0 as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv3' union all select pl,day,0 as pv1,0 as pv2,0 as pv3,ct as pv4,0 as pv5_10,0 as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv4' union all select pl,day,0 as pv1,0 as pv2,0 as pv3,0 as pv4,ct as pv5_10,0 as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv5_10' union all select pl,day,0 as pv1,0 as pv2,0 as pv3,0 as pv4,0 as pv5_10,ct as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv10_30' union all select pl,day,0 as pv1,0 as pv2,0 as pv3,0 as pv4,0 as pv5_10,0 as pv10_30,ct as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv30_60' union all select pl,day,0 as pv1,0 as pv2,0 as pv3,0 as pv4,0 as pv5_10,0 as pv10_30,0 as pv30_60,ct as pv60_plus from stats_view_depth_tmp where col='pv60_plus' union all select 'all' as pl,day,ct as pv1,0 as pv2,0 as pv3,0 as pv4,0 as pv5_10,0 as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv1' union all select 'all' as pl,day,0 as pv1,ct as pv2,0 as pv3,0 as pv4,0 as pv5_10,0 as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv2' union all select 'all' as pl,day,0 as pv1,0 as pv2,ct as pv3,0 as pv4,0 as pv5_10,0 as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv3' union all select 'all' as pl,day,0 as pv1,0 as pv2,0 as pv3,ct as pv4,0 as pv5_10,0 as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv4' union all select 'all' as pl,day,0 as pv1,0 as pv2,0 as pv3,0 as pv4,ct as pv5_10,0 as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv5_10' union all select 'all' as pl,day,0 as pv1,0 as pv2,0 as pv3,0 as pv4,0 as pv5_10,ct as pv10_30,0 as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv10_30' union all select 'all' as pl,day,0 as pv1,0 as pv2,0 as pv3,0 as pv4,0 as pv5_10,0 as pv10_30,ct as pv30_60,0 as pv60_plus from stats_view_depth_tmp where col='pv30_60' union all select 'all' as pl,day,0 as pv1,0 as pv2,0 as pv3,0 as pv4,0 as pv5_10,0 as pv10_30,0 as pv30_60,ct as pv60_plus from stats_view_depth_tmp where col='pv60_plus' ) from tmp insert into table stats_view_depth select platform_convert(pl),day_convert(day),6,sum(pv1),sum(pv2),sum(pv3),sum(pv4),sum(pv5_10),sum(pv10_30),sum(pv30_60),sum(pv60_plus),day group by pl,day"

## sqoop
echo "run the sqoop script,insert hive data to mysql table"
sqoop export --connect jdbc:mysql://Master:3306/website --username root
--password root --table stats_view_depth --export-dir /user/hive/warehouse/stats_view_depth/*
--input-fields-terminated-by "\\01" --update-mode allowinsert --update-key platform_dimension_id,date_dimension_id,kpi_dimension_id

sqoop export --connect jdbc:mysql://Master:3306/website --username root
--password root --table stats_view_depth --export-dir /user/hive/warehouse/stats_view_depth/*
--input-fields-terminated-by "\\01" --update-mode allowinsert --update-key platform_dimension_id,date_dimension_id,kpi_dimension_id
echo "complete run the view depth job"