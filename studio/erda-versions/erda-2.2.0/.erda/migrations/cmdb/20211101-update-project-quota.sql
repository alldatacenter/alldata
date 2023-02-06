UPDATE ps_group_projects
SET cpu_quota=0,
    mem_quota=0;

UPDATE ps_group_projects a, ps_group_projects_quota b
SET a.cpu_quota=b.prod_cpu_quota + b.staging_cpu_quota + b.test_cpu_quota + b.dev_cpu_quota,
    a.mem_quota=b.prod_mem_quota + b.staging_mem_quota + b.test_mem_quota + b.dev_mem_quota
WHERE a.id = b.project_id
  AND b.project_id IS NOT NULL
;
