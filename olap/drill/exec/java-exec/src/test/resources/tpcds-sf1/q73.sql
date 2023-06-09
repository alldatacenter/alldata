select c.c_last_name,
  c.c_first_name,
  c.c_salutation,
  c.c_preferred_cust_flag,
  dj.sstn,
  dj.cnt
from (
  select ss.ss_ticket_number as sstn, ss.ss_customer_sk as sscsk, count(*) cnt
  from dfs.tpcds.store_sales as ss,
    dfs.tpcds.date_dim as d,
    dfs.tpcds.store as s,
    dfs.tpcds.household_demographics as hd
  where ss.ss_sold_date_sk = d.d_date_sk
    and ss.ss_store_sk = s.s_store_sk
    and ss.ss_hdemo_sk = hd.hd_demo_sk
    and (hd.hd_buy_potential = '>10000' or hd.hd_buy_potential = 'unknown')
    and hd.hd_vehicle_count > 0
    and case when hd.hd_vehicle_count > 0 then hd.hd_dep_count / hd.hd_vehicle_count else null end > 1
    and s.s_county in ('Saginaw County', 'Sumner County', 'Appanoose County', 'Daviess County')
    and ss.ss_sold_date_sk between 2451180 and 2451269
  group by ss.ss_ticket_number, ss.ss_customer_sk
) dj,
  dfs.tpcds.customer as c
where dj.sscsk = c.c_customer_sk
  and dj.cnt between 1 and 5
order by dj.cnt desc
limit 1000