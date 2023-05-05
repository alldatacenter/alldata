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

import { Progress, Modal, Button } from 'antd';
import i18n from 'i18n';
import React from 'react';
import { TimeInput, transToStr, checkReg, checkMsg } from './time-input';
import './time-trace.scss';
import moment, { Moment } from 'moment';
import { Form } from 'dop/pages/form-editor/index';

const numberOr = (v: string | number, defaultV: number) => {
  return typeof v === 'number' ? v : defaultV;
};

// 已记录logged, 当前记录spent，剩余remain，预估estimate
const calculatePercent = (logged: number, spent: number, remain: number, estimate: number) => {
  const totalUse = logged + spent + remain;
  if (totalUse > estimate) {
    // 总耗时大于预估耗时，以总耗时为总数
    return [
      Math.max(estimate - remain, 0), // 蓝色：预估-剩余
      Math.max(totalUse - estimate, 0), // 黄色：已记录+当前记录+剩余-预估
      remain, // 灰色：剩余
    ].map((n) => (n / totalUse) * 100);
  }
  return [
    // 总耗时小于预估耗时，以预估耗时为总数
    logged + spent, // 蓝色
    0, // 黄色
    remain, // 灰色
  ].map((n) => (n / estimate) * 100);
};

interface IZTraceBarProps {
  logged: number;
  spent: number;
  remain: number;
  estimate: number;
  active?: boolean;
  onClick?: (e: any) => void;
}
const TimeTraceBar = React.forwardRef(
  ({ logged, spent, remain, estimate, active = false, onClick }: IZTraceBarProps, ref) => {
    const _logged = numberOr(logged, 0);
    const _spent = numberOr(spent, 0);
    const _remain = numberOr(remain, 0);
    const _estimate = numberOr(estimate, 0);
    const [blue, yellow] = calculatePercent(_logged, _spent, _remain, _estimate);
    return (
      <div className={`time-trace ${active ? 'active-hover' : ''}`} onClick={onClick} ref={ref}>
        <Progress strokeColor="#f47201" showInfo={false} successPercent={blue} percent={blue + yellow} size="small" />
        <div className="text-sub flex justify-between items-center text-xs">
          <span>
            {_logged + _spent
              ? `${i18n.t('dop:logged')} ${transToStr(_logged + _spent)}`
              : i18n.t('dop:no time logged')}
          </span>
          {_remain ? (
            <span>
              {i18n.t('dop:Remaining')} {transToStr(_remain)}
            </span>
          ) : null}
        </div>
      </div>
    );
  },
);

interface IProps {
  value?: ISSUE.issueManHour;
  disabled?: boolean;
  isModifiedRemainingTime?: boolean;
  onChange?: (e: any) => void;
}

