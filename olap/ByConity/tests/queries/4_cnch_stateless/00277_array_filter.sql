SELECT sum(length(arr)) FROM (SELECT arrayMap(x -> toString(x), range(number % 10)) AS arr FROM (SELECT * FROM system.numbers LIMIT 1000) WHERE length(arrayMap(x -> toString(x), range(number % 10))) % 2 = 0);
SELECT sum(length(arr)) FROM (SELECT range(number % 10) AS arr FROM (SELECT * FROM system.numbers LIMIT 1000) WHERE length(range(number % 10)) % 2 = 0);
