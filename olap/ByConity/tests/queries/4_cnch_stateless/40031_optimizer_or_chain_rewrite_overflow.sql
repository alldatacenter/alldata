set enable_optimizer = 1;

select count(uid)
from (
      select 0 as uid,
             1 as moomoo_register_channel_id,
             2 as moomoo_register_subchannel_id,
             3 as moomoo_register_time,
             4 as futu_sg_sg_option_campaign_participants
     )
where ((`moomoo_register_channel_id` = 1061 and `moomoo_register_subchannel_id` = 80 and (`moomoo_register_time` > 1669887496))
or (`moomoo_register_channel_id` = 1002 and `moomoo_register_subchannel_id` = 12643 and (`moomoo_register_time` > 1669887585))
or (`moomoo_register_channel_id` = 1069 and `moomoo_register_subchannel_id` = 8 and (`moomoo_register_time` > 1669887590))
or (`moomoo_register_channel_id` = 1069 and `moomoo_register_subchannel_id` = 9 and (`moomoo_register_time` > 1669887595))
or (`moomoo_register_channel_id` = 1002 and `moomoo_register_subchannel_id` = 431 and (`moomoo_register_time` > 1669887600))
or (`moomoo_register_channel_id` = 1002 and `moomoo_register_subchannel_id` = 354 and (`moomoo_register_time` > 1669887605))
or (`moomoo_register_channel_id` = 1002 and `moomoo_register_subchannel_id` = 12078 and (`moomoo_register_time` >= 1669887610))
or (`moomoo_register_channel_id` = 1002 and `moomoo_register_subchannel_id` = 13284 and (`moomoo_register_time` > 1669887615))
or (`moomoo_register_channel_id` = 1002 and `moomoo_register_subchannel_id` = 13456 and (`moomoo_register_time` > 1669887619))
or (`moomoo_register_channel_id` = 1002 and `moomoo_register_subchannel_id` = 11443 and (`moomoo_register_time` > 1669887632))
or (`moomoo_register_channel_id` = 1002 and `moomoo_register_subchannel_id` = 13234 and (`moomoo_register_time` > 1669887637))
or (`moomoo_register_channel_id` = 1002 and `moomoo_register_subchannel_id` = 11540 and (`moomoo_register_time` > 1669887640))
or (`moomoo_register_channel_id` = 1002 and `moomoo_register_subchannel_id` = 11998 and (`moomoo_register_time` > 1669887645))
or (`moomoo_register_channel_id` = 1002 and `moomoo_register_subchannel_id` = 11425 and (`moomoo_register_time` >= 1669824000))
or (`moomoo_register_channel_id` = 2093 and `moomoo_register_subchannel_id` = 17 and (`moomoo_register_time` > 1669824000))
or (`moomoo_register_channel_id` = 1002 and `moomoo_register_subchannel_id` = 13456 and (`moomoo_register_time` >= 1669824000))
or (`moomoo_register_channel_id` = 1068 and `moomoo_register_subchannel_id` = 6 and (`moomoo_register_time` >= 1669824000))
or (`moomoo_register_channel_id` = 1068 and `moomoo_register_subchannel_id` = 101 and (`moomoo_register_time` >= 1669824000))
or (`moomoo_register_channel_id` = 1068 and `moomoo_register_subchannel_id` = 102 and (`moomoo_register_time` >= 1669824000))
or (`moomoo_register_channel_id` = 1068 and `moomoo_register_subchannel_id` = 103 and (`moomoo_register_time` >= 1669824000))
or (`moomoo_register_channel_id` = 1068 and `moomoo_register_subchannel_id` = 104 and (`moomoo_register_time` >= 1669824000))
or (`moomoo_register_channel_id` = 1068 and `moomoo_register_subchannel_id` = 105 and (`moomoo_register_time` >= 1669824000))
or (`moomoo_register_channel_id` = 1010 and `moomoo_register_subchannel_id` = 103 and (`moomoo_register_time` >= 1669824000))
or (`moomoo_register_channel_id` = 1010 and `moomoo_register_subchannel_id` = 153 and (`moomoo_register_time` >= 1669824000))
or (`moomoo_register_channel_id` = 1010 and `moomoo_register_subchannel_id` = 160 and (`moomoo_register_time` >= 1669824000))
or (`moomoo_register_channel_id` = 1013 and `moomoo_register_subchannel_id` = 56 and (`moomoo_register_time` >= 1669824000))
or (`moomoo_register_channel_id` = 1013 and `moomoo_register_subchannel_id` = 57 and (`moomoo_register_time` >= 1669824000))
or (`moomoo_register_channel_id` = 1061 and `moomoo_register_subchannel_id` = 83 and (`moomoo_register_time` >= 1669824000))
or (`moomoo_register_channel_id` = 2093 and `moomoo_register_subchannel_id` = 15 and (`moomoo_register_time` >= 1669824000))
or (`moomoo_register_channel_id` = 2093 and `moomoo_register_subchannel_id` = 16 and (`moomoo_register_time` >= 1669824000))
or (`moomoo_register_channel_id` = 2093 and `moomoo_register_subchannel_id` = 22 and (`moomoo_register_time` > 1669824000))
or (`moomoo_register_channel_id` = 2093 and `moomoo_register_subchannel_id` = 19 and (`moomoo_register_time` >= 1669824000))
or (`moomoo_register_channel_id` = 1002 and `moomoo_register_subchannel_id` = 11988 and (`moomoo_register_time` >= 1669824000))
or (`moomoo_register_channel_id` = 1069 and `moomoo_register_subchannel_id` = 6 and (`moomoo_register_time` >= 1669824000))
or (`moomoo_register_channel_id` = 1069 and `moomoo_register_subchannel_id` = 7 and (`moomoo_register_time` >= 1669824000))
or (`futu_sg_sg_option_campaign_participants` in (1)));
