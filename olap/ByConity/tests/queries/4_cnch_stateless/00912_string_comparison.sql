SELECT a = b, a < b, a > b, a <= b, a >= b FROM (SELECT prefix, prefix || 'x' AS a, prefix || 'y' AS b FROM (SELECT substring('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 1, number) AS prefix FROM numbers(40)));
SELECT a = b, a < b, a > b, a <= b, a >= b FROM (SELECT prefix, prefix || 'y' AS a, prefix || 'x' AS b FROM (SELECT substring('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 1, number) AS prefix FROM numbers(40)));
SELECT a = b, a < b, a > b, a <= b, a >= b FROM (SELECT prefix, prefix || 'x' AS a, prefix || 'x' AS b FROM (SELECT substring('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 1, number) AS prefix FROM numbers(40)));

SELECT a = b, a < b, a > b, a <= b, a >= b FROM (SELECT prefix, prefix || 'x' || prefix AS a, prefix || 'y' || prefix AS b FROM (SELECT substring('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 1, number) AS prefix FROM numbers(40)));
SELECT a = b, a < b, a > b, a <= b, a >= b FROM (SELECT prefix, prefix || 'y' || prefix AS a, prefix || 'x' || prefix AS b FROM (SELECT substring('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 1, number) AS prefix FROM numbers(40)));
SELECT a = b, a < b, a > b, a <= b, a >= b FROM (SELECT prefix, prefix || 'x' || prefix AS a, prefix || 'x' || prefix AS b FROM (SELECT substring('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 1, number) AS prefix FROM numbers(40)));

SELECT a = b, a < b, a > b, a <= b, a >= b FROM (SELECT prefix, prefix || 'x' || prefix AS a, prefix || 'y' AS b FROM (SELECT substring('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 1, number) AS prefix FROM numbers(40)));
SELECT a = b, a < b, a > b, a <= b, a >= b FROM (SELECT prefix, prefix || 'y' || prefix AS a, prefix || 'x' AS b FROM (SELECT substring('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 1, number) AS prefix FROM numbers(40)));
SELECT a = b, a < b, a > b, a <= b, a >= b FROM (SELECT prefix, prefix || 'x' || prefix AS a, prefix || 'x' AS b FROM (SELECT substring('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 1, number) AS prefix FROM numbers(40)));

SELECT a = b, a < b, a > b, a <= b, a >= b FROM (SELECT arrayJoin(['aaa', 'bbb']) AS a, 'aaa\0bbb' AS b);
SELECT a = b, a < b, a > b, a <= b, a >= b FROM (SELECT arrayJoin(['aaa', 'zzz']) AS a, 'aaa\0bbb' AS b);
SELECT a = b, a < b, a > b, a <= b, a >= b FROM (SELECT arrayJoin(['aaa', 'bbb']) AS a, materialize('aaa\0bbb') AS b);
SELECT a = b, a < b, a > b, a <= b, a >= b FROM (SELECT arrayJoin(['aaa', 'zzz']) AS a, materialize('aaa\0bbb') AS b);

-- Below query is illegal in optimizer mode, as function `toFixedString` requires its second argument is a constant
-- in analyze phase, but ExprAnalyzer won't do constant folding during analyze phase(SyntaxAnalyzer will do).
SET enable_optimizer=0;
SELECT empty(toFixedString('', 1 + randConstant() % 100));
