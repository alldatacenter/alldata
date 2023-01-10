
drop table if exists lc;
create table lc (str StringWithDictionary, val UInt8WithDictionary) engine = CnchMergeTree order by tuple();
insert into lc values ('a', 1), ('b', 2);

SET enable_left_join_to_right_join=0;

select str, str in ('a', 'd') from lc order by str;
select val, val in (1, 3) from lc order by val;
select str, str in (select arrayJoin(['a', 'd'])) from lc order by str;
select val, val in (select arrayJoin([1, 3])) from lc order by  val;
select str, str in (select str from lc) from lc order by str;
select val, val in (select val from lc) from lc order by val;
drop table if exists lc;
