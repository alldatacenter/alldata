import { EditableProTable, ProColumns } from '@ant-design/pro-table';
import { useCallback, useMemo, useState } from 'react';
import { css } from 'styled-components/macro';
import { uuidv4 } from 'utils/utils';

const tableStyle = css`
  .ant-card-body {
    padding: 0;
  }
`;

interface ConfigurationProps<T> {
  columns: ProColumns<T>[];
  creatorButtonText?: string;
  defaultRecord?: Partial<T>;
  disabled?: boolean;
  value?: T[];
  onChange?: (config: T[]) => void;
}

export function Configuration<T extends { id: string }>({
  columns,
  creatorButtonText,
  defaultRecord,
  disabled,
  value: valueProp,
  onChange,
}: ConfigurationProps<T>) {
  const [innerValue, setInnerValue] = useState<T[]>([]);

  const value = useMemo(
    () => (valueProp !== void 0 ? valueProp : innerValue),
    [valueProp, innerValue],
  );

  const setValue = useMemo(
    () => (onChange !== void 0 ? onChange : setInnerValue),
    [onChange],
  );

  const getDefaultRecord = useCallback(
    () => ({ id: uuidv4(), ...defaultRecord } as T),
    [defaultRecord],
  );

  return (
    <EditableProTable<T>
      size="small"
      rowKey="id"
      columns={columns}
      value={value}
      onChange={setValue}
      recordCreatorProps={
        disabled
          ? false
          : {
              record: () => getDefaultRecord(),
              ...(creatorButtonText && { creatorButtonText }),
            }
      }
      css={tableStyle}
      bordered
    />
  );
}
