import { Checkbox, Form, Input, Select, Space } from 'antd';
import useI18NPrefix, { prefixI18N } from 'app/hooks/useI18NPrefix';
import { FC, useMemo } from 'react';
import { TimeModes } from '../../constants';

const timeModeOptions = [
  { label: prefixI18N('global.time.minute'), value: TimeModes.Minute },
  { label: prefixI18N('global.time.hour'), value: TimeModes.Hour },
  { label: prefixI18N('global.time.day'), value: TimeModes.Day },
  { label: prefixI18N('global.time.week'), value: TimeModes.Week },
  { label: prefixI18N('global.time.month'), value: TimeModes.Month },
  { label: prefixI18N('global.time.year'), value: TimeModes.Year },
];
const minutePeriodOptions = Array.from({ length: 50 }, (_, i) => {
  const n = i + 10;
  return { label: n, value: n };
});

const minuteOptions = Array.from({ length: 60 }, (_, i) => {
  const n = `${`0${i}`.slice(-2)} ${prefixI18N('global.time.m')}`;
  return { label: n, value: i };
});

const hourOptions = Array.from({ length: 24 }, (_, i) => {
  const n = `${`0${i}`.slice(-2)} ${prefixI18N('global.time.h')}`;
  return { label: n, value: i };
});

const dayOptions = Array.from({ length: 31 }, (_, i) => {
  const n = `${`0${i + 1}`.slice(-2)} ${prefixI18N('global.time.d')}`;
  return { label: n, value: i + 1 };
});
const monthOptions = Array.from({ length: 12 }, (_, i) => {
  const n = `${`0${i + 1}`.slice(-2)} ${prefixI18N('global.time.month')}`;
  return { label: n, value: i + 1 };
});

const weekOptions = [
  { label: prefixI18N('global.time.sun'), value: 1 },
  { label: prefixI18N('global.time.mon'), value: 2 },
  { label: prefixI18N('global.time.tues'), value: 3 },
  { label: prefixI18N('global.time.wednes'), value: 4 },
  { label: prefixI18N('global.time.thurs'), value: 5 },
  { label: prefixI18N('global.time.fri'), value: 6 },
  { label: prefixI18N('global.time.satur'), value: 7 },
];

export interface ExecuteFormItemProps {
  periodInput?: boolean;
  periodUnit: TimeModes;
  onPeriodUnitChange: (v: TimeModes) => void;
  onPeriodInputChange: (v: boolean) => void;
}
export const ExecuteFormItem: FC<ExecuteFormItemProps> = ({
  periodUnit: timeMode,
  onPeriodUnitChange,
  periodInput: isInput,
  onPeriodInputChange,
}) => {
  const t = useI18NPrefix('schedule.editor.basicBaseForm.executeFormItem');

  const modeSelect = useMemo(() => {
    return (
      <Form.Item name="periodUnit">
        <Select
          options={timeModeOptions}
          style={{ width: 80 }}
          onChange={v => onPeriodUnitChange(v as TimeModes)}
        />
      </Form.Item>
    );
  }, [onPeriodUnitChange]);
  const daySelect = useMemo(() => {
    return (
      <Form.Item name="day">
        <Select options={dayOptions} style={{ width: 80 }} />
      </Form.Item>
    );
  }, []);
  const hourSelect = useMemo(() => {
    return (
      <Form.Item name="hour">
        <Select options={hourOptions} style={{ width: 100 }} />
      </Form.Item>
    );
  }, []);
  const minuteSelect = useMemo(() => {
    return (
      <Form.Item name="minute">
        <Select options={minuteOptions} style={{ width: 100 }} />
      </Form.Item>
    );
  }, []);
  const timeContent = useMemo(() => {
    switch (timeMode) {
      case TimeModes.Minute:
        return (
          <>
            {t('per')}
            <Form.Item name="minute">
              <Select options={minutePeriodOptions} style={{ width: 100 }} />
            </Form.Item>
            {modeSelect}
          </>
        );
      case TimeModes.Hour:
        return (
          <>
            {t('per')} {modeSelect} {t('of')}
            {minuteSelect}
          </>
        );
      case TimeModes.Day:
        return (
          <>
            {t('per')} {modeSelect} {t('of')}
            {hourSelect}
            {minuteSelect}
          </>
        );
      case TimeModes.Week:
        return (
          <>
            {t('per')} {modeSelect} {t('of')}
            <Form.Item name="weekDay">
              <Select options={weekOptions} style={{ width: 100 }} />
            </Form.Item>
            {hourSelect}
            {minuteSelect}
          </>
        );
      case TimeModes.Month:
        return (
          <>
            {t('per')} {modeSelect} {t('of')} {daySelect}
            {hourSelect}
            {minuteSelect}
          </>
        );
      case TimeModes.Year:
        return (
          <>
            {t('per')} {modeSelect}
            {t('of')}
            <Form.Item name="month">
              <Select options={monthOptions} style={{ width: 80 }} />
            </Form.Item>
            {daySelect}
            {hourSelect}
            {minuteSelect}
          </>
        );
      default:
        return;
    }
  }, [timeMode, modeSelect, daySelect, minuteSelect, hourSelect, t]);
  return (
    <Form.Item label={t('executionTimeSetting')} required>
      <Space align="baseline">
        {isInput ? (
          <Form.Item
            name="cronExpression"
            rules={[{ required: true, message: t('expressionIsRequired') }]}
          >
            <Input
              placeholder={t('pleaseEnterCronExpression')}
              style={{ width: 300 }}
            />
          </Form.Item>
        ) : (
          timeContent
        )}
        <Form.Item name="setCronExpressionManually" valuePropName="checked">
          <Checkbox onChange={e => onPeriodInputChange(e.target.checked)}>
            {t('manualInput')}
          </Checkbox>
        </Form.Item>
      </Space>
    </Form.Item>
  );
};
