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
import { message, Input, Popover, Button } from 'antd';
import { ErdaIcon } from 'common';
import i18n from 'i18n';
import { useEffectOnce } from 'react-use';

interface IProps {
  contentOnly?: boolean;
  defaultName?: string;
  createButton?: React.ReactNode;
  onSubmit: (params: { name: string }) => Promise<void>;
  onHide?: () => void;
}

export const EditCategory = (props: IProps) => {
  const [visible, setVisible] = React.useState(false);
  const [value, setValue] = React.useState(props.defaultName);

  const { onSubmit, createButton, contentOnly, onHide } = props;
  const onHideRef = React.useRef(onHide);

  const handleHide = () => {
    setVisible(false);
    setValue('');
  };

  const onClickNoneSelf = React.useCallback((e: any) => {
    if ('path' in e) {
      if (e.path.every((item: any) => item.id !== 'dice-edit-category')) {
        const hideFunc = onHideRef.current || handleHide;
        hideFunc();
      }
    }
  }, []);

  useEffectOnce(() => {
    if (contentOnly) {
      document.body.addEventListener('click', onClickNoneSelf);
    }
    return () => {
      contentOnly && document.body.removeEventListener('click', onClickNoneSelf);
    };
  });

  const handleSave = async () => {
    if (value) {
      if (value.length > 50) {
        message.warning(i18n.t('common:name length is too long'));
        return;
      }
      if (value.includes('/') || value.includes('\\')) {
        message.error(i18n.t('dop:The name cannot contain forward and backward slashes. Please enter again.'));
        return;
      }
      await onSubmit({ name: value });
      handleHide();
    } else {
      message.warning(i18n.t('dop:name is required'));
    }
  };

  const content = (
    <div
      id="dice-edit-category"
      className="flex justify-between items-center mr-2 overflow-auto"
      onClick={(e) => e.stopPropagation()}
    >
      <Input
        autoFocus
        style={{ minWidth: '150px' }}
        value={value}
        maxLength={50}
        onPressEnter={() => handleSave()}
        onChange={(e) => setValue(e.target.value)}
      />
      <ErdaIcon type="check" className="ml-3 cursor-pointer" onClick={handleSave} size="16" />
      <ErdaIcon
        fill="black-800"
        type="close"
        className="ml-3 cursor-pointer"
        onClick={props.onHide || handleHide}
        size="16"
      />
    </div>
  );

  if (props.contentOnly) {
    return content;
  }

  return (
    <Popover
      key={String(visible)} // 每次重新渲染，让input自动获焦
      visible={visible}
      content={content}
      trigger="click"
      placement="bottomRight"
      align={{ offset: [10, 0] }}
      onVisibleChange={(v) => (v ? setVisible(v) : handleHide())}
      footer={false}
    >
      {createButton || (
        <Button type="primary" className="flex items-center" onClick={() => value && onSubmit({ name: value })}>
          <ErdaIcon type="cir-add" className="mr-4" size="16" fill="white" />
          {i18n.t('add')}
        </Button>
      )}
    </Popover>
  );
};
