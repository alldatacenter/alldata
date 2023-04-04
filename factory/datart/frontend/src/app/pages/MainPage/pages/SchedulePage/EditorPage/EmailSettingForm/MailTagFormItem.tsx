import { SearchOutlined, UserOutlined } from '@ant-design/icons';
import { AutoComplete, Avatar, Input, Space, Tag } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { DEFAULT_DEBOUNCE_WAIT } from 'globalConstants';
import debounce from 'lodash/debounce';
import { FC, useCallback, useEffect, useMemo, useState } from 'react';
import styled from 'styled-components';
import { SPACE_XS } from 'styles/StyleConstants';
import { EMAIL_REG } from '../../constants';
import { searchUserEmails } from '../../services';
import { IUserInfo } from '../../types';

interface MailTagFormItemProps {
  onFocus?: () => void;
  onBlur?: () => void;
  value?: string;
  onChange?: (v: string) => void;
}
export const MailTagFormItem: FC<MailTagFormItemProps> = ({
  value,
  onChange,
}) => {
  const [dataSource, setDataSource] = useState<IUserInfo[]>([]);
  const [keyword, setKeyword] = useState('');
  const t = useI18NPrefix('schedule.editor.emailSettingForm.mailTagFormItem');

  const emails = useMemo(() => {
    return value ? value.split(';').filter(Boolean) : [];
  }, [value]);

  const onSearch = useCallback(async keyword => {
    if (keyword) {
      const res = await searchUserEmails(keyword);
      setDataSource(res);
    } else {
      setDataSource([]);
    }
  }, []);
  const onDebouncedSearch = useMemo(
    () => debounce(onSearch, DEFAULT_DEBOUNCE_WAIT),
    [onSearch],
  );

  const onSelectOrRemoveEmail = useCallback(
    (email: string) => {
      const _emails = [...emails];
      const index = _emails.indexOf(email);
      if (index > -1) {
        _emails.splice(index, 1);
      } else {
        _emails.push(email);
      }
      onChange?.(_emails.join(';'));
    },
    [onChange, emails],
  );

  useEffect(() => {
    setKeyword('');
  }, [value]);

  const options = useMemo(() => {
    const items = dataSource.filter(v => !emails.includes(v?.email));
    return items.map(({ username, email }) => ({
      value: email,
      label: (
        <Space>
          <Avatar src={''} size="small" icon={<UserOutlined />} />
          <span>{username}</span>
          <span>{email}</span>
        </Space>
      ),
    }));
  }, [dataSource, emails]);

  const appendOptions = useMemo(() => {
    const newEmail = keyword as string;
    if (
      !EMAIL_REG.test(newEmail) ||
      ~dataSource.findIndex(({ email }) => email === newEmail) < 0
    ) {
      return [];
    }
    return [
      {
        value: newEmail,
        label: (
          <Space>
            <Avatar size="small" icon={<UserOutlined />} />
            <span>{newEmail.split('@')[0]}</span>
            <span>{newEmail}</span>
          </Space>
        ),
      },
    ];
  }, [keyword, dataSource]);
  const autoCompleteOptions = useMemo(
    () => options.concat(appendOptions),
    [appendOptions, options],
  );

  return (
    <>
      {emails.map(email => (
        <EmailTag
          closable
          key={email}
          color="blue"
          onClose={() => onSelectOrRemoveEmail(email)}
        >
          {email}
        </EmailTag>
      ))}
      <AutoComplete
        value={keyword}
        onChange={setKeyword}
        options={autoCompleteOptions}
        onSearch={onDebouncedSearch}
        onSelect={onSelectOrRemoveEmail}
        onBlur={() => onSearch('')}
      >
        <Input suffix={<SearchOutlined />} placeholder={t('placeholder')} />
      </AutoComplete>
    </>
  );
};

const EmailTag = styled(Tag)`
  margin-bottom: ${SPACE_XS};
`;
