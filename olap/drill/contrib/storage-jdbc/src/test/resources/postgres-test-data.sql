

CREATE TABLE caseSensitiveTable (
  a   bytea
);

INSERT INTO caseSensitiveTable (a) values ('this is a test');

CREATE SEQUENCE person_seq;

CREATE TYPE enum_test AS ENUM ('XXX', 'YYY', 'ZZZ');

CREATE TABLE person
(
  person_id         INT                        NOT NULL DEFAULT NEXTVAL('person_seq') PRIMARY KEY,

  first_name        VARCHAR(255),
  last_name         VARCHAR(255),
  address           VARCHAR(255),
  city              VARCHAR(255),
  state             CHAR(2),
  zip               INT,

  json              VARCHAR(255),

  bigint_field      BIGINT,
  smallint_field    SMALLINT,
  numeric_field     NUMERIC(10, 2),
  boolean_field     BOOLEAN,
  double_field      DOUBLE PRECISION,
  float_field       DOUBLE PRECISION,
  real_field        REAL,

  time_field        TIME(0),
  timestamp_field   TIMESTAMP(0),
  date_field        DATE,
  datetime_field    TIMESTAMP(0),
  year_field        INT,

  text_field        TEXT,
  tiny_text_field   TEXT,
  medium_text_field TEXT,
  long_text_field   TEXT,
  blob_field        BYTEA,
  bit_field         BOOLEAN,
  decimal_field     DECIMAL(15, 2),

  enum_field       enum_test
);

INSERT INTO person (first_name, last_name, address, city, state, zip, bigint_field, smallint_field, numeric_field,
                        boolean_field, double_field, float_field, real_field,
                        time_field, timestamp_field, date_field, datetime_field, year_field,
                        json,
                        text_field, tiny_text_field, medium_text_field, long_text_field, blob_field, bit_field,
                        decimal_field, enum_field)
VALUES ('first_name_1', 'last_name_1', '1401 John F Kennedy Blvd', 'Philadelphia', 'PA', 19107, 123456789, 1, 10.01,
        false, 1.0, 1.1, 1.2,
                        '13:00:01', '2012-02-29 13:00:01', '2012-02-29', '2012-02-29 13:00:01', 2015,
                        '{ a : 5, b : 6 }',
                        'It is a long established fact that a reader will be distracted by the readable content of a page when looking at its layout', 'xxx',
                        'a medium piece of text', 'a longer piece of text this is going on.....',
                        'this is a test',
                        true,
                        123.321, 'XXX');

INSERT INTO person (first_name, last_name, address, city, state, zip, bigint_field, smallint_field, numeric_field,
                    boolean_field, double_field, float_field, real_field,
                    time_field, timestamp_field, date_field, datetime_field, year_field,
                    json,
                    text_field, tiny_text_field, medium_text_field, long_text_field, blob_field, bit_field,
                    enum_field)
VALUES ('first_name_2', 'last_name_2', 'One Ferry Building', 'San Francisco', 'CA', 94111, 45456767, 3, 30.04,
        true, 3.0, 3.1, 3.2,
        '11:34:21', '2011-10-30 11:34:21', '2011-10-30', '2011-10-30 11:34:21', '2015',
        '{ z : [ 1, 2, 3 ] }',
        'It is a long established fact that a reader will be distracted by the readable content of a page when looking at its layout', 'abc',
        'a medium piece of text 2', 'somewhat more text',
        'this is a test 2',
        false,
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
        true,
        'ZZZ');

INSERT INTO person (person_id) VALUES (5);

CREATE VIEW person_view AS SELECT * FROM person;
