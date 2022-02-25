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
import { theme, ComponentsProvider } from "../docz-lib/docz/dist";
import get from "lodash/get";

import * as modes from "./styles/modes";
import { components } from "./components/ui";
import { Global } from "./styles/global";
import { config } from "./config";
import { ThemeProvider } from "./utils/theme";
import { LegalFooter } from "./components/shared/LegalFooter";

const Theme = ({ children }) => {
  return (
    <ThemeProvider>
      <Global />
      <ComponentsProvider components={components}>
        {children}
      </ComponentsProvider>
      <LegalFooter />
    </ThemeProvider>
  );
};

export const enhance = theme(config, ({ mode, codemirrorTheme, ...config }) => {
  return {
    ...config,
    mode,
    codemirrorTheme: codemirrorTheme || `docz-${mode}`,
    colors: {
      ...get(modes, mode),
      ...config.colors
    }
  };
});

export default enhance(Theme);
export { components };
