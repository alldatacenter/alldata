
select 1;
select AppVersionCompare('10.0.0.1', '10.0.0.1', '=');
select AppVersionCompare('10.0.0.1', '10.0.0.1', '!=');
select AppVersionCompare('10.0.0.1', '10.0.0.1', '>=');
select AppVersionCompare('10.0.0.1', '10.0.0.1', '<=');
select AppVersionCompare('10.0.0.1', '10.0.0.1', '>');
select AppVersionCompare('10.0.0.1', '10.0.0.1', '<');
select AppVersionCompare('10.0.0.1', '10.0.0.1', '');

select 2;
select AppVersionCompare('10.0.0.1', '10.0.0', '=');
select AppVersionCompare('10.0.0.1', '10.0.0', '!=');
select AppVersionCompare('10.0.0.1', '10.0.0', '>=');
select AppVersionCompare('10.0.0.1', '10.0.0', '<=');
select AppVersionCompare('10.0.0.1', '10.0.0', '>');
select AppVersionCompare('10.0.0.1', '10.0.0', '<');
select AppVersionCompare('10.0.0.1', '10.0.0', '');

select 3;
select AppVersionCompare('10.0.0.1', '10.0.', '=');
select AppVersionCompare('10.0.0.1', '10.0.', '!=');
select AppVersionCompare('10.0.0.1', '10.0.', '>=');
select AppVersionCompare('10.0.0.1', '10.0.', '<=');
select AppVersionCompare('10.0.0.1', '10.0.', '>');
select AppVersionCompare('10.0.0.1', '10.0.', '<');
select AppVersionCompare('10.0.0.1', '10.0.', '');

select 4;
select AppVersionCompare('10.0.0', '10.0.0.1', '=');
select AppVersionCompare('10.0.0', '10.0.0.1', '!=');
select AppVersionCompare('10.0.0', '10.0.0.1', '>=');
select AppVersionCompare('10.0.0', '10.0.0.1', '<=');
select AppVersionCompare('10.0.0', '10.0.0.1', '>');
select AppVersionCompare('10.0.0', '10.0.0.1', '<');
select AppVersionCompare('10.0.0', '10.0.0.1', '');

select 5;
select AppVersionCompare('10.0.', '10.0.0.1', '=');
select AppVersionCompare('10.0.', '10.0.0.1', '!=');
select AppVersionCompare('10.0.', '10.0.0.1', '>=');
select AppVersionCompare('10.0.', '10.0.0.1', '<=');
select AppVersionCompare('10.0.', '10.0.0.1', '>');
select AppVersionCompare('10.0.', '10.0.0.1', '<');
select AppVersionCompare('10.0.', '10.0.0.1', '');

select 6;
select AppVersionCompare('9.0.1', '10.0.0.1', '=');
select AppVersionCompare('9.0.1', '10.0.0.1', '!=');
select AppVersionCompare('9.0.1', '10.0.0.1', '>=');
select AppVersionCompare('9.0.1', '10.0.0.1', '<=');
select AppVersionCompare('9.0.1', '10.0.0.1', '>');
select AppVersionCompare('9.0.1', '10.0.0.1', '<');
select AppVersionCompare('9.0.1', '10.0.0.1', '');

select 7;
select AppVersionCompare('9.0.1', '', '=');
select AppVersionCompare('9.0.1', '', '!=');
select AppVersionCompare('9.0.1', '', '>=');
select AppVersionCompare('9.0.1', '', '<=');
select AppVersionCompare('9.0.1', '', '>');
select AppVersionCompare('9.0.1', '', '<');
select AppVersionCompare('9.0.1', '', '');

select 8;
select AppVersionCompare('', '9.0.1', '=');
select AppVersionCompare('', '9.0.1', '!=');
select AppVersionCompare('', '9.0.1', '>=');
select AppVersionCompare('', '9.0.1', '<=');
select AppVersionCompare('', '9.0.1', '>');
select AppVersionCompare('', '9.0.1', '<');
select AppVersionCompare('', '9.0.1', '');

select 9;
select AppVersionCompare('', '', '=');
select AppVersionCompare('', '', '!=');
select AppVersionCompare('', '', '>=');
select AppVersionCompare('', '', '<=');
select AppVersionCompare('', '', '>');
select AppVersionCompare('', '', '<');
select AppVersionCompare('', '', '');

