update dice_member_extra
set resource_value = 'Manager'
where scope_type = 'sys'
  and resource_key = 'role'
  and resource_value = 'Owner';
