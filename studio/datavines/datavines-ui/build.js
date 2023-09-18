/* eslint-disable no-shadow */
/* eslint-disable @typescript-eslint/no-unused-vars */
// Build component library
const watch = require('glob-watcher');
const ts = require('gulp-typescript');
const del = require('del');
const less = require('gulp-less');
const path = require('path');
const { statSync } = require('fs');
const { src, dest } = require('vinyl-fs');
const gulpif = require('gulp-if');
const replace = require('gulp-replace');
const chalk = require('chalk');
const through = require('through2');
const babel = require('@babel/core');

const createTsProject = (tsDefault) => ts.createProject('tsconfig.json', tsDefault);

const babelConfig = ({ chunk, format }) => {
    const reg = /\.less$/;
    const contents = chunk?.contents || '';
    const babelObj = {
        presets: [
            [
                require.resolve('@babel/preset-env'),
                {
                    modules: format === 'esm' ? false : 'cjs',
                    loose: true,
                },
            ],
            require.resolve('@babel/preset-react'),
        ],
        plugins: [
            {
                name: 'import-less-to-css',
                visitor: {
                    // Solve the problem of introducing styles using require in commonjs
                    ExpressionStatement(path) {
                        if (format === 'esm') {
                            return;
                        }
                        const argumentsZero = path.node.expression && path.node.expression.arguments && path.node.expression.arguments[0];
                        if (argumentsZero && argumentsZero.value && reg.test(argumentsZero.value)) {
                            path.node.expression.arguments[0].value = argumentsZero.value.replace(reg, '.css');
                        }
                    },
                    // ES Module use import css question
                    ImportDeclaration(path) {
                        if (reg.test(path.node.source.value)) {
                            path.node.source.value = path.node.source.value.replace(reg, '.css');
                        }
                    },
                },
            },
        ],
    };
    return babel.transformSync(contents, babelObj).code;
};

const getPaths = (dir) => {
    const ignores = [
        '**/demo{, /**}',
        '**/Demo{, /**}',
        '**/Demo/**/*',
        '**/__test__{, /**}',
        '**/__tests__{, /**}',
        '**/*.md',
    ];
    const files = [
        '**/*.ts',
        '**/*.tsx',
        '**/*.css',
        '**/*.less',
    ];
    return {
        files: files.map((item) => `${path.resolve(dir, item)}`),
        ignores: ignores.map((item) => `!${path.resolve(dir, item)}`),
    };
};

const createStream = ({
    format, paths, version, dir,
}) => {
    const tsProject = createTsProject({ module: format === 'esm' ? 'ESNext' : 'CommonJS' });
    const output = `${path.resolve(dir.replace(/\/src/, ''), format)}/`;
    console.log(output);
    return src(paths)
        .pipe(gulpif(
            (file) => /\.less$/.test(file.path),
            less(),
        ))
        .pipe(gulpif(
            (file) => /(\.(ts)|\.(tsx))$/.test(file.path),
            tsProject(),
        ))
        .pipe(gulpif(
            (file) => /\.js$/.test(file.path),
            // babel(babelConfig(format)),
            through.obj((chunk, enc, callback) => {
                try {
                    chunk.contents = Buffer.from(babelConfig({ chunk, format }));
                    callback(null, chunk);
                } catch (error) {
                    console.log(error);
                }
            }),
        ))
        .pipe(replace('__version__', version || ''))
        .pipe(dest(output));
};

const buildSingle = async ({
    format, dir, isWatch, version,
}) => {
    await del(`${format}/**`);
    const { files, ignores } = getPaths(dir);
    const paths = [
        ...files,
        ...ignores,
    ];
    createStream({
        format, paths, version, dir,
    }).on('end', () => {
        if (!isWatch) {
            return;
        }
        console.log(chalk.green('watch...'));
        const watcher = watch(paths, {
            ignoreInitial: true,
        });
        watcher.on('all', (event, path) => {
            console.log(format, event, chalk.green(path));
            if (!statSync(path).isFile()) {
                return;
            }
            createStream({ format, paths: [path, ...ignores], version });
        });
    });
};

const build = (options) => {
    const { format, ...rest } = options;
    if (!format) {
        console.log(chalk.green('Format not passed in'));
        return;
    }
    const formatArr = format.split(',');
    formatArr.forEach((item) => {
        buildSingle({ format: item, ...rest });
    });
};

module.exports = {
    createStream,
    build,
};
