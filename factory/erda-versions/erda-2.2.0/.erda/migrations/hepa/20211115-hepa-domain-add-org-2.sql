update tb_gateway_domain a join ps_group_projects b on a.project_id collate utf8mb4_unicode_ci = cast(b.id as char) collate utf8mb4_unicode_ci
set a.org_id = cast(b.org_id as char)
where a.project_id != '' and a.is_deleted = 'N';


