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
echo "start run of event job "

## insert overwrite
echo "start insert event data to hive table"
hive -e "with tmp as (select pl,from_unixtime(cast(s_time/1000 as bigint),'yyyy-MM-dd') as day,ca,ac from event_logs where en='e_e' and pl is not null and s_time >= unix_timestamp('$startDate','yyyy-MM-dd')*1000 and s_time < unix_timestamp('$endDate','yyyy-MM-dd')*1000 ) from (select pl as pl,day,ca as ca,ac as ac,count(1) as times from tmp group by pl,day,ca,ac union all select 'all' as pl,day,ca as ca,ac as ac,count(1) as times from tmp group by day,ca,ac union all select pl as pl,day,ca as ca,'all' as ac,count(1) as times from tmp group by pl,day,ca union all select 'all' as pl,day,ca as ca,'all' as ac,count(1) as times from tmp group by day,ca union all select pl as pl,day,'all' as ca,'all' as ac,count(1) as times from tmp group by pl,day union all select 'all' as pl,day,'all' as ca,'all' as ac,count(1) as times from tmp group by day) as tmp2 insert overwrite table stats_event select platform_convert(pl),date_convert(day),event_convert(ca,ac),sum(times),day group by pl,day,ca,ac"

## sqoop
echo "run the sqoop script,insert hive data to mysql table"
sqoop export --connect jdbc:mysql://Master:3306/website --username root  --password root --table stats_event --export-dir /user/hive/warehouse/stats_event/* --input-fields-terminated-by "\\01" --update-mode allowinsert --update-key platform_dimension_id,date_dimension_id,kpi_dimension_id
echo "complete run the event job"