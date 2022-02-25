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
import { useMemo, SFC } from "react";
import { useConfig, useDocs } from "../../../docz-lib/docz/dist";
import styled from "styled-components";

import { get } from "../../utils/theme";

export const LinkStyled = styled.a`
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

const getSeparator = (separator, href) => {
  if (typeof window === "undefined") return null;
  return [
    location.pathname
      .split(separator)
      .slice(0, -1)
      .join(separator)
      .slice(1),
    (href || "").replace(/^(?:\.\/)+/gi, "")
  ].join("/");
};

export const Link = ({ href, ...props }) => {
  const { separator, linkComponent: Link } = useConfig();
  const docs = useDocs();
  const toCheck = useMemo(() => getSeparator(separator, href), [
    separator,
    href
  ]);

  const matched = docs && docs.find(doc => doc.filepath === toCheck);
  const nHref = matched ? matched.route : href;
  const isInternal = nHref && nHref.startsWith("/");

  return isInternal ? (
    <LinkStyled as={Link} {...props} to={nHref} />
  ) : (
    <LinkStyled {...props} href={nHref} />
  );
};