select 10;
select AppVersionCompare('9.0.1', '123', '=');
select AppVersionCompare('9.0.1', '123', '!=');
select AppVersionCompare('9.0.1', '123', '>=');
select AppVersionCompare('9.0.1', '123', '<=');
select AppVersionCompare('9.0.1', '123', '>');
select AppVersionCompare('9.0.1', '123', '<');
select AppVersionCompare('9.0.1', '123', '');

select 11;
select AppVersionCompare('123', '123', '=');
select AppVersionCompare('123', '123', '!=');
select AppVersionCompare('123', '123', '>=');
select AppVersionCompare('123', '123', '<=');
select AppVersionCompare('123', '123', '>');
select AppVersionCompare('123', '123', '<');
select AppVersionCompare('123', '123', '');

select 12;
select AppVersionCompare('1234', '123', '=');
select AppVersionCompare('1234', '123', '!=');
select AppVersionCompare('1234', '123', '>=');
select AppVersionCompare('1234', '123', '<=');
select AppVersionCompare('1234', '123', '>');
select AppVersionCompare('1234', '123', '<');
select AppVersionCompare('1234', '123', '');

select 13;
select AppVersionCompare('0', '0', '=');
select AppVersionCompare('0', '0', '!=');
select AppVersionCompare('0', '0', '>=');
select AppVersionCompare('0', '0', '<=');
select AppVersionCompare('0', '0', '>');
select AppVersionCompare('0', '0', '<');
select AppVersionCompare('0', '0', '');

select 14;
select AppVersionCompare('0.7.1', '0.6.0', '=');
select AppVersionCompare('0.7.1', '0.6.0', '!=');
select AppVersionCompare('0.7.1', '0.6.0', '>=');
select AppVersionCompare('0.7.1', '0.6.0', '<=');
select AppVersionCompare('0.7.1', '0.6.0', '>');
select AppVersionCompare('0.7.1', '0.6.0', '<');
select AppVersionCompare('0.7.1', '0.6.0', '');

select 15;
select AppVersionCompare('6.4.0', '6.4', '=');
select AppVersionCompare('6.4.0', '6.4', '!=');
select AppVersionCompare('6.4.0', '6.4', '>=');
select AppVersionCompare('6.4.0', '6.4', '<=');
select AppVersionCompare('6.4.0', '6.4', '>');
select AppVersionCompare('6.4.0', '6.4', '<');
select AppVersionCompare('6.4.0', '6.4', '');

select 16;
select AppVersionCompare('.', '6.4', '=');
select AppVersionCompare('.', '6.4', '!=');
select AppVersionCompare('.', '6.4', '>=');
select AppVersionCompare('.', '6.4', '<=');
select AppVersionCompare('.', '6.4', '>');
select AppVersionCompare('.', '6.4', '<');
select AppVersionCompare('.', '6.4', '');

select 17;
select AppVersionCompare('1.', '1.4', '=');
select AppVersionCompare('1.', '1.4', '!=');
select AppVersionCompare('1.', '1.4', '>=');
select AppVersionCompare('1.', '1.4', '<=');
select AppVersionCompare('1.', '1.4', '>');
select AppVersionCompare('1.', '1.4', '<');
select AppVersionCompare('1.', '1.4', '');

select 18;
select AppVersionCompare('1.', '1.', '=');
select AppVersionCompare('1.', '1.', '!=');
select AppVersionCompare('1.', '1.', '>=');
select AppVersionCompare('1.', '1.', '<=');
select AppVersionCompare('1.', '1.', '>');
select AppVersionCompare('1.', '1.', '<');
select AppVersionCompare('1.', '1.', '');

select 19;
select AppVersionCompare('1.', '1..', '=');
select AppVersionCompare('1.', '1..', '!=');
select AppVersionCompare('1.', '1..', '>=');
select AppVersionCompare('1.', '1..', '<=');
select AppVersionCompare('1.', '1..', '>');
select AppVersionCompare('1.', '1..', '<');
select AppVersionCompare('1.', '1..', '');

select 20;
select AppVersionCompare('.1.', '1..', '=');
select AppVersionCompare('.1.', '1..', '!=');
select AppVersionCompare('.1.', '1..', '>=');
select AppVersionCompare('.1.', '1..', '<=');
select AppVersionCompare('.1.', '1..', '>');
select AppVersionCompare('.1.', '1..', '<');
select AppVersionCompare('.1.', '1..', '');

