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

import { map, isEmpty } from 'lodash';
import { Select } from 'antd';
import React from 'react';
import { useEffectOnce } from 'react-use';
import { useTempPaging } from 'common/use-hooks';
import { getProjectIterations } from 'app/modules/project/services/project-iteration';
import routeInfoStore from 'core/stores/route';
import moment from 'moment';
import iterationStore from 'app/modules/project/stores/iteration';
import i18n from 'i18n';

const { Option } = Select;

interface IProps {
  addAllOption?: boolean;
  value?: number[] | number | 'ALL';
  fullWidth?: boolean;
  disabled?: boolean;
  className?: string;
  autoSelectFirst?: boolean;
  disabledBacklog?: boolean;
  allowClear?: boolean;
  placeholder?: string;
  mode?: 'default' | 'multiple';
  width?: string;
  onChange?: (v: number[] | number) => void;
}
const noop = () => {};
export default ({
  value,
  onChange = noop,
  fullWidth = false,
  width = '268px',
  autoSelectFirst = false,
  className,
  disabled = false,
  disabledBacklog = false,
  allowClear = false,
  placeholder,
  mode = 'default',
  addAllOption = false,
  ...rest
}: IProps) => {
  const { projectId } = routeInfoStore.useStore((s) => s.params);
  const [iterationList, paging, loading, load, clear] = useTempPaging<ITERATION.Detail>({
    service: getProjectIterations,
  });
  useEffectOnce(() => {
    if (!iterationList.length) {
      if (!loading) {
        load({ projectID: projectId, pageNo: 1, pageSize: 100, withoutIssueSummary: true }).then(
          ({ list }: { list: ITERATION.Detail[] }) => {
            if (!isEmpty(list) && !value && autoSelectFirst) {
              let currentId = 0;
              list.forEach((a) => {
                if (
                  !currentId &&
                  moment().isBetween(moment(a.startedAt).startOf('day'), moment(a.finishedAt).endOf('day'))
                ) {
                  currentId = a.id;
                }
              });
              if (mode === 'multiple') {
                onChange([currentId || list[0].id]);
              } else {
                onChange(currentId || list[0].id);
              }
            }
          },
        );
      }
    } else if (autoSelectFirst) {
      if (mode === 'multiple') {
        onChange([iterationList[0].id]);
      } else {
        onChange(iterationList[0].id);
      }
    }
    return () => clear();
  });

  const iterationOption = React.useMemo(() => {
    const list = isEmpty(iterationList)
      ? []
      : !disabledBacklog
      ? [{ title: i18n.t('dop:backlog'), id: -1 }, ...iterationList]
      : [...iterationList];
    return addAllOption ? [{ title: i18n.t('dop:all iterations'), id: 'ALL' }, ...list] : list;
  }, [addAllOption, iterationList, disabledBacklog]);

  React.useEffect(() => {
    iterationStore.reducers.updateIterationList(iterationList);
  }, [iterationList]);
  return (
    <Select
      {...rest}
      disabled={disabled}
      className={className}
      style={{ width: fullWidth ? '100%' : width }}
      value={
        value && !isEmpty(iterationList)
          ? mode === 'multiple'
            ? value.map((item) => String(item))
            : String(value)
          : undefined
      }
      onChange={onChange}
      placeholder={placeholder}
      allowClear={allowClear}
      mode={mode}
    >
      {map(iterationOption, ({ id, title }) => (
        <Option key={id} value={String(id)}>
          {title}
        </Option>
      ))}
    </Select>
  );
};
