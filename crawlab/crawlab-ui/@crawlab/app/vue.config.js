const { BundleAnalyzerPlugin } = require('webpack-bundle-analyzer')

module.exports = {
  pages: {
    index: {
      entry: `src/${process.env.ENTRY}` || 'src/main-app.ts',
      template: 'public/index.html',
      filename: 'index.html',
    }
  },
  configureWebpack: {
    optimization: {
      splitChunks: {
        chunks: 'initial',
        minSize: 20000,
        minChunks: 1,
        maxAsyncRequests: 3,
        cacheGroups: {
          defaultVendors: {
            test: /[\\/]node_modules[\\/]]/,
            priority: -10,
            reuseExistingChunk: true,
          },
          default: {
            minChunks: 2,
            priority: -20,
            reuseExistingChunk: true,
          }
        },
      }
    },
    plugins: [
      new BundleAnalyzerPlugin()
    ]
  }
}
