/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const path = require("path");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");

const distPath = path.resolve(__dirname, "dist");

const node_env = process.env.NODE_ENV ? process.env.NODE_ENV : 'production';

const config = {
	mode: node_env,
	entry: "./src/index.js",
	output: {
		library: "LineageHelper",
		libraryTarget: "umd",
		path: distPath,
		filename: "index.js"
	},
	plugins: [new MiniCssExtractPlugin({ filename: "styles.css" })],
	externals: {
		//don't bundle the 'react' npm package with our bundle.js
		//but get it from a global 'React' variable
		d3: "d3",
		"dagre-d3": "dagreD3",
		platform: "platform",
		dagre: "dagre",
		graphlib: "graphlib"
	},
	module: {
		rules: [
			{
				test: /\.m?js$/,
				exclude: /(node_modules|bower_components)/,
				use: {
					loader: "babel-loader",
					options: {
						presets: ["@babel/preset-env"],
						plugins: ["transform-class-properties"]
					}
				}
			},
			{
				test: /\.s[ac]ss$/i,
				use: [
					MiniCssExtractPlugin.loader,
					// Translates CSS into CommonJS
					"css-loader",
					// Compiles Sass to CSS
					"sass-loader"
				]
			}
		]
	}
};

if (process.env.NODE_ENV === "development") {
	config.devtool = "inline-source-map";
}
module.exports = config;