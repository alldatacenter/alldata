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
import { Ellipsis, ErdaIcon, Icon as CustomIcon } from 'common';
import i18n from 'i18n';
import './index.scss';

interface Label {
  label: string;
  group?: string;
  color?: string;
  checked?: boolean;
}
export interface IProps extends Omit<IItemProps, 'label'> {
  labels: Label[] | Label;
  maxShowCount?: number;
  containerClassName?: string;
}

interface IItemProps {
  label: Label;
  maxWidth?: number;
  colorMap?: Obj<string>;
  size?: 'small' | 'default';
  checked?: boolean;
  readOnly?: boolean;
  deleteConfirm?: boolean;
  onDelete?: (p: Label) => void;
}

export const TagItem = (props: IItemProps) => {
  const {
    label: _label,
    size = 'default',
    maxWidth,
    onDelete,
    deleteConfirm = true,
    colorMap,
    checked,
    readOnly,
  } = props;
  const { label, color = 'blue' } = _label;
  const [isChecked, setIsChecked] = React.useState(checked);
  // TODO: compatible with gray which color is removed now
  const curColor = color === 'gray' ? 'blue' : color;
  const style = {
    maxWidth,
  };

  React.useEffect(() => {
    if (readOnly) {
      setIsChecked(checked);
    }
  }, [checked, readOnly]);

  const cls = isChecked
    ? `text-${curColor}-light bg-${curColor}-deep border-0 border-solid border-l-2 border-${curColor}-mid`
    : `text-${curColor}-deep bg-${curColor}-light border-0 border-solid border-l-2 border-${curColor}-mid`;

  return (
    <span style={style} className={`tag-default twt-tag-item ${size} ${cls}`}>
      <div className="flex items-center">
        <Ellipsis
          title={label}
          zIndex={2010} // popconfirm zIndex is bigger than tooltip
        />
        {onDelete ? (
          deleteConfirm ? (
            <Popconfirm
              title={`${i18n.t('common:confirm deletion')}?`}
              arrowPointAtCenter
              zIndex={2000} //  popconfirm default zIndex=1030, is smaller than tooltip zIndex=1070
              onConfirm={(e) => {
                e?.stopPropagation();
                onDelete(_label);
              }}
              onCancel={(e) => e && e.stopPropagation()}
            >
              <ErdaIcon size="16" className="cursor-pointer text-default-2 ml-0.5" type="close" />
            </Popconfirm>
          ) : (
            <ErdaIcon
              size="16"
              className="cursor-pointer text-default-2 ml-0.5"
              type="close"
              onClick={() => onDelete(_label)}
            />
          )
        ) : null}
        {isChecked && !readOnly && (
          <ErdaIcon
            size="16"
            className={`cursor-pointer text-default-2 ml-0.5 text-${color}-light`}
            type="check"
            onClick={() => setIsChecked(!isChecked)}
          />
        )}
        {checked && readOnly && <CustomIcon className={`text-default-2 ml-0.5 text-${color}-light`} type="tg" />}
      </div>
    </span>
  );
};

const Tags = ({
  labels: propsLabels,
  maxShowCount = 2,
  containerClassName = '',
  size = 'small',
  colorMap,
  onDelete,
}: IProps) => {
  const labels = propsLabels ? (Array.isArray(propsLabels) ? propsLabels : [propsLabels]) : [];
  const showMore = labels.length > maxShowCount;

  const restTags = () => {
    return labels
      .slice(maxShowCount)
      .map((l) => <TagItem colorMap={colorMap} key={l.label} label={l} onDelete={onDelete} size={size} />);
  };

  const oneAndMoreTag = (
    <React.Fragment>
      {labels.slice(0, maxShowCount).map((l) => (
        <TagItem
          colorMap={colorMap}
          key={l.label}
          label={l}
          maxWidth={100}
          onDelete={onDelete}
          size={size}
          checked={l.checked}
        />
      ))}
      {showMore ? (
        <Tooltip
          title={
            <div onClick={(e) => e.stopPropagation()} className="tags-container ">
              {restTags()}
            </div>
          }
          placement="right"
          overlayClassName="tags-row-tooltip"
        >
          <ErdaIcon className={`twt-tag-ellipsis ${size}`} type="more" />
        </Tooltip>
      ) : (
        labels
          .slice(maxShowCount)
          .map((l) => (
            <TagItem colorMap={colorMap} key={l.label} label={l} maxWidth={160} onDelete={onDelete} size={size} />
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
    </div>
  );
};

export default Tags;
