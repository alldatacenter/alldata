INSERT INTO `erda_token` (id, access_key,  type, data, scope, scope_id, creator_id)
SELECT UUID(), token, 'PAT', '', scope_type, scope_id, user_id
FROM `dice_member` WHERE scope_type = 'org';
