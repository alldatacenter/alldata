ALTER TABLE dice_issues ADD `reopen_count` int(11) NOT NULL DEFAULT 0 COMMENT 'issue reopen count';

UPDATE dice_issues,
       (SELECT issue_id,
               Count(*) AS reopen
        FROM   dice_issue_streams
        WHERE  stream_type = 'TransferState'
               AND stream_params LIKE '%"NewState":"重新打开"%'
        GROUP  BY issue_id) AS stats
SET    reopen_count = stats.reopen
WHERE  reopen_count = 0
       AND dice_issues.id = stats.issue_id; 