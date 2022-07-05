// user
export const signIn = (signData) => {
  	return {
		type: 'SIGN_IN',
		signData
  	};
};

export const signOut = () => {
  	return {
		type: 'SIGN_OUT'
  	};
};

