
create SCHEMA drill_h2_test;
set schema drill_h2_test;

create table person (
  person_id       INT NOT NULL PRIMARY KEY,

  first_name      VARCHAR(255),
  last_name       VARCHAR(255),
  address         VARCHAR(255),
  city            VARCHAR(255),
  state           CHAR(2),
  zip             INT,

  json            VARCHAR(255),

  bigint_field    BIGINT,
  smallint_field  SMALLINT,
  numeric_field   NUMERIC(20, 2),
  boolean_field   BOOLEAN,
  double_field    DOUBLE,
  float_field     FLOAT,
  real_field      REAL,

  time_field      TIME,
  timestamp_field TIMESTAMP,
  date_field      DATE,

  clob_field      CLOB
);

insert into person (person_id, first_name, last_name, address, city, state, zip, json, bigint_field, smallint_field, numeric_field,
                    boolean_field, double_field, float_field, real_field, time_field, timestamp_field, date_field,
                    clob_field)
  values (1, 'first_name_1', 'last_name_1', '1401 John F Kennedy Blvd', 'Philadelphia', 'PA', 19107,
          '{ a : 5, b : 6 }', 123456, 1, 10.01, false, 1.0, 1.1, 111.00, '13:00:01', '2012-02-29 13:00:01',
          '2012-02-29', 'some clob data 1');

insert into person (person_id, first_name, last_name, address, city, state, zip, json, bigint_field, smallint_field, numeric_field,
                    boolean_field, double_field, float_field, real_field, time_field, timestamp_field, date_field, clob_field)
  values (2, 'first_name_2', 'last_name_2', 'One Ferry Building', 'San Francisco', 'CA', 94111,
          '{ foo : "abc" }', 95949, 2, 20.02, true, 2.0, 2.1, 222.00, '23:59:59', '1999-09-09 23:59:59', '1999-09-09',
          'some more clob data');

insert into person (person_id, first_name, last_name, address, city, state, zip, json, bigint_field, smallint_field, numeric_field,
                    boolean_field, double_field, float_field, real_field, time_field, timestamp_field, date_field, clob_field)
  values (3, 'first_name_3', 'last_name_3', '176 Bowery', 'New York', 'NY', 10012,
          '{ z : [ 1, 2, 3 ] }', 45456, 3, 30.04, true, 3.0, 3.1, 333.00, '11:34:21', '2011-10-30 11:34:21',
          '2011-10-30', 'clobber');

insert into person (person_id, first_name, last_name, address, city, state, zip, json, bigint_field, smallint_field, numeric_field,
                    boolean_field, double_field, float_field, real_field, time_field, timestamp_field, date_field, clob_field)
  values (4, NULL, NULL, '2 15th St NW', 'Washington', 'DC', 20007,
          '{ z : { a : 1, b : 2, c : 3 } }', -67, 4, 40.04, false, 4.0, 4.1, 444.00, '16:00:01',
          '2015-06-01 16:00:01', '2015-06-01', 'xxx');

insert into person (person_id) values (5);

create SCHEMA drill_h2_test_1;
set schema drill_h2_test_1;
create table person(person_id INT NOT NULL PRIMARY KEY);
set schema drill_h2_test;
