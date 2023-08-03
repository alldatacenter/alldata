SELECT t, t.1, t.2 FROM (SELECT ('1',2) AS t);
SELECT t, t.1, t.2 FROM (SELECT materialize(('1',2)) AS t);
SELECT t, t.1, t.2 FROM (SELECT (materialize('1'),2) AS t);
SELECT t, t.1, t.2 FROM (SELECT ('1',materialize(2)) AS t);
SELECT t, t.1, t.2 FROM (SELECT (materialize('1'),materialize(2)) AS t);

SELECT t, t[1].1, t[1].2 FROM (SELECT [('1',2)] AS t);
SELECT t, t[1].1, t[1].2 FROM (SELECT [materialize(('1',2))] AS t);
SELECT t, t[1].1, t[1].2 FROM (SELECT [(materialize('1'),2)] AS t);
SELECT t, t[1].1, t[1].2 FROM (SELECT [('1',materialize(2))] AS t);
SELECT t, t[1].1, t[1].2 FROM (SELECT [(materialize('1'),materialize(2))] AS t);
SELECT t, t[1].1, t[1].2 FROM (SELECT materialize([('1',2)]) AS t);

SELECT
    thing,
    thing[1],
    thing[1].1,
    thing[1].2,
    thing[1].1.1,
    thing[1].1.2,
    (thing[1].2)[1],
    (thing[1].2)[1].1,
    (thing[1].2)[1].2,
    ((thing[1].2)[1].2)[1]
FROM (
    SELECT [((1, materialize('2')), [(3, [4])])] AS thing
);

select arrayMap(t->tuple(t.1, t.2*2), [('1',2)]);
select arrayMap(t->tuple(t.1, t.2*2), [materialize(('1',2))]);
select arrayMap(t->tuple(t.1, t.2*2), [(materialize('1'),2)]);
select arrayMap(t->tuple(t.1, t.2*2), [('1',materialize(2))]);
select arrayMap(t->tuple(t.1, t.2*2), [(materialize('1'),materialize(2))]);
select arrayMap(t->tuple(t.1, t.2*2), materialize([('1',2)]));
