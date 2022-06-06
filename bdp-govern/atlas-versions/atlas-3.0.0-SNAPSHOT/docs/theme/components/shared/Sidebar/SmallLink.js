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
import { SFC, useState, useEffect } from "react";
import { WindowLocation } from "@reach/router";
import styled from "styled-components";

import { get as themeGet } from "../../../utils/theme";

const sidebarPrimary = themeGet("colors.sidebarPrimary");
const primaryColor = themeGet("colors.primary");

export const LinkProps = {
  to: "",
  onClick: React.MouseEventHandler
};
const Link = styled.a`
  position: relative;
  font-size: 14px;
  padding: 0 0 5px 16px;
  text-decoration: none;
  opacity: 0.5;
  transition: opacity 0.2s;

  &,
  &:visited,
  &.active {
    color: ${themeGet("colors.sidebarText")};
  }

  &.active {
    opacity: 1;
  }

  &:before {
    z-index: 1;
    position: absolute;
    display: block;
    content: "";
    top: 1px;
    left: 0;
    width: 0;
    height: 20px;
    background: ${p => sidebarPrimary(p) || primaryColor(p)};
    transition: width 0.2s;
  }

  &.active:before {
    width: 2px;
  }
`;
Link.defaultProps = LinkProps;

const SmallLinkProps = Object.assign({}, LinkProps, {
  as: null,
  slug: ""
  //location: WindowLocation
});

export const SmallLink = ({ as: Component, slug, location, ...props }) => {
  const [isActive, setActive] = useState(false);

  useEffect(() => {
    const decodedHash = decodeURI(location.hash);
    const currentHash = decodedHash && decodedHash.slice(1, Infinity);
    setActive(Boolean(slug === currentHash));
  }, [location]);

  return (
    <Link as={Component} {...props} className={isActive ? "active" : ""} />
  );
};

SmallLink.defaultProps = SmallLinkProps;
