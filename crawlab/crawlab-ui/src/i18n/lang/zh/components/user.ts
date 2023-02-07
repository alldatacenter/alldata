const user: LComponentsUser = {
  form: {
    username: '用户名',
    password: '密码',
    changePassword: '更改密码',
    email: '邮箱',
    role: '角色',
    newPassword: '新密码',
  },
  role: {
    admin: '管理员',
    normal: '普通用户',
  },
  delete: {
    tooltip: {
      adminUserNonDeletable: '管理员用户不可删除',
    }
  },
  messageBox: {
    prompt: {
      changePassword: '请输入新密码',
    }
  },
  rules: {
    invalidPassword: '无效密码，长度不能低于 5',
  },
};

export default user;
