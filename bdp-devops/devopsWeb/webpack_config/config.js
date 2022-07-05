const path = require('path');

module.exports = {
	prod: {
		root: path.resolve(__dirname, '../dist'),
		subDirectory: 'resource',
		publicPath: '/',
	}
};