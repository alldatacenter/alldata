import { Button, Card, Form, Input, message, Upload } from 'antd';
import { Avatar } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { fetchCheckName } from 'app/utils/fetch';
import debounce from 'debounce-promise';
import {
  BASE_API_URL,
  BASE_RESOURCE_URL,
  DEFAULT_DEBOUNCE_WAIT,
} from 'globalConstants';
import { useCallback, useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import {
  BORDER_RADIUS,
  SPACE_LG,
  SPACE_MD,
  SPACE_UNIT,
} from 'styles/StyleConstants';
import { APIResponse } from 'types';
import { getToken } from 'utils/auth';
import { useMainSlice } from '../../slice';
import {
  selectCurrentOrganization,
  selectSaveOrganizationLoading,
} from '../../slice/selectors';
import { editOrganization } from '../../slice/thunks';
import { DeleteConfirm } from './DeleteConfirm';

export function OrgSettingPage() {
  const [deleteConfirmVisible, setDeleteConfirmVisible] = useState(false);
  const { actions } = useMainSlice();
  const [avatarLoading, setAvatarLoading] = useState(false);
  const dispatch = useDispatch();
  const currentOrganization = useSelector(selectCurrentOrganization);
  const saveOrganizationLoading = useSelector(selectSaveOrganizationLoading);
  const [form] = Form.useForm();
  const t = useI18NPrefix('orgSetting');
  const tg = useI18NPrefix('global');

  useEffect(() => {
    if (currentOrganization) {
      form.setFieldsValue(currentOrganization);
    }
  }, [form, currentOrganization]);

  const avatarChange = useCallback(
    ({ file }) => {
      if (file.status === 'done') {
        const response = file.response as APIResponse<string>;
        if (response.success) {
          dispatch(actions.setCurrentOrganizationAvatar(response.data));
        }
        setAvatarLoading(false);
      } else {
        setAvatarLoading(true);
      }
    },
    [dispatch, actions],
  );

  const save = useCallback(
    values => {
      dispatch(
        editOrganization({
          organization: { id: currentOrganization?.id, ...values },
          resolve: () => {
            message.success(tg('operation.updateSuccess'));
          },
        }),
      );
    },
    [dispatch, currentOrganization, tg],
  );

  const showDeleteConfirm = useCallback(() => {
    setDeleteConfirmVisible(true);
  }, []);

  const hideDeleteConfirm = useCallback(() => {
    setDeleteConfirmVisible(false);
  }, []);

  return (
    <Wrapper>
      <Card title={t('info')}>
        <Form
          name="org_form_"
          form={form}
          labelAlign="left"
          labelCol={{ offset: 1, span: 7 }}
          wrapperCol={{ span: 16 }}
          onFinish={save}
        >
          <Form.Item label={t('avatar')} className="avatar">
            <Avatar
              size={SPACE_UNIT * 20}
              src={`${BASE_RESOURCE_URL}${currentOrganization?.avatar}`}
            >
              {currentOrganization?.name.substr(0, 1).toUpperCase()}
            </Avatar>
          </Form.Item>
          <Form.Item label=" " colon={false}>
            <Upload
              accept=".jpg,.jpeg,.png,.gif"
              method="post"
              action={`${BASE_API_URL}/files/org/avatar?orgId=${currentOrganization?.id}`}
              headers={{ authorization: getToken()! }}
              className="uploader"
              showUploadList={false}
              onChange={avatarChange}
            >
              <Button type="link" loading={avatarLoading}>
                {t('clickToUpload')}
              </Button>
            </Upload>
          </Form.Item>
          <Form.Item
            name="name"
            label={t('name')}
            getValueFromEvent={event => event.target.value?.trim()}
            rules={[
              {
                required: true,
                message: `${t('name')}${tg('validation.required')}`,
              },
              {
                validator: debounce((_, value) => {
                  if (value === currentOrganization?.name) {
                    return Promise.resolve();
                  }
                  const data = { name: value };
                  return fetchCheckName('orgs', data);
                }, DEFAULT_DEBOUNCE_WAIT),
              },
            ]}
          >
            <Input />
          </Form.Item>
          <Form.Item name="description" label={t('description')}>
            <Input.TextArea autoSize={{ minRows: 4, maxRows: 8 }} />
          </Form.Item>
          <Form.Item label=" " colon={false}>
            <Button
              type="primary"
              htmlType="submit"
              loading={saveOrganizationLoading}
            >
              {tg('button.save')}
            </Button>
          </Form.Item>
        </Form>
      </Card>
      <Card title={t('deleteOrg')}>
        <h4 className="notice">{t('deleteOrgDesc')}</h4>
        <Button danger onClick={showDeleteConfirm}>
          {t('deleteOrg')}
        </Button>
        <DeleteConfirm
          width={480}
          title={t('deleteOrg')}
          visible={deleteConfirmVisible}
          onCancel={hideDeleteConfirm}
        />
      </Card>
    </Wrapper>
  );
}

const Wrapper = styled.div`
  flex: 1;
  padding: ${SPACE_LG};
  overflow-y: auto;

  .ant-card {
    margin-top: ${SPACE_LG};
    background-color: ${p => p.theme.componentBackground};
    border-radius: ${BORDER_RADIUS};
    box-shadow: ${p => p.theme.shadow1};

    &:first-of-type {
      margin-top: 0;
    }
  }

  form {
    max-width: 480px;
    padding-top: ${SPACE_MD};
  }

  .avatar {
    align-items: center;
    margin-bottom: 0;
  }

  .notice {
    margin-bottom: ${SPACE_LG};
  }
`;