select '===================';
select 21;
drop table if exists test_app_version;
create table test_app_version (app_version String DEFAULT '') Engine = CnchMergeTree order by app_version;

insert into table test_app_version values ('1.0.0.1');
insert into table test_app_version values ('1.0.0.');
insert into table test_app_version values ('1.0.0');
insert into table test_app_version values ('1.0.');
insert into table test_app_version values ('1.0');
insert into table test_app_version values ('1.');
insert into table test_app_version values ('1');
insert into table test_app_version values ('');

select 22;
insert into table test_app_version values ('1.0.0.1');
insert into table test_app_version values ('1.0.0.');
insert into table test_app_version values ('1.0.0');
insert into table test_app_version values ('1.0.');
insert into table test_app_version values ('1.0');
insert into table test_app_version values ('1.');
insert into table test_app_version values ('1');
insert into table test_app_version values ('');

select 23;
insert into table test_app_version values ('.');
insert into table test_app_version values ('.1');
insert into table test_app_version values ('.1.');
insert into table test_app_version values ('.1.1');
insert into table test_app_version values ('1..');
insert into table test_app_version values ('1..1');
insert into table test_app_version values ('1.1.1');

select 24;
insert into table test_app_version values ('2');
insert into table test_app_version values ('2.');
insert into table test_app_version values ('2.0');
insert into table test_app_version values ('2.0.1');
insert into table test_app_version values ('2.0.1.');
insert into table test_app_version values ('2.0.1.1');

select 25;
insert into table test_app_version values ('1.1');
insert into table test_app_version values ('1.0.1');
insert into table test_app_version values ('1.0.0.2');
insert into table test_app_version values ('1.1.0.2');
insert into table test_app_version values ('1.1.1.2');

select 26;
select * from test_app_version where AppVersionCompare(app_version, '1.0.0.1', '=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.0.1', '!=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.0.1', '>') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.0.1', '>=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.0.1', '<') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.0.1', '<=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.0.1', '') order by app_version;

select 27;
select * from test_app_version where AppVersionCompare(app_version, '1.0.0.', '=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.0.', '!=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.0.', '>') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.0.', '>=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.0.', '<') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.0.', '<=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.0.', '') order by app_version;

select 28;
select * from test_app_version where AppVersionCompare(app_version, '1.0.0', '=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.0', '!=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.0', '>') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.0', '>=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.0', '<') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.0', '<=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.0', '') order by app_version;

select 29;
select * from test_app_version where AppVersionCompare(app_version, '1.0.', '=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.', '!=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.', '>') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.', '>=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.', '<') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.', '<=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0.', '') order by app_version;

select 30;
select * from test_app_version where AppVersionCompare(app_version, '1.0', '=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0', '!=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0', '>') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0', '>=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0', '<') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0', '<=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.0', '') order by app_version;

select 31;
select * from test_app_version where AppVersionCompare(app_version, '1.', '=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.', '!=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.', '>') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.', '>=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.', '<') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.', '<=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1.', '') order by app_version;

select 32;
select * from test_app_version where AppVersionCompare(app_version, '1', '=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1', '!=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1', '>') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1', '>=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1', '<') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1', '<=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '1', '') order by app_version;

select 33;
select * from test_app_version where AppVersionCompare(app_version, '.', '=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '.', '!=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '.', '>') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '.', '>=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '.', '<') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '.', '<=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '.', '') order by app_version;

select 34;
select * from test_app_version where AppVersionCompare(app_version, '', '=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '', '!=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '', '>') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '', '>=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '', '<') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '', '<=') order by app_version;
select * from test_app_version where AppVersionCompare(app_version, '', '') order by app_version;

select 35;
select * from test_app_version where AppVersionCompare( '1.0.0.1', app_version, '=') order by app_version;
select * from test_app_version where AppVersionCompare( '1.0.0.1', app_version, '!=') order by app_version;
select * from test_app_version where AppVersionCompare( '1.0.0.1', app_version, '>') order by app_version;
select * from test_app_version where AppVersionCompare( '1.0.0.1', app_version, '>=') order by app_version;
select * from test_app_version where AppVersionCompare( '1.0.0.1', app_version, '<') order by app_version;
select * from test_app_version where AppVersionCompare( '1.0.0.1', app_version, '<') order by app_version;
select * from test_app_version where AppVersionCompare( '1.0.0.1', app_version, '') order by app_version;

