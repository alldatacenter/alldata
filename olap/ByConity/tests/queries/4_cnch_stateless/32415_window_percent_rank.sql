


DROP TABLE IF EXISTS window_percent_rank;
CREATE TABLE window_percent_rank (id Int, department String, onboard_date String, age Int) ENGINE = CnchMergeTree() ORDER BY tuple();

INSERT INTO window_percent_rank VALUES('1', 'data', '2019-01-01', '20') ('2', 'data', '2019-03-01', '21') ('3', 'data', '2019-02-01', '29') ('4', 'data', '2019-03-01', '23') ('5', 'data', '2019-04-01', '22') ('6', 'payment', '2019-01-01', '20') ('7', 'payment', '2019-02-01', '20') ('8', 'payment', '2019-04-01', '21') ('9', 'payment', '2019-05-01', '23') ('10', 'solution', '2019-08-01', '22') ('11', 'solution', '2019-08-01', '24') ('12', 'solution', '2019-09-01', '25') ('13', 'ML', '2019-03-01', '21') ('14', 'ML', '2019-12-01', '22') ('15', 'ML', '2019-03-01', '24') ('16', 'ML', '2019-02-01', '25');


SELECT
    id,
    sum(age) over (PARTITION BY department ORDER by id rows between 1 preceding and 1 following),
    Avg(age) OVER (PARTITION BY department ORDER by id rows between 1 preceding and 1 following),
    rank() OVER (PARTITION BY department ORDER BY id),
    percent_rank() OVER (PARTITION BY department ORDER BY id)
FROM window_percent_rank
Order by department, id
settings allow_experimental_window_functions = true;

SELECT
    id,
    sum(age) over (PARTITION BY department ORDER by id rows between 1 preceding and 1 following),
    Avg(age) OVER (PARTITION BY department ORDER by id rows between 1 preceding and 1 following),
    rank() OVER (PARTITION BY department ORDER BY id),
    percent_rank() OVER (PARTITION BY department ORDER BY id)
FROM window_percent_rank
Order by department, id
settings max_block_size = 2, allow_experimental_window_functions = true;



DROP TABLE window_percent_rank;
