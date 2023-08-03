drop table if exists test.view_test_join_view;
drop table if exists test.test_join_view;

create table test.test_join_view (p_date Date, id Int32, event String) engine = CnchMergeTree partition by p_date order by id;
create VIEW test.view_test_join_view (p_date Date, id Int32) AS select p_date, id from test.test_join_view as a join test.test_join_view as b on a.id = b.id;

insert into test.test_join_view values ('2021-01-01', 1, 'a');

select * from test.view_test_join_view as a join test.view_test_join_view as b on a.id = b.id;

drop table if exists test.view_test_join_view;
drop table if exists test.test_join_view;