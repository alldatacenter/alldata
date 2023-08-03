drop table if exists test.test_bitmapjoinandcard;

create table test.test_bitmapjoinandcard (tag_id Int32, tag_value String, id_map BitMap64, p_date Date, split_id Int32) engine = CnchMergeTree() partition by (p_date, split_id) order by (p_date, split_id, tag_id, tag_value) settings index_granularity = 128;

insert into table test.test_bitmapjoinandcard values (1, 'a', [1], '2020-01-01', 1);
insert into table test.test_bitmapjoinandcard values (1, 'b', [1,2], '2020-01-01', 1);
insert into table test.test_bitmapjoinandcard values (2, 'a', [2,3], '2020-01-01', 1);
insert into table test.test_bitmapjoinandcard values (3, 'b', [2,3,4], '2020-01-01', 1);
insert into table test.test_bitmapjoinandcard values (2, 'a', [1,2,3,4], '2020-01-01', 1);
insert into table test.test_bitmapjoinandcard values (3, 'a', [1,2,3], '2020-01-01', 2);
insert into table test.test_bitmapjoinandcard values (3, 'a', [1,2,3,4], '2020-01-01', 2);
insert into table test.test_bitmapjoinandcard values (4, 'c', [3], '2020-01-01', 2);
insert into table test.test_bitmapjoinandcard values (5, 'c', [1,4], '2020-01-01', 2);
insert into table test.test_bitmapjoinandcard values (4, 'a', [1], '2020-01-01', 3);
insert into table test.test_bitmapjoinandcard values (4, 'b', [1,2], '2020-01-01', 3);
insert into table test.test_bitmapjoinandcard values (5, 'b', [2], '2020-01-01', 3);
insert into table test.test_bitmapjoinandcard values (3, 'c', [1,2,3], '2020-01-01', 3);

select tupleElement(resTuples, 2) as split_id, tupleElement(resTuples, 4) as tag_bin, sum(tupleElement(resTuples, 1)) as count
from(
    select split_id, arrayJoin(BitMapJoinAndCard(1, 1)(id_map, position, split_id, tag_bin)) as resTuples
    from
    (
        select id_map, 1 as position, split_id, '#-1#' as tag_bin
        from test.test_bitmapjoinandcard
        where p_date = '2020-01-01' and tag_value = 'a'
        union all
        select id_map, 2 as position, split_id, multiIf(tag_value = 'b', 'b', tag_value = 'c', 'c', 'others') as tag_bin
        from test.test_bitmapjoinandcard
        where (p_date = '2020-01-01' and tag_value = 'b') or (p_date = '2020-01-01' and tag_value = 'c')
    )
    group by split_id
)
group by tupleElement(resTuples, 2), tupleElement(resTuples, 4)
order by split_id, tag_bin, count desc;

select tupleElement(resTuples, 2) as split_id, tupleElement(resTuples, 4) as tag_bin, tupleElement(resTuples, 5) as tag_bin_1, sum(tupleElement(resTuples, 1)) count
from(
    select split_id, arrayJoin(BitMapJoinAndCard(2, 4)(id_map, position, split_id, tag_bin)) as resTuples
    from
    (
        select id_map, 1 as position, split_id, '#-1#' as tag_bin
        from test.test_bitmapjoinandcard
        where p_date = '2020-01-01' and tag_value = 'a'
        union all
        select id_map, 2 as position, split_id, multiIf(tag_value = 'b', 'b', tag_value = 'c', 'c', 'others') as tag_bin
        from test.test_bitmapjoinandcard
        where (p_date = '2020-01-01' and tag_value = 'b') or (p_date = '2020-01-01' and tag_value = 'c')
        union all
        select id_map, 3 as position, split_id, multiIf(p_date = '2020-01-01' and tag_id = 1, '1', p_date = '2020-01-01' and tag_id = 4, '4','others') as tag_bin
        from test.test_bitmapjoinandcard
        where (p_date = '2020-01-01' and tag_id = 1) or (p_date = '2020-01-01' and tag_id = 4)
    )
    group by split_id
)
group by tupleElement(resTuples, 2), tupleElement(resTuples, 4) as tag_bin, tupleElement(resTuples, 5) as tag_bin_1
order by split_id, tag_bin, tag_bin_1, count desc;

