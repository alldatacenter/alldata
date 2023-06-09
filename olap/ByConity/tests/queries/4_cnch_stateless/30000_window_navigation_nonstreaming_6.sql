DROP TABLE IF EXISTS wfnav6;

CREATE TABLE wfnav6 (`ts` UInt64, `user` String, `sql` String) ENGINE = CnchMergeTree() ORDER BY tuple();

INSERT INTO wfnav6 (`ts`, `user`, `sql`) values (1 , 'b', 's1'), (5 , 'b', 's1'), (3 , 'b', 's1'), (10, 'a', 's1'), (1 , 'a', 's1'), (5 , 'a', 's1');

select user, sql, ts, lagInFrame(ts, 1) over (partition by user order by ts rows between unbounded preceding and unbounded following) as prev,
leadInFrame(ts, 1) over (partition by user order by ts rows between unbounded preceding and unbounded following) as follow
from wfnav6 order by user, ts, prev, follow
FORMAT TabSeparated;

DROP TABLE wfnav6;
