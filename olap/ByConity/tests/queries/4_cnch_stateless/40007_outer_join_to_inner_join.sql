CREATE DATABASE IF NOT EXISTS test;

DROP TABLE IF EXISTS people;
DROP TABLE IF EXISTS city;

CREATE TABLE people(`id` Int32, `name` String, `city_id` Int32)
    ENGINE = CnchMergeTree()
    PARTITION BY `id`
    PRIMARY KEY `id`
    ORDER BY `id`
    SETTINGS index_granularity = 8192;

CREATE TABLE city(`id` Int32, `name` String)
    ENGINE = CnchMergeTree()
    PARTITION BY `id`
    PRIMARY KEY `id`
    ORDER BY `id`
    SETTINGS index_granularity = 8192;

INSERT INTO city values (1, 'ct1') (2, 'ct2') (3, 'ct3');
INSERT INTO people values (1001, 'pe1', 1) (1002, 'pe2', 1) (1003, 'pe3', 2) (1004, 'pe4', 100);

SELECT '--case 1';
SET join_use_nulls = 1;
select * from people p left join city c on p.city_id = c.id where c.id > -1 order by p.id;
SELECT '--case 2';
SET join_use_nulls = 1;
select * from people p left join city c on p.city_id = c.id where p.name != 'foo' and c.id > -1 and rand(1) >100 order by p.id;
SELECT '--case 3';
SET join_use_nulls = 1;
select * from people p left join city c on p.city_id = c.id where p.id > -1 order by p.id;
SELECT '--case 4';
SET join_use_nulls = 1;
select * from people p right join city c on p.city_id = c.id where p.id > -1 order by p.id;
SELECT '--case 5';
SET join_use_nulls = 1;
select * from people p full join city c on p.city_id = c.id where p.id > -1 order by p.id;
SELECT '--case 6';
SET join_use_nulls = 1;
select * from people p full join city c on p.city_id = c.id where c.id > -1 order by p.id;
SELECT '--case 7';
SET join_use_nulls = 1;
select * from people p full join city c on p.city_id = c.id where p.id > -1 and c.id > -1 order by p.id;
SELECT '--case 8';
SET join_use_nulls = 1;
select c.id, c.name from city c any left join people p on p.city_id = c.id where p.id > -1 order by c.id;
SELECT '--case 9';
SET join_use_nulls = 0;
select * from people p left join city c on p.city_id = c.id where c.id > -1 order by p.id;
SELECT '--case 10';
SET join_use_nulls = 0;
select * from people p left join city c on p.city_id = c.id where c.id > 0 order by p.id;
SELECT '--case 11';
SET join_use_nulls = 1;
select
    people_id
from (
         select
             p.id as people_id,
             c.id + rand() % 2 as city_id
         from people p
             left join city c
         on p.city_id = c.id
     )
where city_id > 0
order by people_id;

SELECT '--case 12';
DROP TABLE IF EXISTS country;
DROP TABLE IF EXISTS state;
DROP TABLE IF EXISTS city;

CREATE TABLE country(`id` Int32, `name` String)
    ENGINE = CnchMergeTree()
    PARTITION BY `id`
    PRIMARY KEY `id`
    ORDER BY `id`
    SETTINGS index_granularity = 8192;

CREATE TABLE state(`id` Int32, `name` String, `country_id` Int32)
    ENGINE = CnchMergeTree()
    PARTITION BY `id`
    PRIMARY KEY `id`
    ORDER BY `id`
    SETTINGS index_granularity = 8192;

CREATE TABLE city(`id` Int32, `name` String, `state_id` Int32)
    ENGINE = CnchMergeTree()
    PARTITION BY `id`
    PRIMARY KEY `id`
    ORDER BY `id`
    SETTINGS index_granularity = 8192;

INSERT INTO country values (1, 'co1') (2, 'co2') (3, 'co3');
INSERT INTO state values (101, 'st1', 1) (102, 'st2', 1) (103, 'st3', 2) (104, 'some_state', 4);
INSERT INTO city values (1001, 'ct3', 101) (1002, 'ct4', 101) (1003, 'ct5', 103) (1004, 'some_city', 109);

SET join_use_nulls = 1;

select
    country.id as co_id,
    country.name as co_name,
    s_id,
    s_name,
    s_country_id,
    c_id,
    c_name,
    c_state_id
from (
         select
             state.id as s_id,
             state.name as s_name,
             state.country_id as s_country_id,
             city.id as c_id,
             city.name as c_name,
             city.state_id as c_state_id
         from state
                  left join city
                            on state.id = city.state_id
     ) t
         right join country on country.id = t.s_country_id
where c_id > 0
order by country.id, t.s_id, t.c_id;

