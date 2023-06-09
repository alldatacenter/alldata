set enable_optimizer=1;

DROP TABLE IF EXISTS nation_league;
DROP TABLE IF EXISTS all_nation;
-- Skip the test with system tables because optimizer cannot support them now
-- This testcase mainly test quantified comparison with subquery

select toUInt64(1) == any (select number from system.numbers limit 10);
select toUInt64(12) == any (select number from system.numbers limit 10);
select toUInt64(1) != all (select toUInt64(1) from system.numbers limit 10);
select toUInt64(1) != all (select number from system.numbers limit 10);
select toUInt64(1) == all (select toUInt64(1) from system.numbers limit 10);
select toUInt64(1) == all (select number from system.numbers limit 10);
select toUInt64(1) != any (select toUInt64(1) from system.numbers limit 10);
select toUInt64(1) != any (select number from system.numbers limit 10);
select toUInt64(1) < any (select toUInt64(1) from system.numbers limit 10);
select toUInt64(1) <= any (select toUInt64(1) from system.numbers limit 10);
select toUInt64(1) < any (select number from system.numbers limit 10);
select toUInt64(1) > any (select number from system.numbers limit 10);
select toUInt64(1) >= any (select number from system.numbers limit 10);
select toUInt64(11) > all (select number from system.numbers limit 10);
select toUInt64(11) <= all (select number from system.numbers limit 11);
select toUInt64(11) < all (select 11 from system.numbers limit 10);
select toUInt64(11) > all (select 11 from system.numbers limit 10);
select toUInt64(11) >= all (select 11 from system.numbers limit 10);

create table nation_league (id_nation Int32, name Nullable(String), region_key Nullable(Int32)) ENGINE=CnchMergeTree() order by id_nation;
create table all_nation (id_nation Int32, nation_name Nullable(String), nation_key Nullable(Int32)) ENGINE=CnchMergeTree() order by id_nation;
insert into nation_league values(8,'a',55) , (20, 'b', 546), (70, 'c', 44), (30, 'f', 90), (1,'p',3), (88, 's', 78);
insert into all_nation values(15,'u',546) , (2, 'm', 546), (7, 'c', 44), (30, 'f', 44), (8,'a',55), (4,'p',60), (5, 'p', 70), (61, 's', 102), (62, 's', 103), (63, 's', null);
select id_nation from nation_league where id_nation  != any (select id_nation from all_nation where region_key = all_nation.nation_key) order by id_nation;
select id_nation from nation_league where id_nation  > any (select id_nation from all_nation where region_key = all_nation.nation_key) order by id_nation;
select id_nation from nation_league where id_nation  < all (select id_nation from all_nation where region_key = all_nation.nation_key) order by id_nation;
select region_key from nation_league where region_key  < all (select nation_key from all_nation where name = all_nation.nation_name) order by id_nation;
select region_key < any (select nation_key from all_nation) from nation_league;
