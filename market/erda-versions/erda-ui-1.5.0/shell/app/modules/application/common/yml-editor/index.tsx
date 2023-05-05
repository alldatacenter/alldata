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
import PipelineEditor from './pipeline-editor';
import DiceEditor from './dice-editor';
import { isPipelineYml, isDiceYml } from './utils';

export interface IYmlEditorProps {
  fileName: string;
  ops?: any;
  editing: boolean;
  viewType?: string;
  content?: string;
  onUpdateViewType?: (val: string) => void;
}

const Module = (props: IYmlEditorProps) => {
  return isPipelineYml(props.fileName) ? (
    <PipelineEditor {...props} />
  ) : isDiceYml(props.fileName) ? (
    <DiceEditor {...props} />
  ) : null;
};

export default Module;