select tupleElement(resTuples, 2) as split_id, tupleElement(resTuples, 4) as tag_bin, tupleElement(resTuples, 5) as seg_name, sum(tupleElement(resTuples, 1)) count
from(
    select split_id, arrayJoin(BitMapJoinAndCard(2, 4)(id_map, position, split_id, tag_bin, seg_name)) as resTuples
    from
    (
        select id_map, 1 as position, split_id, '#-1#' as tag_bin, 'a' as seg_name
        from test.test_bitmapjoinandcard
        where p_date = '2020-01-01' and tag_value = 'a'
        union all
        select id_map, 2 as position, split_id, multiIf(tag_value = 'b', 'b', tag_value = 'c', 'c', 'others') as tag_bin, '#-1#' as seg_name
        from test.test_bitmapjoinandcard
        where (p_date = '2020-01-01' and tag_value = 'b') or (p_date = '2020-01-01' and tag_value = 'c')
        union all
        select id_map, 3 as position, split_id, multiIf(p_date = '2020-01-01' and tag_id = 1, '1', p_date = '2020-01-01' and tag_id = 4, '4', 'others') as tag_bin, '#-1#' as seg_name
        from test.test_bitmapjoinandcard
        where (p_date = '2020-01-01' and tag_id = 1) or (p_date = '2020-01-01' and tag_id = 4)
    )
    group by split_id
)
group by tupleElement(resTuples, 2), tupleElement(resTuples, 4), tupleElement(resTuples, 5)
order by split_id, tag_bin, seg_name, count desc;


select tupleElement(resTuples, 2) as split_id, tupleElement(resTuples, 4) as tag_bin, sum(tupleElement(resTuples, 1)) as count
from(
    select split_id, arrayJoin(BitMapJoinAndCard2(1, 1)(id_map, position, split_id, tag_bin)) as resTuples
    from
    (
        select id_map, 1 as position, split_id, '#-1#' as tag_bin
        from test.test_bitmapjoinandcard
        where p_date = '2020-01-01' and tag_value = 'a'
        union all
        select id_map, 2 as position, split_id, multiIf(tag_value = 'b', 'b', tag_value = 'c', 'c', 'others') as tag_bin
        from test.test_bitmapjoinandcard
        where (p_date = '2020-01-01' and tag_value = 'b') or (p_date = '2020-01-01' and tag_value = 'c')
    )
    group by split_id
)
group by tupleElement(resTuples, 2), tupleElement(resTuples, 4)
order by split_id, tag_bin, count desc;

select tupleElement(resTuples, 2) as split_id, tupleElement(resTuples, 4) as tag_bin, tupleElement(resTuples, 5) as tag_bin_1, sum(tupleElement(resTuples, 1)) count
from(
    select split_id, arrayJoin(BitMapJoinAndCard2(2, 4)(id_map, position, split_id, tag_bin)) as resTuples
    from
    (
        select id_map, 1 as position, split_id, '#-1#' as tag_bin
        from test.test_bitmapjoinandcard
        where p_date = '2020-01-01' and tag_value = 'a'
        union all
        select id_map, 2 as position, split_id, multiIf(tag_value = 'b', 'b', tag_value = 'c', 'c', 'others') as tag_bin
        from test.test_bitmapjoinandcard
        where (p_date = '2020-01-01' and tag_value = 'b') or (p_date = '2020-01-01' and tag_value = 'c')
        union all
        select id_map, 3 as position, split_id, multiIf(p_date = '2020-01-01' and tag_id = 1, '1', p_date = '2020-01-01' and tag_id = 4, '4','others') as tag_bin
        from test.test_bitmapjoinandcard
        where (p_date = '2020-01-01' and tag_id = 1) or (p_date = '2020-01-01' and tag_id = 4)
    )
    group by split_id
)
group by tupleElement(resTuples, 2), tupleElement(resTuples, 4), tupleElement(resTuples, 5)
order by split_id, tag_bin, tag_bin_1, count desc;

select tupleElement(resTuples, 2) as split_id, tupleElement(resTuples, 4) as tag_bin, tupleElement(resTuples, 6) as seg_name, sum(tupleElement(resTuples, 1)) count
from(
    select split_id, arrayJoin(BitMapJoinAndCard2(2, 4)(id_map, position, split_id, tag_bin, seg_name)) as resTuples
    from
    (
        select id_map, 1 as position, split_id, '#-1#' as tag_bin, 'a' as seg_name
        from test.test_bitmapjoinandcard
        where p_date = '2020-01-01' and tag_value = 'a'
        union all
        select id_map, 2 as position, split_id, multiIf(tag_value = 'b', 'b', tag_value = 'c', 'c', 'others') as tag_bin, '#-1#' as seg_name
        from test.test_bitmapjoinandcard
        where (p_date = '2020-01-01' and tag_value = 'b') or (p_date = '2020-01-01' and tag_value = 'c')
        union all
        select id_map, 3 as position, split_id, multiIf(p_date = '2020-01-01' and tag_id = 1, '1', p_date = '2020-01-01' and tag_id = 4, '4', 'others') as tag_bin, '#-1#' as seg_name
        from test.test_bitmapjoinandcard
        where (p_date = '2020-01-01' and tag_id = 1) or (p_date = '2020-01-01' and tag_id = 4)
    )
    group by split_id
)
group by tupleElement(resTuples, 2), tupleElement(resTuples, 4), tupleElement(resTuples, 6)
order by split_id, tag_bin, seg_name, count desc;



drop table if exists test.test_bitmapjoinandcard;
drop table if exists test.test_bitmapjoinandcard;
