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

import * as React from "react";
import { SFC } from "react";
import styled from "styled-components";
import { get } from "../../../utils/theme";
import { mq, breakpoints } from "../../../styles/responsive";

const sidebarBg = get("colors.sidebarBg");
const sidebarText = get("colors.sidebarText", "#13161F");
const LinkStyled = styled.a`
	&,
	&:visited,
	&:active {
		text-decoration: none;
		color: ${get("colors.link")};
	}

	&:hover {
		color: ${get("colors.link")};
	}

	${get("styles.link")};
`;
const FooterDiv = styled.div`
	position: absolute;
	margin-left: 350px;
	width: 70%;
	font-size: 10px;
	@media screen and (max-width: ${breakpoints.oldDesktop}px) {
		width: 90%;
		margin: 0 50px;
	}
	@media screen and (max-width: ${breakpoints.tablet - 1}px) {
		width: 90%;
		margin: 0 50px;
	}
`;
const ParagraphFooter = styled.div``;
export const LegalFooter = props => {
	let { options, title, ...rest } = props;
	return (
		<FooterDiv>
			<div className="container">
				<div className="row">
					<ParagraphFooter>
						<LinkStyled href="https://www.apache.org/foundation/contributing">
							<img
								src="https://www.apache.org/images/SupportApache-small.png"
								alt="Support the ASF"
								id="asf-logo"
								height="10"
								width="10"
							/>
						</LinkStyled>
						Copyright © 2011-2018 The Apache Software Foundation.
						Licensed under the{" "}
						<LinkStyled href="https://www.apache.org/licenses/LICENSE-2.0">
							Apache License, Version 2.0
						</LinkStyled>
						.Apache Atlas, Atlas, Apache, the Apache feather logo
						are trademarks of the{" "}
						<LinkStyled href="https://www.apache.org">
							Apache Software Foundation
						</LinkStyled>
						.All other marks mentioned may be trademarks or
						registered trademarks of their respective owners.
					</ParagraphFooter>
				</div>
			</div>
		</FooterDiv>
	);
};