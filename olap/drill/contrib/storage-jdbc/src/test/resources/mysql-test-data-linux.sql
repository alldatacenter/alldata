use drill_mysql_test;

create table CASESENSITIVETABLE (
  a   BLOB,
  b   BLOB
);

insert into CASESENSITIVETABLE (a, b) values ('this is a test', 'for case sensitive table names');
