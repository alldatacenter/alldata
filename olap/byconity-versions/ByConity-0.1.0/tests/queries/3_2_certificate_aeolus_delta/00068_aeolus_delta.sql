SET output_format_write_statistics = 0;
select
toDateTime(timestamp) as date,
feature_string{'activity_name_uniq'} as activity_name_uniq,
decision_id,
uid,
did,
result,
feature_number,
feature_string,
rule,
var_number
from caijing_tp.decision_result_zhifu
where   event='user_get_voucher_request'
and feature_string{'activity_name_uniq'}  like '%免单%'
and toDate(timestamp)= toDate(today())
--and rule{'8571'}=1
--and timestamp>=1628578807
--and timestamp>=1628006400
--and uid in ('971619260437799',
--'4503673386142723')
--and result='deny'
 limit 1000000 format JSONCompact