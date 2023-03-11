// vue.config.js
module.exports = {
    // 基本路径
    publicPath: '.',
    devServer: {
        host: '0.0.0.0',
        port: 3000,
        proxy: {
            '/business': {
                target: 'http://127.0.0.1:9900',
                changeOrigin: true,
                ws: true,
                pathRewrite: {
                    '^/business': '/'
                }
            }
        },
        disableHostCheck: true,
    }
}