const users: LViewsUsers = {
  table: {
    columns: {
      username: '用户名',
      email: '邮箱',
      role: '角色',
    }
  },
  navActions: {
    new: {
      label: '新建用户',
      tooltip: '添加一个新用户',
    },
    filter: {
      search: {
        placeholder: '搜索用户',
      }
    }
  },
  navActionsExtra: {
    filter: {
      select: {
        role: {
          label: '角色',
        }
      },
      search: {
        email: {
          placeholder: '搜索邮箱',
        }
      }
    }
  }
};

export default users;
