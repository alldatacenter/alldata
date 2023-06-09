SELECT
    toNullable(NULL) AS a,
    toNullable('Hello') AS b,
    toNullable(toNullable(1)) AS c,
    toNullable(materialize(NULL)) AS d,
    toNullable(materialize('Hello')) AS e,
    toNullable(toNullable(materialize(1))) AS f,
    toTypeName(toNullable(NULL)),
    toTypeName(toNullable('Hello')),
    toTypeName(toNullable(toNullable(1))),
    toTypeName(toNullable(materialize(NULL))),
    toTypeName(toNullable(materialize('Hello'))),
    toTypeName(toNullable(toNullable(materialize(1))));
