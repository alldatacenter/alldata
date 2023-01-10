drop table if exists monotonic_integer;
create table monotonic_integer (val Int32) engine = CnchMergeTree order by val;
insert into monotonic_integer values (-2), (0), (2);
select count() from monotonic_integer where toUInt64(val) == 0;
