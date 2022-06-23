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
import { useState } from "react";
import ChevronDown from "react-feather/dist/icons/chevron-down";
import styled from "styled-components";

import { MenuLink } from "./MenuLink";
import { get } from "../../../utils/theme";

import { SubMenu } from "./SubMenu";
import Utils from "../../../utils/utils";
export const MenuItem = {
  id: "",
  name: "",
  route: "",
  href: "",
  menu: [],
  order: Number,
  parent: ""
};

const Wrapper = styled.div`
  display: flex;
  flex-direction: column;
`;

const OpenedProps = {
  opened: false
};

const List = styled.dl`
  flex: 1;
  overflow-y: auto;
  visibility: ${p => (p.opened ? "visible" : "hidden")};
  max-height: ${p => (p.opened ? "auto" : "0px")};
`;

List.defaultProps = OpenedProps;

const iconRotate = p => (p.opened ? "-180deg" : "0deg");

const Icon = styled.div`
  position: absolute;
  top: 50%;
  right: 20px;
  transform: translateY(-50%) rotate(${iconRotate});
  transform-origin: 50% 50%;
  transition: transform 0.3s;
  & svg {
    stroke: ${get("colors.sidebarText")};
  }
`;
Icon.defaultProps = OpenedProps;
const MenuProps = {
  item: MenuItem,
  sidebarToggle: null,
  collapseAll: true
};

const MenuState = {
  opened: false,
  hasActive: false
};
export const Menu = props => {
  const { item, sidebarToggle, handleActiveMenu, activeMenu } = props;
  const opened = Utils.checkMenuIsOPen(props);
  const show = opened;
  const hasChildren = !item.href && item.menu && item.menu.length > 0;
  const hasToggle = !item.href && !item.route;
  const handleToggle = ev => {
    ev.preventDefault();
    handleActiveMenu(item);
  };
  const options = { handleActiveMenu, activeMenu };
  let OutputHtml = (
    <Wrapper>
      <MenuLink item={item} {...(hasToggle && { onClick: handleToggle })}>
        {` ${item.name} `}
        {hasChildren && (
          <Icon opened={show}>
            <ChevronDown size={15} />
          </Icon>
        )}
      </MenuLink>
      {hasChildren && (
        <List opened={show}>
          {item.menu &&
            item.menu.map(dataItem => (
              <List opened={show} key={dataItem.name}>
                <SubMenu item={dataItem} {...options} />
              </List>
            ))}
        </List>
      )}
    </Wrapper>
  );

  if (!hasChildren && !hasToggle) {
    OutputHtml = (
      <Wrapper>
        <MenuLink item={item} {...(opened && { handleToggle })}>
          {`${item.name} `}
        </MenuLink>
      </Wrapper>
    );
  }
  return OutputHtml;
};
Menu.defaultProps = MenuProps;
Menu.defaultProps = MenuState;