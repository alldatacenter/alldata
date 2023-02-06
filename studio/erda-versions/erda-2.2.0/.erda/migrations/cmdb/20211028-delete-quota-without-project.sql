DELETE FROM ps_group_projects_quota WHERE project_id NOT IN (SELECT id FROM ps_group_projects);
