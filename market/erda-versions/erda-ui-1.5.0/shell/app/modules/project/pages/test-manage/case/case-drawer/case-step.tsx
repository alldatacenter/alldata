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

import { Icon as CustomIcon } from 'common';
import { useListDnD } from 'common/use-hooks';
import { reorder } from 'common/utils';
import { map, cloneDeep } from 'lodash';
import { Input } from 'antd';
import React from 'react';
import i18n from 'i18n';
import './case-step.scss';

const { TextArea } = Input;
interface IProps {
  value: IStep[] | null;
  onChange: (list: IStep[], autoSave: boolean) => any;
}

interface IStep {
  step: string;
  result: string;
  invalid?: boolean;
}

export const getEmptyStep = () => {
  return {
    step: '',
    result: '',
  };
};

const CaseSteps = ({ value, onChange }: IProps) => {
  const [steps, setSteps] = React.useState(value || []);
  React.useEffect(() => {
    setSteps(value || []);
  }, [value]);

  const output = React.useCallback(
    (data: IStep[], autoSave = false) => {
      const list = data.map((item) => {
        return { step: item.step, result: item.result };
      });
      onChange(list, autoSave);
    },
    [onChange],
  );

  const updateStep = (i: number, k: string, v: string) => {
    const tempStep = cloneDeep(steps);
    const preValue = tempStep[i][k];
    tempStep[i][k] = v;
    tempStep[i].invalid = preValue && !v;
    setSteps([...tempStep]);
    output([...tempStep]);
  };

  const handleDelete = (num: number) => {
    const newSteps = steps.filter((item, i) => i !== num);
    setSteps(newSteps);
    output(newSteps, true);
  };

  const onMove = React.useCallback(
    (dragIndex: number, hoverIndex: number) => {
      const newList = reorder(steps, dragIndex, hoverIndex);
      setSteps(newList);
      output(newList, true);
    },
    [output, steps],
  );

  if (!steps.length) {
    return <span className="text-holder">{i18n.t('none')}</span>;
  }

  return (
    <div className="case-step-list">
      {map(steps, (step, i) => {
        return (
          <StepItem key={i} step={step} index={i} updateStep={updateStep} handleDelete={handleDelete} onMove={onMove} />
        );
      })}
    </div>
  );
};

const StepItem = ({ step, index, updateStep, handleDelete, onMove }: any) => {
  const [dragRef, previewRef] = useListDnD({
    type: 'case-step',
    index,
    onMove,
  });

  return (
    <div ref={previewRef} key={index} className="case-step case-index-hover">
      <div className="flex justify-between items-center step-detail">
        <div ref={dragRef} className="case-index-block">
          <span className={`index-num ${step.invalid === true ? 'error' : ''}`}>{index + 1}</span>
          <CustomIcon className="drag-icon" type="px" />
        </div>
        <TextArea
          autoSize
          className="flex-1"
          placeholder={i18n.t('dop:input step')}
          value={step.step}
          onChange={(e) => updateStep(index, 'step', e.target.value)}
        />
        <CustomIcon className="delete-icon hover-active" type="sc1" onClick={() => handleDelete(index)} />
      </div>
      <div className="flex justify-between items-center result-detail">
        <TextArea
          autoSize
          placeholder={i18n.t('dop:input expected result')}
          value={step.result}
          onChange={(e) => updateStep(index, 'result', e.target.value)}
        />
      </div>
    </div>
  );
};

export default CaseSteps;
