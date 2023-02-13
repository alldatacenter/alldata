// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import React from 'react';
import { TagsRow } from 'common';
import { colorMap } from 'config-page/utils';

const CP_TAGS = (props: CP_TAGS.Props) => {
  const { props: configProps, data } = props || {};
  const { visible = true, ...rest } = configProps || {};

  if (!visible) return null;

  return <TagsRow colorMap={colorMap} {...rest} labels={data?.labels} />;
};

export default CP_TAGS;
