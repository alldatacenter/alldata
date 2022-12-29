/* eslint-disable import/no-anonymous-default-export */
import { babel } from '@rollup/plugin-babel';
import commonjs from '@rollup/plugin-commonjs';
import json from '@rollup/plugin-json';
import { nodeResolve } from '@rollup/plugin-node-resolve';
import replace from '@rollup/plugin-replace';
import typescript from '@rollup/plugin-typescript';
import path from 'path';
import cleanup from 'rollup-plugin-cleanup';
export default {
  input: 'src/task.ts', // 打包入口
  output: {
    // 打包出口
    name: 'getQueryData', // namespace
    file: path.resolve(__dirname, 'public/task/index.js'), // 最终打包出来的文件路径和文件名
    format: 'umd', // umd/amd/cjs/iife
  },
  plugins: [
    json(),
    nodeResolve({
      extensions: ['.js', '.ts'],
    }),
    // 解析TypeScript
    typescript({
      tsconfig: path.resolve(__dirname, 'tsconfig.json'),
    }),
    // 将 CommonJS 转换成 ES2015 模块供 Rollup 处理
    commonjs(),
    // es6--> es5
    babel({
      babelHelpers: 'runtime',
      exclude: 'node_modules/**',
      presets: [['@babel/preset-env', { modules: false }]],
      comments: false,
    }),
    cleanup(),
    replace({
      'console.log': '//console.log',
    }),
  ],
};
