const webpack = require('webpack');
const paths = require('./paths');
const runtimePaths = require('./runtimePaths');
const CopyWebpackPlugin = require('copy-webpack-plugin');

module.exports = {
    entry: {
        vendor: ['lodash'],
    },
    output: {
        filename: '[name].dll.js',
        path: paths.appBuild,
        library: "[name]_[hash]"
    },
    plugins: [
        // 目标操作
        new CopyWebpackPlugin([
            ...runtimePaths.dependency_arr_pre
        ]),
    ]
};