select 36;
select * from test_app_version where AppVersionCompare( '1.0.0.', app_version, '=') order by app_version;
select * from test_app_version where AppVersionCompare( '1.0.0.', app_version, '!=') order by app_version;
select * from test_app_version where AppVersionCompare( '1.0.0.', app_version, '>') order by app_version;
select * from test_app_version where AppVersionCompare( '1.0.0.', app_version, '>=') order by app_version;
select * from test_app_version where AppVersionCompare( '1.0.0.', app_version, '<') order by app_version;
select * from test_app_version where AppVersionCompare( '1.0.0.', app_version, '<') order by app_version;
select * from test_app_version where AppVersionCompare( '1.0.0.', app_version, '') order by app_version;

select 37;
select * from test_app_version where AppVersionCompare( '1.0.0', app_version, '=') order by app_version;
select * from test_app_version where AppVersionCompare( '1.0.0', app_version, '!=') order by app_version;
select * from test_app_version where AppVersionCompare( '1.0.0', app_version, '>') order by app_version;
select * from test_app_version where AppVersionCompare( '1.0.0', app_version, '>=') order by app_version;
select * from test_app_version where AppVersionCompare( '1.0.0', app_version, '<') order by app_version;
select * from test_app_version where AppVersionCompare( '1.0.0', app_version, '<=') order by app_version;
select * from test_app_version where AppVersionCompare( '1.0.0', app_version, '') order by app_version;

drop table test_app_version;


create table test_app_version (version_1 String, version_2 String) Engine = CnchMergeTree order by tuple();
insert into test_app_version values ('', '') ('0', '0') ('.', '6.4') ('1.', '1.') ('1.', '1.4') ('1.', '1..') ('9.0.1', '') ('.1.', '1..') ('123', '123') ('1234', '123') ('6.4', '6.4.') ('9.0.1', '123') ('6.4', '6.4.0') ('6.4.0', '6.4') ('0.7.1', '0.6.0') ('10.0.0.1', '10.0.') ('10.0.', '10.0.0.1') ('9.0.1', '10.0.0.1') ('10.0.0.1', '10.0.0') ('10.0.0.1', '10.0.0.1');

select 38;
select version_1, '=',  version_2, AppVersionCompare(version_1, version_2,  '=') from test_app_version order by version_1, version_2;
select version_1, '!=', version_2, AppVersionCompare(version_1, version_2, '!=') from test_app_version order by version_1, version_2;
select 39;
select version_1, '<',  version_2, AppVersionCompare(version_1, version_2,  '<') from test_app_version order by version_1, version_2;
select version_1, '>',  version_2, AppVersionCompare(version_1, version_2,  '>') from test_app_version order by version_1, version_2;
select 40;
select version_1, '<=', version_2, AppVersionCompare(version_1, version_2, '<=') from test_app_version order by version_1, version_2;
select version_1, '>=', version_2, AppVersionCompare(version_1, version_2, '>=') from test_app_version order by version_1, version_2;

select 41, 'set max_length=2';
select version_1, '=',  version_2, AppVersionCompare(version_1, version_2,  '=', 2) from test_app_version order by version_1, version_2;
select version_1, '!=', version_2, AppVersionCompare(version_1, version_2, '!=', 2) from test_app_version order by version_1, version_2;
select 42, 'set max_length=2';
select version_1, '<',  version_2, AppVersionCompare(version_1, version_2,  '<', 2) from test_app_version order by version_1, version_2;
select version_1, '>',  version_2, AppVersionCompare(version_1, version_2,  '>', 2) from test_app_version order by version_1, version_2;
select 43, 'set max_length=2';
select version_1, '<=', version_2, AppVersionCompare(version_1, version_2, '<=', 2) from test_app_version order by version_1, version_2;
select version_1, '>=', version_2, AppVersionCompare(version_1, version_2, '>=', 2) from test_app_version order by version_1, version_2;

select 44;
select version_1, '=', version_2, AppVersionCompare(version_1, version_2, '=', -1)  from test_app_version order by version_1, version_2;  -- { serverError 43 }
select version_1, '=', version_2, AppVersionCompare(version_1, version_2, '=', 256) from test_app_version order by version_1, version_2;  --{ serverError 43 }
select version_1, '=', version_2, AppVersionCompare(version_1, version_2, '=', 0)   from test_app_version order by version_1, version_2;
select version_1, '=', version_2, AppVersionCompare(version_1, version_2, '=', 255) from test_app_version order by version_1, version_2;

drop table test_app_version;
