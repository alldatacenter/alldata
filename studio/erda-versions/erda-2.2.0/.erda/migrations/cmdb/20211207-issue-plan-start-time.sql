UPDATE dice_issues
SET    plan_started_at = plan_finished_at
WHERE  plan_finished_at IS NOT NULL
       AND ( plan_started_at IS NULL
              OR plan_started_at > plan_finished_at ) 
