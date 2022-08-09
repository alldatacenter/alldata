const userReducer = (state = null, action) => {
	switch (action.type) {
		case 'SIGN_IN':
		  return {...action.signData};
		case 'SIGN_OUT':
		  return null;
		default:
		  return state;
	}
};

export default userReducer;
