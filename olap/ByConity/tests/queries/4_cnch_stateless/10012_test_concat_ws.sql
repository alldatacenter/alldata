
select concat_ws('-', 'a', 'b', 'c', 'd');
select concat_ws(',', concat_ws(':', 'a', 'b'), concat_ws(':', 'c', 'd'));
drop table if exists test_concat_ws;
create table test_concat_ws (a String, b String, c String, d Nullable(String), t Date default now()) ENGINE = CnchMergeTree partition by t order by t;

insert into test_concat_ws (a, b, c, d) values ('a', 'b', 'c', 'd'), ('k1', 'v1', 'k2', 'v2'), ('abc', 'abc', 'xyz', null);
SELECT concat_ws(',', concat_ws(':', a, b), concat_ws(':', c, d)) FROM test_concat_ws;
drop table test_concat_ws;
