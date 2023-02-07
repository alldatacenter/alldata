interface LComponentsUser {
  form: {
    username: string;
    password: string;
    changePassword: string;
    email: string;
    role: string;
    newPassword: string;
  };
  role: {
    admin: string;
    normal: string;
  };
  delete: {
    tooltip: {
      adminUserNonDeletable: string;
    };
  };
  messageBox: {
    prompt: {
      changePassword: string;
    };
  };
  rules: {
    invalidPassword: string;
  };
}
