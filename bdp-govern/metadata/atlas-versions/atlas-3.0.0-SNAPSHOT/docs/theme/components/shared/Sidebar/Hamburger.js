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
import { mq } from "../../../styles/responsive";

const OpenProps = {
  opened: true
};

const IconFirst = p => (!p.opened ? "0px" : "10px");
const IconMiddle = p => (!p.opened ? "1" : "0");
const IconLast = p => (!p.opened ? "0px" : "-6px");
const IconRotate = p => (!p.opened ? "0deg" : "45deg");

const Icon = styled.div`
  position: relative;
  width: 23px;
  height: 32px;
  transform: translateX(${p => (p.opened ? "-2px" : "-1px")})
    translateY(${p => (p.opened ? "0" : "2px")})
    scale(${p => (p.opened ? 0.8 : 1)});
`;
Icon.defaultProps = OpenProps;
const sidebarBg = get("colors.sidebarBg");
const sidebarPrimary = get("colors.sidebarPrimary");
const sidebarText = get("colors.sidebarText");
const primaryColor = get("colors.primary");
const backgroundColor = get("colors.background");
const textColor = get("colors.text");

const IconLine = styled.span`
  content: "";
  display: block;
  position: absolute;
  width: 100%;
  height: 2px;
  left: 0;
  right: 0;
  background: ${p => (p.opened ? sidebarText(p) : textColor(p))};
  transition: transform 0.3s, opacity 0.3s;

  &:nth-of-type(1) {
    top: -2px;
    transform: translateY(${IconFirst}) rotate(${IconRotate});
  }

  &:nth-of-type(2) {
    top: 6px;
    opacity: ${IconMiddle};
  }

  &:nth-of-type(3) {
    top: 14px;
    transform: translateY(${IconLast}) rotate(-${IconRotate});
  }
`;
IconLine.defaultProps = OpenProps;

const translateX = p => (!p.opened ? "10px" : "-6px");
const translateY = p => (!p.opened ? "4px" : "0px");

const radii = get("radii");

const ToggleButton = styled.button`
  cursor: pointer;
  z-index: 99;
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 5px 6px;
  width: 33px;
  height: 30px;
  top: ${p => (p.opened ? "3px" : "2px")};
  right: ${p => (p.opened ? "-39px" : "-27px")};
  transform: translateX(${translateX}) translateY(${translateY});
  transition: transform 0.3s;
  outline: none;
  border: none;
  background: ${p => (p.opened ? sidebarBg(p) : backgroundColor(p))};
  border-radius: ${p => (p.opened ? `0 0 ${radii(p)} 0` : `${radii(p)}`)};

  &:before {
    position: absolute;
    content: "";
    top: -3px;
    left: 0;
    width: calc(100% + 1px);
    height: ${p => (p.opened ? "3px" : 0)};
    background: ${p => sidebarPrimary(p) || primaryColor(p)};
  }

  ${mq({
    display: ["block", "block", "block", "none"]
  })};
`;
ToggleButton.defaultProps = OpenProps;

// interface HamburgerProps extends OpenProps {
//   onClick: (ev: React.SyntheticEvent<any>) => void
// }

const HamburgerProps = Object.assign({}, OpenProps, {
  onClick: ev => null
});

export const Hamburger = ({ opened, onClick }) => (
  <ToggleButton opened={opened} onClick={onClick}>
    <Icon opened={opened}>
      <IconLine opened={opened} />
      <IconLine opened={opened} />
      <IconLine opened={opened} />
    </Icon>
  </ToggleButton>
);