SELECT '--case 13';
DROP TABLE IF EXISTS people;
DROP TABLE IF EXISTS company;
DROP TABLE IF EXISTS city;
DROP TABLE IF EXISTS state;

CREATE TABLE people(`id` Int32, `name` String, `company_id` Int32, `state_id` Int32, `city_id` Int32)
    ENGINE = CnchMergeTree()
    PARTITION BY `id`
    PRIMARY KEY `id`
    ORDER BY `id`
    SETTINGS index_granularity = 8192;

CREATE TABLE company(`id` Int32, `name` String)
    ENGINE = CnchMergeTree()
    PARTITION BY `id`
    PRIMARY KEY `id`
    ORDER BY `id`
    SETTINGS index_granularity = 8192;

CREATE TABLE city(`id` Int32, `name` String, `state_id` Int32)
    ENGINE = CnchMergeTree()
    PARTITION BY `id`
    PRIMARY KEY `id`
    ORDER BY `id`
    SETTINGS index_granularity = 8192;

CREATE TABLE state(`id` Int32, `name` String)
    ENGINE = CnchMergeTree()
    PARTITION BY `id`
    PRIMARY KEY `id`
    ORDER BY `id`
    SETTINGS index_granularity = 8192;

INSERT INTO people values (1001, 'p1', 2001, 4001, 3001) (1002, 'p2', 2002, 4001, 3001) (1003, 'p3', 2001, 4002, 3003) (1004, 'p4', 2002, 4019, 3018) (1005, 'p5', 2009, 4001, 3002);
INSERT INTO company values (2001, 'comp1') (2002, 'comp2') (2003, 'comp3');
INSERT INTO city values (3001, 'c1', 4001) (3002, 'c2', 4001) (3003, 'c3', 4002) (3004, 'c4', 4003) (3005, 'c5', 4009);
INSERT INTO state values (4001, 's1') (4002, 's2') (4003, 's3') (4004, 's4') (0, 'xx');

SET join_use_nulls = 1;

select
    x.p_id,
    x.p_comp_id,
    x.p_c_id,
    x.p_s_id,
    x.comp_id,
    y.s_id,
    y.c_id,
    y.c_s_id
from (
         select
             p.id as p_id,
             p.company_id as p_comp_id,
             p.city_id as p_c_id,
             p.state_id as p_s_id,
             comp.id as comp_id
         from company comp
                  left join people p
                            on p.company_id = comp.id
     ) x
         join (
    select
        s.id as s_id,
        c.id as c_id,
        c.state_id as c_s_id
    from state s
             full outer join city c
                             on s.id = c.state_id
) y
              on x.p_s_id = y.s_id and x.p_c_id = y.c_id
order by x.p_id, x.p_comp_id, x.p_c_id, x.p_s_id, x.comp_id, y.s_id, y.c_id, y.c_s_id;

SELECT '--case 14';
SET join_use_nulls = 0;

select
    x.p_id,
    x.p_comp_id,
    x.p_c_id,
    x.p_s_id,
    x.comp_id,
    y.s_id,
    y.c_id,
    y.c_s_id
from (
         select
             p.id as p_id,
             p.company_id as p_comp_id,
             p.city_id as p_c_id,
             p.state_id as p_s_id,
             comp.id as comp_id
         from company comp
                  left join people p
                            on p.company_id = comp.id
     ) x
         join (
    select
        s.id as s_id,
        c.id as c_id,
        c.state_id as c_s_id
    from state s
             full outer join city c
                             on s.id = c.state_id
) y
              on x.p_s_id = y.s_id and x.p_c_id = y.c_id
order by x.p_id, x.p_comp_id, x.p_c_id, x.p_s_id, x.comp_id, y.s_id, y.c_id, y.c_s_id;

SELECT '--case 15';
SET join_use_nulls = 1;

select
    x.p_id,
    x.p_comp_id,
    x.p_c_id,
    x.p_s_id,
    x.comp_id,
    y.s_id,
    y.c_id,
    y.c_s_id
from (
         select
             p.id as p_id,
             p.company_id as p_comp_id,
             p.city_id as p_c_id,
             p.state_id as p_s_id,
             comp.id as comp_id
         from company comp
                  left join people p
                            on p.company_id = comp.id
     ) x
         left join (
    select
        s.id as s_id,
        c.id as c_id,
        c.state_id as c_s_id
    from state s
             full outer join city c
                             on s.id = c.state_id
) y
                   on x.p_s_id = y.s_id and x.p_c_id = y.c_id
order by x.p_id, x.p_comp_id, x.p_c_id, x.p_s_id, x.comp_id, y.s_id, y.c_id, y.c_s_id;
