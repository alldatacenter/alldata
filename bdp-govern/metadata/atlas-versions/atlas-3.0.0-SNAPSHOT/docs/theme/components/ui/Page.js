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
import { SFC, Fragment } from "react";
import { PageProps, useConfig } from "../../../docz-lib/docz/dist";
import Edit from "react-feather/dist/icons/edit-2";
import styled from "styled-components";

import { ButtonLink } from "./Button";
import { GithubLink, Sidebar, Main } from "../shared";
import { get } from "../../utils/theme";
import { mq } from "../../styles/responsive";
import Header from "../shared/Header";
import Utils from "../../utils/utils";

const Wrapper = styled.div`
  flex: 1;
  color: ${get("colors.text")};
  background: ${get("colors.background")};
  min-width: 0;
  position: relative;
  padding-top: 50px;
`;

export const Container = styled.div`
  box-sizing: border-box;
  margin: 0 auto;

  ${mq({
    width: ["100%", "100%", "95%"],
    padding: ["20px", "0 30px 36px"]
  })}

  ${get("styles.container")};
`;

const EditPage = styled(ButtonLink.withComponent("a"))`
  display: flex;
  align-items: center;
  justify-content: center;
  position: absolute;
  padding: 2px 8px;
  margin: 8px;
  border-radius: ${get("radii")};
  border: 1px solid ${get("colors.border")};
  opacity: 0.7;
  transition: opacity 0.4s;
  font-size: 14px;
  color: ${get("colors.text")};
  text-decoration: none;
  text-transform: uppercase;

  &:hover {
    opacity: 1;
    background: ${get("colors.border")};
  }

  ${mq({
    visibility: ["hidden", "hidden", "visible"],
    top: [0, -60, 32],
    right: [0, 0, 40]
  })};
`;

const EditIcon = styled(Edit)`
  margin-right: 5px;
`;
export const Page = ({ children, doc: { link, fullpage, edit = false } }) => {
  const { repository } = useConfig();
  const { props } = children;
  const show = Utils.pagesForGithubLink.toString().includes(props.doc.name);
  const content = (
    <Fragment>
      {link && edit && (
        <EditPage href={link} target="_blank">
          <EditIcon width={14} /> Edit page
        </EditPage>
      )}
      <Header showGithubLink={show} />
      {children}
    </Fragment>
  );
  return (
    <Main>
      {repository && <GithubLink repository={repository} />}
      {!fullpage && <Sidebar />}
      <Wrapper>{fullpage ? content : <Container>{content}</Container>}</Wrapper>
    </Main>
  );
};
