const user: LComponentsUser = {
  form: {
    username: 'Username',
    password: 'Password',
    changePassword: 'Change Password',
    email: 'Email',
    role: 'Role',
    newPassword: 'New Password',
  },
  role: {
    admin: 'Admin',
    normal: 'Normal',
  },
  delete: {
    tooltip: {
      adminUserNonDeletable: 'Admin user is non-deletable',
    }
  },
  messageBox: {
    prompt: {
      changePassword: 'Please enter the new password',
    }
  },
  rules: {
    invalidPassword: 'Invalid password. Length must be no less than 5.',
  },
};

export default user;
