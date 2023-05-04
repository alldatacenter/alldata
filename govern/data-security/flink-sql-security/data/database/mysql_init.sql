INSERT INTO products
VALUES (default,"scooter","Small 2-wheel scooter"),
       (default,"car battery","12V car battery"),
       (default,"12-pack drill bits","12-pack of drill bits with sizes ranging from #40 to #3"),
       (default,"hammer","12oz carpenter's hammer"),
       (default,"hammer","14oz carpenter's hammer"),
       (default,"hammer","16oz carpenter's hammer"),
       (default,"rocks","box of assorted rocks"),
       (default,"jacket","water resistent black wind breaker"),
       (default,"spare tire","24 inch spare tire");


INSERT INTO orders
VALUES (default, '2020-07-30 10:08:22', 'Jack', 50.50, 102, false, 'beijing'),
       (default, '2020-07-30 10:11:09', 'Sally', 15.00, 105, false, 'beijing'),
       (default, '2020-07-30 12:00:30', 'Edward', 25.25, 106, false, 'hangzhou'),
       (default, '2022-12-15 12:11:09', 'John', 78.00, 103, false, 'hangzhou'),
       (default, '2022-12-16 12:00:30', 'Edward', 64.00, 104, false, 'shanghai'),
       (default, '2022-12-17 23:00:30', 'Jack', 20.00, 103, false, 'shanghai');


INSERT INTO shipments
VALUES (default,10001,'Beijing','Shanghai',false),
       (default,10002,'Hangzhou','Shanghai',false),
       (default,10003,'Shanghai','Hangzhou',false);


-- this data can be inserted when running the job
-- INSERT INTO orders VALUES (default, '2022-12-14 18:08:22', 'Jack', 35, 102, false, 'beijing')