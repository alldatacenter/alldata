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

import { Component } from 'react';

export interface IPointInfo {
  ITEM_WIDTH: number;
  ITEM_HEIGHT: number;
  PADDING_TOP: number;
  PADDING_LEFT: number;
  ITEM_MARGIN_BOTTOM: number;
  ITEM_MARGIN_RIGHT: number;
  ICON_WIDTH: number;
  RX: number;
}

export default abstract class PointComponentAbstract<P, S> extends Component<P, S> {
  public abstract info: IPointInfo;
}
