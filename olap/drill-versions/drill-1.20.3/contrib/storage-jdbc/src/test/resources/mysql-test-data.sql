use drill_mysql_test;

create table caseSensitiveTable (
  a   BLOB
);

insert into caseSensitiveTable (a) values ('this is a test');

create table person (
  person_id       INT NOT NULL AUTO_INCREMENT PRIMARY KEY,

  first_name      VARCHAR(255),
  last_name       VARCHAR(255),
  address         VARCHAR(255),
  city            VARCHAR(255),
  state           CHAR(2),
  zip             INT,

  json            VARCHAR(255),

  bigint_field    BIGINT,
  smallint_field  SMALLINT,
  numeric_field   NUMERIC(10, 2),
  boolean_field   BOOLEAN,
  double_field    DOUBLE,
  float_field     FLOAT,
  real_field      REAL,

  time_field      TIME,
  timestamp_field TIMESTAMP,
  date_field      DATE,
  datetime_field  DATETIME,
  year_field      YEAR(4),

  text_field      TEXT,
  tiny_text_field TINYTEXT,
  medium_text_field MEDIUMTEXT,
  long_text_field LONGTEXT,
  blob_field      BLOB,
  bit_field       BIT,
  decimal_field   DECIMAL(15, 2),

  enum_field      ENUM('XXX', 'YYY', 'ZZZ') NOT NULL
);

insert into person (first_name, last_name, address, city, state, zip, bigint_field, smallint_field, numeric_field,
                    boolean_field, double_field, float_field, real_field,
                    time_field, timestamp_field, date_field, datetime_field, year_field,
                    json,
                    text_field, tiny_text_field, medium_text_field, long_text_field, blob_field, bit_field,
                    decimal_field, enum_field)
    values ('first_name_1', 'last_name_1', '1401 John F Kennedy Blvd', 'Philadelphia', 'PA', 19107, 123456789, 1, 10.01,
            false, 1.0, 1.1, 1.2,
            '13:00:01', '2012-02-29 13:00:01', '2012-02-29', '2012-02-29 13:00:01', 2015,
            '{ a : 5, b : 6 }',
            'It is a long established fact that a reader will be distracted by the readable content of a page when looking at its layout', 'xxx',
            'a medium piece of text', 'a longer piece of text this is going on.....',
            'this is a test',
            1,
            123.321, 'XXX');

insert into person (first_name, last_name, address, city, state, zip, bigint_field, smallint_field, numeric_field,
                    boolean_field, double_field, float_field, real_field,
                    time_field, timestamp_field, date_field, datetime_field, year_field,
                    json,
                    text_field, tiny_text_field, medium_text_field, long_text_field, blob_field, bit_field,
                    enum_field)
    values ('first_name_2', 'last_name_2', 'One Ferry Building', 'San Francisco', 'CA', 94111, 45456767, 3, 30.04,
            true, 3.0, 3.1, 3.2,
            '11:34:21', '2011-10-30 11:34:21', '2011-10-30', '2011-10-30 11:34:21', '2015',
            '{ z : [ 1, 2, 3 ] }',
            'It is a long established fact that a reader will be distracted by the readable content of a page when looking at its layout', 'abc',
            'a medium piece of text 2', 'somewhat more text',
            'this is a test 2',
            0,
            'YYY');

insert into person (first_name, last_name, address, city, state, zip, bigint_field, smallint_field, numeric_field,
                    boolean_field, double_field, float_field, real_field,
                    time_field, timestamp_field, date_field, datetime_field, year_field,
                    json,
                    text_field, tiny_text_field, medium_text_field, long_text_field, blob_field, bit_field,
                    enum_field)
    values ('first_name_3', 'last_name_3', '176 Bowery', 'New York', 'NY', 10012, 123090, -3, 55.12,
            false, 5.0, 5.1, 5.55,
            '16:00:01', '2015-06-02 10:01:01', '2015-06-01', '2015-09-22 15:46:10', 1901,
            '{ [ a, b, c ] }',
            'Neque porro quisquam est qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit', 'abc',
            'a medium piece of text 3', 'somewhat more text',
            'this is a test 3',
            1,
            'ZZZ');

insert into person (person_id) values (5);

create view person_view as select * from person;
