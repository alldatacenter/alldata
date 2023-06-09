drop table if exists test.test_data;

create table test.test_data (tag String, uids BitMap64) Engine=CnchMergeTree() order by tag;

insert into test.test_data values ('a', [1,2,3,4,5,6,7,8,9,10]);
insert into test.test_data values ('b', [1,2,3,4,5,6,7,8]);
insert into test.test_data values ('c', [1,2,3,4,5,6]);
insert into test.test_data values ('d', [1,2,3,4]);
insert into test.test_data values ('e', [1,2]);

select bitmapMaxLevel(level, uids)
from (
    select
        multiIf(tag='a', 1, tag='b', 2, tag='c', 3, tag='d', 4, tag='e', 5, -1) as level,
        uids
    from test.test_data
);

select bitmapMaxLevel(1)(level, uids)
from (
    select
        multiIf(tag='a', 1, tag='b', 2, tag='c', 3, tag='d', 4, tag='e', 5, -1) as level,
        uids
    from test.test_data
);

select bitmapMaxLevel(2)(level, uids)
from (
    select
        multiIf(tag='a', 1, tag='b', 2, tag='c', 3, tag='d', 4, tag='e', 5, -1) as level,
        uids
    from test.test_data
);

drop table test.test_data;


---- level is int in the table
create table test.test_data (level Int32, uids BitMap64) Engine=CnchMergeTree() order by level;

insert into test.test_data values (-2, [1,2,3,4,5,6,7,8,9,10]);
insert into test.test_data values (-1, [1,2,3,4,5,6,7,8]);
insert into test.test_data values (1, [1,2,3,4,5,6]);
insert into test.test_data values (2, [1,2,3,4]);
insert into test.test_data values (3, [1,2]);

select bitmapMaxLevel(level, uids) from test.test_data;

select bitmapMaxLevel(1)(level, uids)
from (
    select
        level,
        uids
    from test.test_data
);

select bitmapMaxLevel(2)(level, uids)
from (
    select
        level,
        uids
    from test.test_data
);

drop table test.test_data;