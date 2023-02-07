create table person (
  person_id       Int32,

  first_name      Nullable(String),
  last_name       Nullable(String),
  address         Nullable(String),
  city            Nullable(String),
  state           Nullable(String),
  zip             Nullable(Int32),

  json            Nullable(String),

  bigint_field    Nullable(Int64),
  smallint_field  Nullable(Int16),
  decimal_field   Nullable(DECIMAL(15, 2)),
  boolean_field   Nullable(UInt8),
  double_field    Nullable(Float64),
  float_field     Nullable(Float32),

  date_field      Nullable(Date),
  datetime_field  Nullable(Datetime),
  enum_field      Enum('XXX'=1, 'YYY'=2, 'ZZZ'=3)
) ENGINE = MergeTree() order by person_id;

insert into person (person_id, first_name, last_name, address, city, state, zip, json,
                    bigint_field, smallint_field, decimal_field, boolean_field, double_field,
                    float_field, date_field, datetime_field, enum_field)
values (1, 'first_name_1', 'last_name_1', '1401 John F Kennedy Blvd', 'Philadelphia', 'PA',
        19107, '{ a : 5, b : 6 }', 123456789, 1, 123.321, 0, 1.0, 1.1, '2012-02-29',
        '2012-02-29 13:00:01', 'XXX');

insert into person (person_id, first_name, last_name, address, city, state, zip, json,
                    bigint_field, smallint_field, boolean_field, double_field,
                    float_field, date_field, datetime_field, enum_field)
values (2, 'first_name_2', 'last_name_2', 'One Ferry Building', 'San Francisco', 'CA', 94111,
        '{ z : [ 1, 2, 3 ] }', 45456767, 3, 1, 3.0, 3.1, '2011-10-30',
        '2011-10-30 11:34:21', 'YYY');

insert into person (person_id, first_name, last_name, address, city, state, zip, json,
                    bigint_field, smallint_field, boolean_field, double_field,
                    float_field, date_field, datetime_field, enum_field)
values (3, 'first_name_3', 'last_name_3', '176 Bowery', 'New York', 'NY', 10012, '{ [ a, b, c ] }',
        123090, -3, 0, 5.0, 5.1, '2015-06-01', '2015-09-22 15:46:10', 'ZZZ');

insert into person (person_id) values (4);

create view person_view as select * from person;
