interface LViewsLogin {
  loginForm: {
    username: string;
    password: string;
    confirmPassword: string;
    email: string;
    signUp: string;
    signIn: string;
  };
  forgotPassword: {
    label: string;
    content: string;
  };
  initial: {
    title: string;
  };
  documentation: string;
  mobile: {
    warning: string;
  };
  errors: {
    incorrectUsername: string;
    passwordLength: string;
    passwordSame: string;
    unauthorized: string;
    noTokenReturned: string;
  };
}
