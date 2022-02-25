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

import * as colors from "./colors";

export const light = {
  ...colors,
  primary: colors.green,
  text: colors.grayDark,
  link: colors.blue,
  footerText: colors.grayDark,
  sidebarBg: colors.grayExtraLight,
  sidebarText: colors.dark,
  sidebarHighlight: null,
  sidebarBorder: colors.grayLight,
  background: colors.white,
  border: colors.greenLight,
  theadColor: colors.gray,
  theadBg: colors.grayExtraLight,
  tableColor: colors.dark,
  tooltipBg: colors.dark,
  tooltipColor: colors.grayExtraLight,
  codeBg: colors.grayExtraLight,
  codeColor: colors.gray,
  preBg: colors.grayExtraLight,
  blockquoteBg: colors.grayExtraLight,
  blockquoteBorder: colors.grayLight,
  blockquoteColor: colors.gray,
  propsText: colors.gray,
  propsBg: colors.grayUltraLight
};

export const dark = {
  ...colors,
  primary: colors.skyBlue,
  text: colors.grayExtraLight,
  link: colors.skyBlue,
  footerText: colors.grayLight,
  sidebarBg: colors.dark,
  sidebarText: colors.grayLight,
  sidebarHighlight: null,
  sidebarBorder: colors.grayDark,
  background: colors.grayExtraDark,
  border: colors.grayDark,
  theadColor: colors.gray,
  theadBg: colors.grayDark,
  tableColor: colors.grayExtraLight,
  tooltipBg: colors.dark,
  tooltipColor: colors.grayExtraLight,
  codeBg: colors.gray,
  codeColor: colors.grayExtraLight,
  preBg: colors.grayDark,
  blockquoteBg: colors.grayDark,
  blockquoteBorder: colors.gray,
  blockquoteColor: colors.gray,
  propsText: colors.grayLight,
  propsBg: colors.dark
};
