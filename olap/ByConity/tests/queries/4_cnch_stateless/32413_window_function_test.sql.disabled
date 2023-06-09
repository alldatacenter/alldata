

DROP TABLE IF EXISTS ppl4;
CREATE TABLE ppl4 (id Int, department String, onboard_date String, age Int) ENGINE = CnchMergeTree() ORDER BY tuple();

INSERT INTO ppl4 VALUES('1', 'data', '2019-01-01', '20') ('2', 'data', '2019-03-01', '21') ('3', 'data', '2019-02-01', '22') ('4', 'data', '2019-03-01', '23') ('5', 'data', '2019-04-01', '24') ('6', 'payment', '2019-01-01', '25') ('7', 'payment', '2019-02-01', '26') ('8', 'payment', '2019-04-01', '27') ('9', 'payment', '2019-05-01', '28') ('10', 'solution', '2019-08-01', '29') ('11', 'solution', '2019-08-01', '30') ('12', 'solution', '2019-09-01', '31') ('13', 'ML', '2019-03-01', '32') ('14', 'ML', '2019-12-01', '33') ('15', 'ML', '2019-03-01', '34') ('16', 'ML', '2019-02-01', '35');


SELECT
    row_number() OVER (ORDER BY age ASC)
FROM ppl4
Order BY id;


DROP TABLE ppl4;
