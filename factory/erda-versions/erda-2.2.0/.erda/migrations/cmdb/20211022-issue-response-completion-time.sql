ALTER TABLE dice_issues 
ADD `start_time` datetime DEFAULT NULL COMMENT 'issue start time';

UPDATE dice_issues,
       (SELECT a.id,
               b.created_at
        FROM   dice_issues a
               INNER JOIN dice_issue_streams b
                       ON a.id = b.issue_id
        WHERE  a.deleted = 0
               AND b.stream_type = 'TransferState'
               AND b.stream_params LIKE '%"CurrentState":"待处理"%') AS stats
SET    start_time = stats.created_at
WHERE  start_time IS NULL
       AND dice_issues.id = stats.id; 