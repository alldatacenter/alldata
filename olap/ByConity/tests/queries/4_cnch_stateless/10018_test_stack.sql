
set send_logs_level = 'none';

drop table if exists test_stack;

create table test_stack(date Date, x Int32) engine = CnchMergeTree partition by date order by x;
insert into test_stack values ('2019-11-01', 1), ('2019-11-01', 3), ('2019-10-01', 6);
insert into test_stack values ('2019-11-01', 6), ('2019-11-01', 4), ('2019-10-01', 1);
select countStack(1, 10, 1)(1, x) from test_stack;
select sumStack(1, 10, 1)(1, x) from test_stack;
select uniqExactStack(1, 10, 1)(x, x) from test_stack;
select quantileExactStack(1, 10, 1)(x, x) from test_stack;
select quantileExactStack(0.1, 1, 10, 1)(x, x) from test_stack;
select quantileExactStack(0.5, 1, 10, 1)(x, x) from test_stack;

drop table test_stack;

SET enable_optimizer = 0; -- type tuple is not supported for hash in optimizer


select MergeStreamStack(a) from( select a from (select cast([(1, 2), (2, 2)] as Array(Tuple(Int8, Int8))) as a )full outer join (select cast([(1, 2), (2, 4)] as Array(Tuple(Int8, Int8)) ) as a ) using a );
select MergeStreamStack(a) from( select a from (select cast([(1, 2), (2, 2)] as Array(Tuple(Int, Int))) as a )full outer join (select cast([(1, 2), (2, 4)] as Array(Tuple(Int, Int)) ) as a ) using a );
select MergeStreamStack(a) from( select a from (select cast([(1, 2), (2, 2)] as Array(Tuple(Int64, Int64))) as a )full outer join (select cast([(1, 2), (2, 4)] as Array(Tuple(Int64, Int64)) ) as a ) using a );
select MergeStreamStack(a) from( select a from (select cast([(1, 2), (2, 2)] as Array(Tuple(Int8, Int64))) as a )full outer join (select cast([(1, 2), (2, 4)] as Array(Tuple(Int8, Int64)) ) as a ) using a );
select MergeStreamStack(a) from( select a from (select cast([(1, 2), (2, 2)] as Array(Tuple(Int8, Float32))) as a )full outer join (select cast([(1, 2), (2, 4)] as Array(Tuple(Int8, Float32)) ) as a ) using a );
select MergeStreamStack(a) from( select a from (select cast([(1, 2), (2, 2)] as Array(Tuple(Int64, Float64))) as a )full outer join (select cast([(1, 2), (2, 4)] as Array(Tuple(Int64, Float64)) ) as a ) using a );
select MergeStreamStack(a) from( select a from (select cast([(1, 2), (2, 2)] as Array(Tuple(Int, Int))) as a )full outer join (select cast([(1, 2), (3, 4)] as Array(Tuple(Int, Int)) ) as a ) using a ); -- { serverError 36 }
select MergeStreamStack(a) from( select a from (select cast([(1, 2), (2, 2)] as Array(Tuple(Int, Int))) as a )full outer join (select cast([(1, 2), (2, 4), (3, 4)] as Array(Tuple(Int, Int)) ) as a ) using a ); -- { serverError 190 }
