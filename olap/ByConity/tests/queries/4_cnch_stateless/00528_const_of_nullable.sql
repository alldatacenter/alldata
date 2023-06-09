SELECT toNullable(0) + 1 AS x, toTypeName(toNullable(0) + 1), toColumnTypeName(toNullable(0) + 1);
SELECT toNullable(materialize(0)) + 1 AS x, toTypeName(toNullable(materialize(0)) + 1), toColumnTypeName(toNullable(materialize(0)) + 1);
SELECT materialize(toNullable(0)) + 1 AS x, toTypeName(materialize(toNullable(0)) + 1), toColumnTypeName(materialize(toNullable(0)) + 1);
SELECT toNullable(0) + materialize(1) AS x, toTypeName(toNullable(0) + materialize(1)), toColumnTypeName(toNullable(0) + materialize(1));
SELECT toNullable(materialize(0)) + materialize(1) AS x, toTypeName(toNullable(materialize(0)) + materialize(1)), toColumnTypeName(toNullable(materialize(0)) + materialize(1));
SELECT materialize(toNullable(0)) + materialize(1) AS x, toTypeName(materialize(toNullable(0)) + materialize(1)), toColumnTypeName(materialize(toNullable(0)) + materialize(1));
