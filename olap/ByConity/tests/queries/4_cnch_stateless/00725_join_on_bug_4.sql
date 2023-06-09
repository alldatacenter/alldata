
drop table if exists t_00725_4;
drop table if exists s_00725_4;
create table t_00725_4(a Int64, b Int64, c String) engine = CnchMergeTree ORDER BY a;
insert into t_00725_4 values(1,1,'a'),(2,2,'b');
create table s_00725_4(a Int64, b Int64, c String) engine = CnchMergeTree ORDER BY a;
insert into s_00725_4 values(1,1,'a');
select t_00725_4.* from t_00725_4 all left join s_00725_4 on
(s_00725_4.a = t_00725_4.a and s_00725_4.b = t_00725_4.b)
where s_00725_4.a is null and s_00725_4.b is null;
drop table if exists t_00725_4;
drop table if exists s_00725_4;
