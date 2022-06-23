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

import menu from "./docz-lib/config/menu";
import versions from './docz-lib/config/versions';

module.exports = {
	title: "Apache Atlas – Data Governance and Metadata framework for Hadoop",
	description: "Apache Atlas – Data Governance and Metadata framework for Hadoop",
	files: "**/*.{md,mdx}",
	base: "/",
	baseUrl:"./public",
	src: "./src",
	public: "./src/resources",
	dest: '/site',
	menu: menu,
	atlasVersions: versions,
	theme: "theme/",
	htmlContext:{
		favicon: "public/images/favicon.ico"
	},
	modifyBundlerConfig: config => {
		config.module.rules.push(
			{
				test: /\.(js)$/,
				exclude: /node_modules/,
				use: {
					loader: "babel-loader",
					query: {
						presets: ["@babel/react"],
						plugins: [
							"@babel/plugin-proposal-class-properties",
							"@babel/plugin-syntax-dynamic-import"
						]
					}
				}
			}
		);
		return config;
	}
};
