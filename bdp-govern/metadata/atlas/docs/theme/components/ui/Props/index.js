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
import { useState, useMemo } from "react";
import {
  PropsComponentProps,
  useComponents
} from "../../../../docz-lib/docz/dist";
import styled from "styled-components";
import { PropsRaw } from "./PropsRaw";
import { PropsTable } from "./PropsTable";

const Container = styled.div`
  margin: 20px 0;
`;

export const Props = ({ title, isRaw, isToggle, ...props }) => {
  const [isOpen, setIsOpen] = useState(true);

  const components = useComponents();
  const Title = useMemo(
    () => styled(components.H3 || "h3")`
      padding: 8px 0;
      position: relative;
      ${!isRaw ? "margin-bottom: 0;" : ""}
      ${!isOpen || isRaw ? "border-bottom: 1px solid rgba(0, 0, 0, 0.1);" : ""}

      ${
        isToggle
          ? `
        cursor: pointer;
        padding-right: 40px;

        &::after {
          content: '';
          position: absolute;
          top: 50%;
          right: 16px;
          transform: translateY(-50%) ${
            isOpen ? "rotate(-135deg)" : "rotate(45deg)"
          };
          ${!isOpen ? "margin-top: -2px;" : ""}
          width: 8px;
          height: 8px;
          border-bottom: 2px solid;
          border-right: 2px solid;
        }
      `
          : ""
      }
    `,
    [isOpen]
  );

  const titleProps = isToggle
    ? {
        onClick: () => setIsOpen(open => !open),
        onkeydown: () => setIsOpen(open => !open),
        role: "button",
        tabindex: 0
      }
    : {};

  return (
    <Container>
      {(!!title || isToggle) && (
        <Title {...titleProps}>{title || "Component props"}</Title>
      )}
      {isOpen && (
        <div>{isRaw ? <PropsRaw {...props} /> : <PropsTable {...props} />}</div>
      )}
    </Container>
  );
};