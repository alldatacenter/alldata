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
import { Tooltip, Popconfirm } from 'antd';
import { Ellipsis, ErdaIcon } from 'common';
import { some, has, groupBy, map, max } from 'lodash';
import { colorToRgb } from 'common/utils';
import i18n from 'i18n';
import './index.scss';

interface ILabel {
  label: string;
  group?: string;
  color?: string;
}
export interface IProps extends Omit<IItemProps, 'label'> {
  labels: ILabel[] | ILabel;
  showCount?: number;
  containerClassName?: string;
  onAdd?: () => void;
}

interface IItemProps {
  colorMap?: Obj;
  label: ILabel;
  maxWidth?: number;
  size?: 'small' | 'default';
  deleteConfirm?: boolean;
  onDelete?: (p: ILabel) => void;
}

export const TagColorMap = {
  green: '#34b37e',
  red: '#df3409',
  orange: '#f47201',
  purple: '#6a549e',
  blue: '#0567ff',
  cyan: '#5bd6d0',
  gray: '#666666',
};

export const TagItem = (props: IItemProps) => {
  const { label: _label, size, maxWidth, onDelete, deleteConfirm = true, colorMap } = props;
  const { label, color } = _label;
  const curColor = (colorMap || TagColorMap)[color || 'gray'] || color || TagColorMap.gray;
  const style = {
    maxWidth,
    color: curColor,
    backgroundColor: colorToRgb(curColor, 0.1),
  };
  return (
    <span style={style} className={`tag-default twt-tag-item ${size}`}>
      {onDelete ? (
        deleteConfirm ? (
          <Popconfirm
            title={`${i18n.t('common:confirm deletion')}?`}
            arrowPointAtCenter
            zIndex={2000} //  popconfirm default zIndex=1030, is smaller than tooltip zIndex=1070
            onConfirm={(e) => {
              e && e.stopPropagation();
              onDelete(_label);
            }}
            onCancel={(e) => e && e.stopPropagation()}
          >
            <ErdaIcon
              type="close-one"
              size="12"
              className="tag-close cursor-pointer text-holder"
            />
          </Popconfirm>
        ) : (
          <ErdaIcon
            type="close-one"
            size="12"
            className="tag-close cursor-pointer text-holder"
            onClick={() => onDelete(_label)}
          />
        )
      ) : null}

      <Ellipsis
        title={label}
        zIndex={2010} // popconfirm zIndex is bigger than tooltip
      />
    </span>
  );
};

const MAX_LABEL_WIDTH = 180;

const TagsRow = ({
  labels: propsLabels,
  showCount = 2,
  containerClassName = '',
  size = 'small',
  colorMap,
  onDelete,
  onAdd,
}: IProps) => {
  const labels = propsLabels ? (Array.isArray(propsLabels) ? propsLabels : [propsLabels]) : [];
  const showMore = labels.length > showCount;
  const showGroup = some(labels, (l) => has(l, 'group'));
  
  const [labelWidth, setLabelWidth] = React.useState<string | number>('auto');

  const countLabelWidth = () => {
    const labelEles = document.querySelectorAll('.tag-group-name');
    const maxWidth: number = max(map(labelEles, (ele: HTMLSpanElement) => ele.offsetWidth));
    setLabelWidth(Math.min(maxWidth, MAX_LABEL_WIDTH) + 8);
  };

  const fullTags = () => {
    if (showGroup) {
      return (
        <div>
          {map(groupBy(labels, 'group'), (groupItem, gKey) => (
            <div key={gKey} className="tag-group-container mb-2">
              <span className="tag-group-name" style={{ width: labelWidth }}>{`${gKey} : `}</span>
              <span className="flex-1 overflow-auto">
                {groupItem.map((item) => (
                  <TagItem colorMap={colorMap} key={item.label} label={item} onDelete={onDelete} size={size} />
                ))}
              </span>
            </div>
          ))}
        </div>
      );
    }
    return labels.map((l) => <TagItem colorMap={colorMap} key={l.label} label={l} onDelete={onDelete} size={size} />);
  };

  const oneAndMoreTag = (
    <React.Fragment>
      {labels.slice(0, showCount).map((l) => (
        <TagItem colorMap={colorMap} key={l.label} label={l} maxWidth={100} onDelete={onDelete} size={size} />
      ))}
      {showMore ? (
        <Tooltip
          onVisibleChange={(vis) => {
            if (vis && labelWidth === 'auto') {
              countLabelWidth();
            }
          }}
          title={
            <div onClick={(e) => e.stopPropagation()} className="tags-container ">
              {fullTags()}
            </div>
          }
          placement="right"
          overlayClassName="tags-row-tooltip"
        >
          <ErdaIcon className={`twt-tag-ellipsis ${size}`} type="more" color="currentColor" />
        </Tooltip>
      ) : (
        labels
          .slice(showCount)
          .map((l) => (
            <TagItem colorMap={colorMap} key={l.label} label={l} maxWidth={120} onDelete={onDelete} size={size} />
          ))
      )}
    </React.Fragment>
  );

  return (
    <div
      className={`tags-container flex items-center justify-start ${containerClassName}`}
      onClick={(e) => e.stopPropagation()}
    >
      <span className="tags-box flex items-center">{oneAndMoreTag}</span>
      {onAdd ? (
        <ErdaIcon
          className={`tags-add ${size} ml-2 text-xs leading-6 cursor-pointer`}
          type="tj1"
          color="currentColor"
          onClick={onAdd}
        />
      ) : null}
    </div>
  );
};

export default TagsRow;