const defaultValue = {
  elapsedTime: undefined,
  remainingTime: undefined,
  estimateTime: undefined,
  thisElapsedTime: 0,
  startTime: '',
  workContent: '',
};
export const TimeTrace = React.forwardRef(
  ({ value = { ...defaultValue } as any, onChange = () => {}, disabled, isModifiedRemainingTime }: IProps, ref) => {
    const [editData, setEditData] = React.useState({} as ISSUE.issueManHour);
    const [modalVis, setModalVis] = React.useState(false);
    const form = React.useRef();

    const onSpentTimeChange = (v: number | string) => {
      const curForm = form && (form.current as any);
      if (curForm && typeof v === 'number') {
        let remain = value.estimateTime - value.elapsedTime;
        remain = isNaN(remain) ? 0 : remain;
        // 剩余时间=预估-已记录
        curForm.setFieldValue('remainingTime', Math.max(remain - v, 0));
      }
      return v;
    };

    // 如果没保存过剩余时间，则默认值为：预估-已用，保存过，则用后端给的
    let defaultRemaining = value.estimateTime - value.elapsedTime;
    defaultRemaining = isNaN(defaultRemaining) ? 0 : Math.max(defaultRemaining, 0);
    defaultRemaining = isModifiedRemainingTime ? value.remainingTime : defaultRemaining;
    const fields = [
      {
        label: i18n.t('dop:Time spent'),
        key: 'thisElapsedTime',
        getComp: () => (
          <TimeInput
            placeholder={checkMsg}
            onChange={onSpentTimeChange}
            tooltip={
              <div>
                {i18n.t('dop:Format must be 2w 3d 4h 5m')} <br />. w = {i18n.t('week')}
                <br />. d = {i18n.t('common:day')}
                <br />. h = {i18n.t('common:hour')}
                <br />. m = {i18n.t('common:minutes')}
                <br />
              </div>
            }
          />
        ),
        rules: [
          {
            validator: (v: number | string) => [
              v === undefined || v === '' || v === 0 ? true : checkReg.test(`${transToStr(v)} `),
              checkMsg,
            ],
          },
        ],
      },
      {
        label: i18n.t('dop:Time remaining'),
        key: 'remainingTime',
        getComp: () => (
          <TimeInput
            placeholder={checkMsg}
            tooltip={
              <div>
                {i18n.t('dop:Format must be 2w 3d 4h 5m')} <br />. w = {i18n.t('week')}
                <br />. d = {i18n.t('common:day')}
                <br />. h = {i18n.t('common:hour')}
                <br />. m = {i18n.t('common:minutes')}
                <br />
              </div>
            }
          />
        ),
        defaultValue: defaultRemaining,
        rules: [
          {
            validator: (v: number | string) => [
              v === undefined || v === '' || v === 0 ? true : checkReg.test(`${transToStr(v)} `),
              checkMsg,
            ],
          },
        ],
      },
      {
        label: i18n.t('dop:Date started'),
        component: 'datePicker',
        key: 'startTime',
        componentProps: {
          dateType: 'date',
          showTime: true,
          className: 'w-full',
        },
        labelTip: i18n.t('dop:The start time of this counted time, not the start time of the issue'),
        type: 'datePicker',
        fixIn: (v: string) => (v ? moment(v) : null),
        fixOut: (m?: Moment) => m && m.format('YYYY-MM-DD HH:mm:ss'),
      },
      {
        label: i18n.t('dop:Work description'),
        component: 'textarea',
        key: 'workContent',
        type: 'textarea',
        componentProps: {
          maxLength: 2000,
        },
      },
    ];

    const handleCancel = () => {
      setModalVis(false);
      setEditData(defaultValue as any);
      setTimeout(() => {
        const curForm = form && (form.current as any);
        if (curForm) {
          curForm.reset();
        }
      });
    };

    const handleOk = () => {
      const curForm = form && (form.current as any);
      if (curForm) {
        curForm.onSubmit((formData: Obj) => {
          const { remainingTime, thisElapsedTime, ...rest } = formData;
          onChange({
            ...rest,
            remainingTime: remainingTime === '' ? 0 : remainingTime,
            thisElapsedTime: thisElapsedTime === '' ? 0 : thisElapsedTime,
          });
          handleCancel();
        });
      }
    };

    return (
      <div>
        <TimeTraceBar
          active={!disabled}
          logged={value.elapsedTime}
          spent={0}
          ref={ref}
          remain={value.remainingTime}
          estimate={value.estimateTime}
          onClick={() => !disabled && setModalVis(true)}
        />
        {!disabled && (
          <Modal
            title={i18n.t('dop:Time tracking')}
            visible={modalVis}
            onCancel={handleCancel}
            destroyOnClose
            footer={[
              <Button key="back" onClick={handleCancel}>
                {i18n.t('cancel')}
              </Button>,
              <Button key="submit" type="primary" onClick={handleOk}>
                {i18n.t('ok')}
              </Button>,
            ]}
          >
            <TimeTraceBar
              logged={value.elapsedTime}
              spent={editData.thisElapsedTime || 0}
              remain={editData.remainingTime ?? defaultRemaining}
              estimate={value.estimateTime}
            />
            <div className="my-4">
              {i18n.t('dop:The original estimated time for this event is')} {transToStr(value.estimateTime)}
            </div>
            <Form formRef={form} fields={fields} onChange={setEditData} />
          </Modal>
        )}
      </div>
    );
  },
);
