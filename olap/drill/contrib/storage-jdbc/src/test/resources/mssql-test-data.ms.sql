CREATE TABLE person
(
  person_id         INT                        NOT NULL IDENTITY PRIMARY KEY,

  first_name        NVARCHAR(255),
  last_name         NVARCHAR(255),
  address           NVARCHAR(255),
  city              NVARCHAR(255),
  state             NCHAR(2),
  zip               INT,

  json              NVARCHAR(255),

  bigint_field      BIGINT,
  smallint_field    SMALLINT,
  numeric_field     NUMERIC(10, 2),
  double_field      DOUBLE PRECISION,
  float_field       DOUBLE PRECISION,
  real_field        REAL,

  time_field        TIME,
  datetime2_field   DATETIME2,
  date_field        DATE,
  datetime_field    DATETIME,
  year_field        INT,

  blob_field        VARBINARY(255),
  bit_field         BIT,
  decimal_field     DECIMAL(15, 2),
);

INSERT INTO person (first_name, last_name, address, city, state, zip, bigint_field, smallint_field, numeric_field,
                        double_field, float_field, real_field,
                        time_field, datetime2_field, date_field, datetime_field, year_field,
                        json,
                        blob_field, bit_field,
                        decimal_field)
VALUES ('first_name_1', 'last_name_1', '1401 John F Kennedy Blvd', 'Philadelphia', 'PA', 19107, 123456789, 1, 10.01,
         1.0, 1.1, 1.2,
                        '13:00:01', '2012-02-29 13:00:01', '2012-02-29', '2012-02-29 13:00:01', 2015,
                        '{ a : 5, b : 6 }',
                        convert(varbinary, 'this is a test'),
                        1,
                        123.321);

INSERT INTO person (first_name, last_name, address, city, state, zip, bigint_field, smallint_field, numeric_field,
                    double_field, float_field, real_field,
                    time_field, datetime2_field, date_field, datetime_field, year_field,
                    json,
                    blob_field, bit_field)
VALUES ('first_name_2', 'last_name_2', 'One Ferry Building', 'San Francisco', 'CA', 94111, 45456767, 3, 30.04,
        3.0, 3.1, 3.2,
        '11:34:21', '2011-10-30 11:34:21', '2011-10-30', '2011-10-30 11:34:21', '2015',
        '{ z : [ 1, 2, 3 ] }',
        convert(varbinary, 'this is a test 2'),
        0);

insert into person (first_name, last_name, address, city, state, zip, bigint_field, smallint_field, numeric_field,
                    double_field, float_field, real_field,
                    time_field, datetime2_field, date_field, datetime_field, year_field,
                    json,
                    blob_field, bit_field)
values ('first_name_3', 'last_name_3', '176 Bowery', 'New York', 'NY', 10012, 123090, -3, 55.12,
        5.0, 5.1, 5.55,
        '16:00:01', '2015-06-02 10:01:01', '2015-06-01', '2015-09-22 15:46:10', 1901,
        '{ [ a, b, c ] }',
        convert(varbinary, 'this is a test 3'),
        1);

INSERT INTO person (first_name) VALUES (null);

CREATE VIEW person_view AS SELECT * FROM person;
