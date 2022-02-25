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
  opened: true,
  hasActive: true
};
export const SubMenu = props => {
  const { item, sidebarToggle, handleActiveMenu } = props;
  const opened = Utils.checkMenuIsOPen(props);
  const show = opened;
  const hasChildren = !item.href && item.submenu && item.submenu.length > 0;
  const hasToggle = !item.href && !item.route;
  const hasSubMenu = "";
  const handleToggle = ev => {
    ev.preventDefault();
    handleActiveMenu(item);
  };
  const lengthOfSubMenu = item.submenu && item.submenu.length;

  let output = "";

  if (lengthOfSubMenu > 0 && item.name !== item.submenu[0].name) {
    output = (
      <Wrapper>
        <MenuLink item={item} {...(hasToggle && { onClick: handleToggle })}>
          {`${item.name}`}
          {hasChildren && (
            <Icon opened={show}>
              <ChevronDown size={15} />
            </Icon>
          )}
        </MenuLink>

        {hasChildren && (
          <List opened={show}>
            {item.submenu &&
              item.menu.map(dataItem => (
                <dt key={dataItem.name}>
                  <MenuLink item={dataItem} onClick={sidebarToggle}>
                    {`${dataItem.name}`}
                  </MenuLink>
                </dt>
              ))}
          </List>
        )}
      </Wrapper>
    );
  } else {
    output = (
      <Wrapper>
        {item.submenu &&
          item.submenu.map(dataItem => (
            <dt key={dataItem.name}>
              <MenuLink item={dataItem}>{`${dataItem.name}`}</MenuLink>
            </dt>
          ))}
      </Wrapper>
    );
  }
  return output;
};
SubMenu.defaultProps = MenuProps;
SubMenu.defaultProps = MenuState;