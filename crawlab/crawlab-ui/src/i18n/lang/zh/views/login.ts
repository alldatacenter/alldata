const login: LViewsLogin = {
  loginForm: {
    username: '用户名',
    password: '密码',
    confirmPassword: '确认密码',
    email: '邮箱',
    signUp: '注册',
    signIn: '登陆',
  },
  forgotPassword: {
    label: '忘记密码',
    content: '请参照文档中的重置密码章节'
  },
  initial: {
    title: '初始用户名/密码',
  },
  documentation: '文档',
  mobile: {
    warning: '您在未优化过的移动端上运行，请使用台式机或笔记本'
  },
  errors: {
    incorrectUsername: '请输入正确的用户名',
    passwordLength: '密码长度不能低于 5',
    passwordSame: '两次密码必须相同',
    unauthorized: '验证失败，请检查用户名和密码',
    noTokenReturned: '未返回令牌',
  }
};

export default login;
