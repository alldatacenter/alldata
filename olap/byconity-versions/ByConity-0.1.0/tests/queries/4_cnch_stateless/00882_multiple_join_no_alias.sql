
drop table if exists t;
drop table if exists s;
drop table if exists y;
create table t(a Int64, b Int64) engine = CnchMergeTree ORDER BY a;
create table s(a Int64, b Int64) engine = CnchMergeTree ORDER BY a;
create table y(a Int64, b Int64) engine = CnchMergeTree ORDER BY a;
insert into t values (1,1), (2,2);
insert into s values (1,1);
insert into y values (1,1);

select s.a, s.a, s.b as s_b, s.b from t
left join s on s.a = t.a
left join y on s.b = y.b
order by s.a, s_b;

set enable_optimizer=0;

select max(s.a) from t
left join s on s.a = t.a
left join y on s.b = y.b
group by t.a order by t.a;

select t.a, t.a as t_a, s.a, s.a as s_a, y.a, y.a as y_a from t
left join s on t.a = s.a
left join y on y.b = s.b
order by t_a, s_a, y_a;

select t.a, t.a as t_a, max(s.a) from t
left join s on t.a = s.a
left join y on y.b = s.b
group by t_a order by t_a;

drop table t;
drop table s;
drop table y;
