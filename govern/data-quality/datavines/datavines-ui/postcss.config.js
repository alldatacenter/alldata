module.exports = {
    plugins: {
        'postcss-preset-env': {
            stage: 3,
            features: {
                'nesting-rules': true,
            },
            insertBefore: {},
            // autoprefixer: { grid: true },
        },
        cssnano: {},
        autoprefixer: {
            overrideBrowserslist: [
                'Chrome > 31',
                'IE 11',
            ],
        },
    },
};
