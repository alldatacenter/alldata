SELECT nested, nested.1, nested.2 FROM (SELECT [(1, 'Hello'), (2, 'World')] AS nested);
SELECT nested, nested.1, nested.2 FROM (SELECT [[(1, 'Hello'), (2, 'World')], [(3, 'Goodbye')]] AS nested);
