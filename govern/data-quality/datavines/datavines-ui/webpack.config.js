const { merge } = require('webpack-merge');

module.exports = ({ WEBPACK_SERVE, DV_ENV }) => {
    const NODE_ENV = WEBPACK_SERVE ? 'development' : 'production';
    process.env.NODE_ENV = NODE_ENV;
    process.env.DV_ENV = DV_ENV || 'prod';
    const commonConfig = require('./build/webpack.common.js');
    const envConfig = require(`./build/webpack.${NODE_ENV}.js`);

    return merge(
        commonConfig,
        envConfig,
    );
};
