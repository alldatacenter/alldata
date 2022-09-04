'use strict';


const autoprefixer = require('autoprefixer');
const path = require('path');
const webpack = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const CaseSensitivePathsPlugin = require('case-sensitive-paths-webpack-plugin');
const InterpolateHtmlPlugin = require('react-dev-utils/InterpolateHtmlPlugin');
const WatchMissingNodeModulesPlugin = require('react-dev-utils/WatchMissingNodeModulesPlugin');
const eslintFormatter = require('react-dev-utils/eslintFormatter');
const ModuleScopePlugin = require('react-dev-utils/ModuleScopePlugin');
const SimpleProgressWebpackPlugin = require('simple-progress-webpack-plugin')
const getClientEnvironment = require('./env');
const paths = require('./paths');
//const cdnPath = require('./cdnPath');
const ThemeVariables = require('./generateTheme');
const threadLoader = require('thread-loader');
const HtmlWebpackTagsPlugin = require('html-webpack-tags-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const publicPath = "/";
// `publicUrl` is just like `publicPath`, but we will provide it to our app
// as %PUBLIC_URL% in `index.html` and `process.env.PUBLIC_URL` in JavaScript.
// Omit trailing slash as %PUBLIC_PATH%/xyz looks better than %PUBLIC_PATH%xyz.
const publicUrl = '';
// Get environment variables to inject into our app.
const env = getClientEnvironment(publicUrl);
threadLoader.warmup(
    {
        // 池选项，例如传递给 loader 选项
        // 必须匹配 loader 选项才能启动正确的池
    },
    [
        // 加载模块
        // 可以是任意模块，例如
        'babel-loader',
        'babel-preset-es2015',
        'sass-loader',
        'less-loader'
    ]
);
// This is the development configuration.
// It is focused on developer experience and fast rebuilds.
// The production configuration is different and lives in a separate file.
module.exports = {
    // You may want 'eval' instead if you prefer to see the compiled output in DevTools.
    // See the discussion in https://github.com/facebookincubator/create-react-app/issues/343.
    mode: 'development',
    devtool: 'inline-cheap-source-map',
    // These are the "entry points" to our application.
    // This means they will be the "root" imports that are included in JS bundle.
    // The first two entry points enable "hot" CSS and auto-refreshes for JS.
    externals: {
        'axios': 'axios',
        'react': 'React',
        'react-dom': 'ReactDOM',
        "antd":"antd",
        'moment':'moment',
        "moment-duration-format":"moment-duration-format",
        "ant-design-icons":"ant-design-icons",
        "redux": 'Redux',
        "react-redux": 'ReactRedux',
        "bizcharts": "BizCharts",
        "html2canvas": "html2canvas",
        "jquery": "jQuery"
    },
    entry: {
        index: [
            // Include an alternative client for WebpackDevServer. A client's job is to
            // connect to WebpackDevServer by a socket and get notified about changes.
            // When you save a file, the client will either apply hot updates (in case
            // of CSS changes), or refresh the page (in case of JS changes). When you
            // make a syntax error, this client will display a syntax error overlay.
            // Note: instead of the default WebpackDevServer client, we use a custom one
            // to bring better experience for Create React App users. You can replace
            // the line below with these two lines if you prefer the stock client:
            // require.resolve('webpack-dev-server/client') + '?/',
            // require.resolve('webpack/hot/dev-server'),
            require.resolve('react-dev-utils/webpackHotDevClient'),
            // We ship a few polyfills by default:
            require.resolve('./polyfills'),
            // Errors should be considered fatal in development
            require.resolve('react-error-overlay'),
            // Finally, this is your app's code:
            paths.appIndexJs,
        ],
        // vendor: ['react', 'react-router', 'react-dom'],
    },
    output: {
        // Next line is not used in dev but WebpackDevServer crashes without it:
        path: paths.appBuild,
        // Add /* filename */ comments to generated require()s in the output.
        pathinfo: true,
        // This does not produce a real file. It's just the virtual path that is
        // served by WebpackDevServer in development. This is the JS bundle
        // containing code from all our entry points, and the Webpack runtime.
        filename: 'static/js/[name].bundle.js',
        // There are also additional JS chunk files if you use code splitting.
        chunkFilename: 'static/js/[name].chunk.js',
        // This is the URL that app is served from. We use "/" in development.
        publicPath: publicPath,
        // Point sourcemap entries to original disk location (format as URL on Windows)
        devtoolModuleFilenameTemplate: info =>
            path.resolve(info.absoluteResourcePath).replace(/\\/g, '/'),
    },
    optimization: {
        splitChunks: {
            chunks: "all"
        },
        namedModules: true,
        nodeEnv: 'development'
    },
    resolve: {
        // This allows you to set a fallback for where Webpack should look for modules.
        // We placed these paths second because we want `node_modules` to "win"
        // if there are any conflicts. This matches Node resolution mechanism.
        // https://github.com/facebookincubator/create-react-app/issues/253
        modules: ['node_modules', paths.appNodeModules].concat(
            // It is guaranteed to exist because we tweak it in `env.js`
            process.env.NODE_PATH.split(path.delimiter).filter(Boolean)
        ),
        // These are the reasonable defaults supported by the Node ecosystem.
        // We also include JSX as a common component filename extension to support
        // some tools, although we do not recommend using it, see:
        // https://github.com/facebookincubator/create-react-app/issues/290
        // `web` extension prefixes have been added for better support
        // for React Native Web.
        extensions: ['.web.js', '.js', '.json', '.web.jsx', '.jsx', '.mjs'],
        alias: paths.namespace,
        plugins: [
            // Prevents users from importing files from outside of src/ (or node_modules/).
            // This often causes confusion because we only process files within src/ with babel.
            // To fix this, we prevent you from importing files out of src/ -- if you'd like to,
            // please link the files into your node_modules/ and let module-resolution kick in.
            // Make sure your source files are compiled, as they will not be processed in any way.
            new ModuleScopePlugin(paths.appSrc, [paths.appPackageJson])
        ],
    },
    resolveLoader: {
        alias: {
            'copy': 'file-loader?name=[path][name].[ext]&context=./src'
        }
    },
    module: {
        strictExportPresence: true,
        rules: [
            // It's important to do this before Babel processes the JS.
            {
                test: /\.(js|jsx|mjs)$/,
                enforce: 'pre',
                use: [
                    {
                        options: {
                            formatter: eslintFormatter,
                            eslintPath: require.resolve('eslint'),

                        },
                        loader: require.resolve('eslint-loader'),
                    },
                ],
                include: paths.appSrc,
            },
            {
                // "oneOf" will traverse all following loaders until one will
                // match the requirements. When no loader matches it will fall
                // back to the "file" loader at the end of the loader list.
                oneOf: [
                    // "url" loader works like "file" loader except that it embeds assets
                    // smaller than specified limit in bytes as data URLs to avoid requests.
                    // A missing `test` is equivalent to a match.
                    {
                        test: [/\.bmp$/, /\.gif$/, /\.jpe?g$/, /\.png$/i, /\.svg$/],
                        loader: require.resolve('url-loader'),
                        options: {
                            limit: 1024 * 10,
                            name: 'static/media/[name].[hash:8].[ext]',
                        },
                    },
                    // Process JS with Babel.
                    {
                        test: /\.(js|jsx|mjs)$/,
                        include: paths.appSrc,
                        use: [
                            {
                                loader: require.resolve('thread-loader'),
                            },
                            {
                                // loader: require.resolve('babel-loader'),
                                loader: 'babel-loader',
                                options: {
                                    plugins: [
                                        // ["transform-runtime"],
                                        ['import', [{ libraryName: 'antd', style: 'css' }]],  // import less
                                    ],
                                    compact: true,
                                    cacheDirectory: true
                                },
                            }
                        ],
                    },
                    {
                        test: /\.(css|less)$/,
                        use: [
                            { loader: "style-loader" },
                            {
                                loader: 'thread-loader',
                            },
                            {
                                loader: require.resolve('css-loader'),
                                options: {
                                    importLoaders: 1,
                                },
                            },
                            {
                                loader: require.resolve('less-loader'), // compiles Less to CSS
                                options: {
                                    modifyVars: require('./generateTheme'),
                                    javascriptEnabled: true
                                }
                            },
                        ],
                    },
                    {
                        test: /\.(css|scss)$/,
                        use: [
                            { loader: "style-loader" },
                            {
                                loader: 'thread-loader',
                            },
                            {
                                loader: require.resolve('css-loader'),
                                options: {
                                    importLoaders: 1,
                                },
                            },
                            {
                                loader: require.resolve('sass-loader') // compiles Less to CSS
                            },
                        ],
                    },
                    {
                        exclude: [/\.js$/, /\.html$/, /\.json$/, /\.mjs$/],
                        loader: require.resolve('file-loader'),
                        options: {
                            name: 'static/media/[name].[hash:8].[ext]',
                        },
                    },
                ],
            },
            // ** STOP ** Are you adding a new loader?
            // Remember to add the new extension(s) to the "file" loader exclusion list.
        ],
    },
    plugins: [
        // new MiniCssExtractPlugin({ filename: 'static/css/[name].[hash:8].css', chunkFilename: 'static/css/[name].[hash:8].css' }),
        new HtmlWebpackPlugin({
            inject: true,
            template: paths.appHtml,
            chunksSortMode: 'none'
        }),
        // Makes some environment variables available in index.html.
        // The public URL is available as %PUBLIC_URL% in index.html, e.g.:
        // <link rel="shortcut icon" href="%PUBLIC_URL%/favicon.ico">
        // In development, this will be an empty string.
        new SimpleProgressWebpackPlugin(),
        new InterpolateHtmlPlugin(HtmlWebpackPlugin, env.raw),
        // Generates an `index.html` file with the <script> injected.
        // Add module names to factory functions so they appear in browser profiler.
        // Makes some environment variables available to the JS code, for example:
        // if (process.env.NODE_ENV === 'development') { ... }. See `./env.js`.
        new webpack.DefinePlugin(env.stringified),
        // This is necessary to emit hot updates (currently CSS only):
        new webpack.HotModuleReplacementPlugin(),
        // Watcher doesn't work well if you mistype casing in a path so we use
        // a plugin that prints an error when you attempt to do this.
        // See https://github.com/facebookincubator/create-react-app/issues/240
        new CaseSensitivePathsPlugin(),
        // If you require a missing module and then `npm install` it, you still have
        // to restart the development server for Webpack to discover it. This plugin
        // makes the discovery automatic so you don't have to restart.
        // See https://github.com/facebookincubator/create-react-app/issues/186
        new WatchMissingNodeModulesPlugin(paths.appNodeModules),
        // Moment.js is an extremely popular library that bundles large locale files
        // by default due to how Webpack interprets its code. This is a practical
        // solution that requires the user to opt into importing specific locales.
        // https://github.com/jmblog/how-to-optimize-momentjs-with-webpack
        // You can remove this if you don't use Moment.js:
        new webpack.IgnorePlugin(/^\.\/locale$/, /moment$/),

        new webpack.DefinePlugin({
            THEMES: JSON.stringify(ThemeVariables)
        })
        //new BundleAnalyzerPlugin()
    ],
    // Some libraries import Node modules but don't use them in the browser.
    // Tell Webpack to provide empty mocks for them so importing them works.
    node: {
        dgram: 'empty',
        fs: 'empty',
        net: 'empty',
        tls: 'empty',
        child_process: 'empty',
        __dirname: true
    },
    performance: {
        hints: false,
    }
};
