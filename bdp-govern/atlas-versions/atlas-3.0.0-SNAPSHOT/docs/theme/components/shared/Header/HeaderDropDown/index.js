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

import React, { useContext } from "react";
import styled from "styled-components";
import Basic from "./Basic";
import { doczState } from "../../../../../docz-lib/docz/dist";

const DropDownDivContainer = styled.div`
        width: 160px;
	padding: 0;
	margin-right: 5px;
	float: right;
`;

export const HeaderDropDown = () => {
	const { config } = useContext(doczState.context);
	const { atlasVersions } = config;
	return (
		<DropDownDivContainer>
			<Basic
				options={atlasVersions}
				style={{
					minHeight: "auto",
					padding: "0 5px",
					borderRadius: "5px"
				}}
			/>
		</DropDownDivContainer>
	);
};
