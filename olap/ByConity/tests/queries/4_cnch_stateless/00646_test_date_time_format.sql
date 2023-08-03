SELECT date_format(toDateTime('2022-02-06 09:01:12', 'Asia/Shanghai'), 'Y:y:M:d:H:K:m:s:D:a', 'Asia/Shanghai');
SELECT date_format(toDateTime('2022-02-06 09:01:12', 'Asia/Shanghai'), 'Y:y:M:d:H:K:m:s:D:a', 'America/New_York');

SELECT date_format(toDateTime('2022-02-06', 'Asia/Shanghai'), 'Y:y:M:d:H:K:m:s:D:a', 'America/New_York');
SELECT date_format(toDateTime('2022-02-06 09:01:12', 'Asia/Shanghai'), 'Y:y:M:d:H:K:m:s:D:a', 'America/New_York');

SELECT date_format(toDateTime('2022 02 06 09 01 12', 'Asia/Shanghai'), 'Y:y:M:d:H:K:m:s:D:a', 'Asia/Shanghai');
SELECT date_format(toDateTime('2022 02 06 09 01 12', 'Asia/Shanghai'), 'Y:y:M:d:H:K:m:s:D:a', 'America/New_York');

SELECT date_format(toDateTime('2022 02 06 09 01 12', 'Asia/Shanghai'), 'YyMdHKmsDa');
SELECT date_format(toDateTime('2022 02 06 09 01 12', 'Asia/Shanghai'), 'YyMdHKmsDa', 'America/New_York');
