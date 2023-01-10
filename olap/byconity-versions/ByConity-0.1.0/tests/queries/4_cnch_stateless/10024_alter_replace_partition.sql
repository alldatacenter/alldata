
drop table if exists replace_dst;
drop table if exists replace_src;

create table if not exists replace_dst (date Date, id UInt64) Engine = CnchMergeTree partition by date order by id;

insert into replace_dst values ('2019-01-01', 1);
insert into replace_dst values ('2019-01-02', 1);
insert into replace_dst values ('2019-01-03', 1);

create table if not exists replace_src as replace_dst;

insert into replace_src select * from replace_dst;

select count() from system.cnch_parts where database = currentDatabase() and table = 'replace_dst' and active;

alter table replace_dst replace partition id '20190101' from replace_src;
select count() from system.cnch_parts where database = currentDatabase() and table = 'replace_dst' and active;
select count() from system.cnch_parts where database = currentDatabase() and table = 'replace_src' and active;

alter table replace_dst replace partition where date = '2019-01-02' from replace_src;
select count() from system.cnch_parts where database = currentDatabase() and table = 'replace_dst' and active;
select count() from system.cnch_parts where database = currentDatabase() and table = 'replace_src' and active;

alter table replace_dst replace partition where date > '2019-01-02' from replace_src;
select count() from system.cnch_parts where database = currentDatabase() and table = 'replace_dst' and active;
select count() from system.cnch_parts where database = currentDatabase() and table = 'replace_src' and active;

drop table if exists replace_dst;
drop table if exists replace_src;
