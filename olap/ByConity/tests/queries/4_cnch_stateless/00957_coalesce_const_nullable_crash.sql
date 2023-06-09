SELECT coalesce(toNullable(1), toNullable(2)) as x, toTypeName(coalesce(toNullable(1), toNullable(2)));
SELECT coalesce(NULL, toNullable(2)) as x, toTypeName(coalesce(NULL, toNullable(2)));
SELECT coalesce(toNullable(1), NULL) as x, toTypeName(coalesce(toNullable(1), NULL));
SELECT coalesce(NULL, NULL) as x, toTypeName(coalesce(NULL, NULL));

SELECT coalesce(toNullable(materialize(1)), toNullable(materialize(2))) as x, toTypeName(coalesce(toNullable(materialize(1)), toNullable(materialize(2))));
SELECT coalesce(NULL, toNullable(materialize(2))) as x, toTypeName(coalesce(NULL, toNullable(materialize(2))));
SELECT coalesce(toNullable(materialize(1)), NULL) as x, toTypeName(coalesce(toNullable(materialize(1)), NULL));
SELECT coalesce(materialize(NULL), materialize(NULL)) as x, toTypeName(coalesce(materialize(NULL), materialize(NULL)));

SELECT coalesce(toLowCardinality(toNullable(1)), toLowCardinality(toNullable(2))) as x, toTypeName(coalesce(toLowCardinality(toNullable(1)), toLowCardinality(toNullable(2))));
SELECT coalesce(NULL, toLowCardinality(toNullable(2))) as x, toTypeName(coalesce(NULL, toLowCardinality(toNullable(2))));
SELECT coalesce(toLowCardinality(toNullable(1)), NULL) as x, toTypeName(coalesce(toLowCardinality(toNullable(1)), NULL));
