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

import { Table, Skeleton, Spin, Button, Popover, Input, Select, Modal, message, Tooltip, Form, Alert } from 'antd';
import { goTo, cutStr, fromNow, replaceEmoji, setApiWithOrg } from 'common/utils';
import { groupBy, sortBy, get } from 'lodash';
import React from 'react';
import { useUnmount, useUpdateEffect } from 'react-use';
import { Icon as CustomIcon, Copy, EmptyHolder, IF, FormModal, ErdaIcon } from 'common';
import RepoFileContainer from './components/repo-file-container';
import RepoEditor from './components/repo-editor';
import StartTip from './components/start-tip';
import { CommitBlock } from './common';
import { renderAsLink, getInfoFromRefName, getSplitPathBy } from './util';
import { Link } from 'react-router-dom';
import { RepoNav } from './components/repo-nav';
import routeInfoStore from 'core/stores/route';
import repoStore from 'application/stores/repo';
import { useLoading } from 'core/stores/loading';
import { WithAuth, usePerm } from 'user/common';
import appStore from 'application/stores/application';
import i18n from 'i18n';
import { FormInstance } from 'core/common/interface';

import './repo-tree.scss';
import { repositoriesTypes } from 'application/common/config';

const { Group: ButtonGroup } = Button;
const { Option } = Select;
const FormItem = Form.Item;

interface IDownProp {
  info: REPOSITORY.IInfo;
  appDetail: any;
}

const RepoDownload = (props: IDownProp) => {
  const {
    info,
    appDetail: { projectName, name, gitRepoAbbrev, token, gitRepoNew },
  } = props;
  const { protocol, host } = window.location;
  const gitRepo = `${protocol}//${gitRepoNew}`;
  const { branch, tag } = getInfoFromRefName(info.refName);
  const currentBranch = branch || tag || info.defaultBranch;
  const download = (format: string) =>
    window.open(setApiWithOrg(`/api/repo/${gitRepoAbbrev}/archive/${currentBranch}.${format}`));
  const renderAddonAfter = (text: string, tip: string) => {
    return (
      <span className="copy-btn cursor-copy" data-clipboard-text={text} data-clipboard-tip={tip}>
        <ErdaIcon size="14" className="mt-2" type="copy" />
      </span>
    );
  };

  return (
    <Popover
      overlayClassName="repo-clone-popover"
      placement="bottomRight"
      title={<h3 className="clone-repo-name">{name}</h3>}
      content={
        <div className="clone-content">
          <div className="addr">
            <Input
              className="w-full"
              value={gitRepo}
              addonAfter={renderAddonAfter(gitRepo, i18n.t('dop:repo address'))}
            />
          </div>
          <Copy selector=".cursor-copy" />
          <ButtonGroup className="download-btn-group mb-4">
            <Button size="small" onClick={() => download('tar')}>
              {' '}
              tar{' '}
            </Button>
            <Button size="small" onClick={() => download('tar.gz')}>
              {' '}
              tar.gz{' '}
            </Button>
            <Button size="small" onClick={() => download('zip')}>
              {' '}
              zip{' '}
            </Button>
          </ButtonGroup>
          {token && (
            <>
              <p className="label mb-2">username</p>
              <Input
                className="w-full mb-4"
                value={info.username}
                addonAfter={renderAddonAfter(info.username, 'username')}
              />
              <p className="label mb-2">token</p>
              <Input className="w-full mb-4" value={token} addonAfter={renderAddonAfter(token, 'token')} />
            </>
          )}
        </div>
      }
      trigger="click"
    >
      <div className="repo-clone-btn">
        <Button type="primary" ghost>
          {i18n.t('dop:repo address')}
        </Button>
      </div>
    </Popover>
  );
};

interface ITreeProps {
  tree: REPOSITORY.ITree;
  info: REPOSITORY.IInfo;
  isFetchingInfo: boolean;
  isFetchingTree: boolean;
}

const RepoTree = ({ tree, info, isFetchingInfo, isFetchingTree }: ITreeProps) => {
  const { branch, commitId, tag } = getInfoFromRefName(tree.refName);
  if (!tree.type) {
    if (isFetchingInfo || isFetchingTree) {
      return (
        <Spin spinning tip="Loading...">
          <div style={{ height: '400px' }} />
        </Spin>
      );
    }
    const { before, after } = getSplitPathBy('tree');

    return (
      <div className="repo-tree-holder relative">
        <EmptyHolder
          tip={`${i18n.t('dop:current branch or path')}：${after} ${i18n.t('does not exist')}。`}
          action={<Link to={before.slice(0, -'/tree'.length)}>{i18n.t('back to repo home')}</Link>}
        />
      </div>
    );
  }

  const inIndexPage = window.location.pathname.match(/apps\/\d+\/repo$/);
  const group = groupBy(tree.entries, 'type');
  const dataSource = sortBy(group.tree, 'name').concat(sortBy(group.blob, 'name'));
  const isTopDir = !tree.path;
  if (!isTopDir) {
    dataSource.splice(0, 0, {
      name: '..',
      commit: {},
    } as any);
  }
  const curBranch = branch || tag || info.defaultBranch;

  return (
    <Spin spinning={isFetchingTree || false} wrapperClassName={tree.type === 'tree' ? 'flex-1' : ''}>
      <CommitBlock commit={tree.commit} />
      {tree.type === 'tree' ? (
        <React.Fragment>
          <Table
            size="small"
            className="repo-tree"
            dataSource={dataSource}
            pagination={false}
            rowKey="name"
            scroll={{ x: 800 }}
            onRow={({ name, id }) => {
              const tropicalPathName = encodeURI(name);
              return {
                onClick: () => {
                  if (inIndexPage) {
                    goTo(`./tree/${curBranch}/${tropicalPathName}`, { forbidRepeat: true });
                  } else if (name === '..' && !id) {
                    goTo('../', { forbidRepeat: true });
                  } else {
                    goTo(`./${tropicalPathName}`, { forbidRepeat: true });
                  }
                },
              };
            }}
            columns={[
              {
                title: 'Name',
                dataIndex: 'name',
                width: 220,
                render: (text, record) => {
                  const iconProps = record.type === 'tree' ? { type: 'folder' } : { type: 'page' };
                  return (
                    <span className="column-name" title={text}>
                      {record.type ? <CustomIcon className="mr-2" {...iconProps} /> : null}
                      {text}
                    </span>
                  );
                },
              },
              {
                title: 'Last Commit',
                dataIndex: ['commit', 'commitMessage'],
                render: (text, record) => (
                  <Skeleton active loading={!record.commit} paragraph={false}>
                    {renderAsLink(
                      'commit',
                      record.commit && record.commit.id,
                      cutStr(replaceEmoji(text), 100),
                      'repo-link',
                    )}
                  </Skeleton>
                ),
              },
              {
                title: 'Last Update',
                dataIndex: ['commit', 'author', 'when'],
                width: 220,
                render: (text) => (text ? fromNow(text) : ''),
              },
            ]}
          />
          <IF check={tree.readmeFile}>
            <RepoFileContainer
              noEdit
              name={tree.readmeFile}
              path={`/${commitId || curBranch}${tree.path ? `/${tree.path}` : ''}/${tree.readmeFile}`}
            />
          </IF>
        </React.Fragment>
      ) : (
        <RepoFileContainer maxLines={50} name={tree.entry.name} path={`/${commitId || curBranch}/${tree.path || ''}`} />
      )}
    </Spin>
  );
};

const RefComp = ({
  form,
  info,
  defaultValue,
  value,
  change,
}: {
  defaultValue: Record<string, string>;
  form: FormInstance;
  info: { branches: string[]; tags: string[] };
}) => {
  const refType = form.getFieldValue('refType');
  const refValue = form.getFieldValue('refValue') || defaultValue[refType];
  const curForm = React.useRef(form);
  useUpdateEffect(() => {
    curForm.current.setFieldsValue({ refValue: defaultValue[refType] });
  }, [refType]);

  const { branches, tags } = info;
  const options = refType === 'commitId' ? null : refType === 'branch' ? branches : tags;

  const handleTextChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    form.setFieldsValue({ refValue: e.target.value });
  };

  const handleSelectChange = (e: React.ChangeEvent) => {
    form.setFieldsValue({ refValue: e.toString() });
  };

  return (
    <div>
      <IF check={options}>
        <Select
          showSearch
          value={refValue}
          optionFilterProp="children"
          onChange={handleSelectChange}
          filterOption={(input, option: any) => option.props.children.toLowerCase().indexOf(input.toLowerCase()) >= 0}
        >
          {options &&
            options.map((option: string) => (
              <Option key={option} value={option}>
                {option}
              </Option>
            ))}
        </Select>
        <IF.ELSE />
        <Input type="text" value={refValue} onChange={handleTextChange} />
      </IF>
    </div>
  );
};

const RepoTreePage = () => {
  const [tipVisible, toggleTip] = React.useState(false);
  const [visible, setVisible] = React.useState(false);
  const params = routeInfoStore.useStore((s) => s.params);
  const branchPerm = usePerm((s) => s.app.repo.branch);
  const appDetail = appStore.useStore((s) => s.detail);
  const [info, tree, mode] = repoStore.useStore((s) => [s.info, s.tree, s.mode]);
  const hasAuth = usePerm((s) => s.app.externalRepo.edit.pass);
  const [refType, setRefType] = React.useState();

  const branchCreateAuth = branchPerm.writeProtected.pass;

  const { clearRepoTree } = repoStore.reducers;
  const { checkCommitId, createBranch } = repoStore.effects;
  const [showEdit, setShowEdit] = React.useState(false);
  const repoNavRef = React.useRef<{ target: { type: string; current: string } }>(null);
  useUnmount(() => {
    clearRepoTree();
  });

  React.useEffect(() => {
    if (get(appDetail, 'mode') === 'ABILITY') {
      goTo(goTo.pages.deploy, { replace: true, ...params });
    }
  }, [appDetail, params]);

  const [isFetchingInfo, isFetchingTree] = useLoading(repoStore, ['getRepoInfo', 'getRepoTree']);
  const getFieldsList = () => {
    const { type, current } = repoNavRef.current?.target || {};
    const fieldsList = [
      {
        label: i18n.t('dop:source type'),
        name: 'refType',
        type: 'radioGroup',
        initialValue: type,
        itemProps: {
          onChange: (value) => {
            setRefType(value);
          },
        },
        options: [
          { name: 'Branch', value: 'branch' },
          { name: 'Tag', value: 'tag' },
          { name: 'commit SHA', value: 'commitId' },
        ],
      },
      {
        label: i18n.t('dop:based on source'),
        name: 'refValue',
        type: 'custom',
        initialValue: current,
        getComp: ({ form }: any) => <RefComp info={info} form={form} defaultValue={{ [type]: current }} />,
      },
      {
        label: i18n.t('dop:branch'),
        name: 'branch',
        itemProps: {
          autoFocus: true,
        },
        rules: [
          {
            validator: (_rule: any, value: string, callback: Function) => {
              if (value.includes('%')) {
                callback(i18n.t('dop:cannot contain %'));
              } else {
                callback();
              }
            },
          },
        ],
      },
    ];
    return fieldsList;
  };

  const beforeSubmit = async (values: { refValue: string; refType: string }) => {
    if (values.refType === 'commitId') {
      const ret = await checkCommitId({ commitId: values.refValue });
      if (ret === 'error') {
        message.error(i18n.t('dop:invalid commit SHA'));
        return null;
      }
    }
    return values;
  };

  const onCreateBranch = (branchInfo: { refValue: string; branch: string }) => {
    createBranch(branchInfo).then((error: any) => {
      if (error) {
        message.error(i18n.t('dop:failed to create branch'));
        return;
      }
      const { before } = getSplitPathBy('tree');
      goTo(`${before}/${branchInfo.branch}`, { append: false });
      setVisible(false);
    });
  };

  const updateApplication = (data: { repoConfig: APPLICATION.GitRepoConfig }) => {
    const { repoConfig } = appDetail;
    const payload = {
      repoConfig: {
        ...repoConfig,
        ...data.repoConfig,
      },
    } as APPLICATION.createBody;
    appStore.effects.updateAppDetail(payload);
    setShowEdit(false);
  };
  if (appDetail.isExternalRepo) {
    const repoConfigFieldsList = [
      {
        label: i18n.t('dop:repository address'),
        name: ['repoConfig', 'url'],
        itemProps: {
          disabled: true,
          placeholder: i18n.t('default:please enter'),
        },
      },
      {
        label: i18n.t('default:user name'),
        name: ['repoConfig', 'username'],
        itemProps: {
          placeholder: i18n.t('default:please enter'),
        },
      },
      {
        label: i18n.t('default:password'),
        name: ['repoConfig', 'password'],
        type: 'custom',
        getComp: () => <Input.Password />,
        itemProps: {
          placeholder: i18n.t('default:please enter'),
        },
      },
      {
        label: i18n.t('dop:repository description'),
        type: 'textArea',
        name: ['repoConfig', 'desc'],
        required: false,
        itemProps: { rows: 4, maxLength: 50, style: { resize: 'none' } },
      },
    ];
    const { type: repoType, ...rest } = appDetail.repoConfig as APPLICATION.GitRepoConfig;
    const repoTypeConfig = get(repositoriesTypes, repoType, {});
    return (
      <div className="git-repo-config">
        <div className="top-button-group">
          <WithAuth pass={hasAuth} tipProps={{ placement: 'bottom' }}>
            <Button
              type="primary"
              disabled={info.isLocked}
              onClick={() => {
                setShowEdit(true);
              }}
            >
              {i18n.t('default:edit')}
            </Button>
          </WithAuth>
        </div>
        <Form layout="vertical">
          <FormItem label={i18n.t('dop:repository source')}>
            <p>{repoTypeConfig.name}</p>
            <img className="logo" src={repoTypeConfig.logo} width="46px" />
          </FormItem>
          <FormItem label={i18n.t('dop:repository address')}>
            <a href={rest.url} target="_blank" rel="noopener noreferrer">
              {rest.url}
            </a>
          </FormItem>
          {rest.desc ? (
            <FormItem label={i18n.t('dop:repository description')}>
              <p>{rest.desc}</p>
            </FormItem>
          ) : null}
        </Form>
        <FormModal
          visible={showEdit}
          formData={{ repoConfig: rest }}
          name={i18n.t('dop:repository information')}
          fieldsList={repoConfigFieldsList}
          onOk={updateApplication}
          onCancel={() => setShowEdit(false)}
        />
      </div>
    );
  }
  if (info.empty && !mode.addFile) {
    return (
      <div>
        <div className="top-button-group">
          <RepoDownload info={info} appDetail={appDetail} />
        </div>
        {info.empty && <StartTip showCreateFile={branchCreateAuth} />}
      </div>
    );
  }
  const editMode = mode.addFile || mode.editFile;
  const fullPage = editMode && tree.type !== 'tree';
  return (
    <div className={`repo-tree-page ${fullPage ? 'full-page' : ''}`}>
      <IF check={info.isLocked}>
        <div className="repo-locked-alert">
          <Alert message={i18n.t('lock-repository-tip')} type="error" />
        </div>
      </IF>
      <RepoNav ref={repoNavRef} info={info} tree={tree} isFetchingInfo={isFetchingInfo} appId={appDetail.id} />
      <div className="top-button-group">
        <Tooltip title={i18n.t('dop:how to start')}>
          <CustomIcon className="text-desc hover-active" type="help" onClick={() => toggleTip(true)} />
        </Tooltip>
        <Modal
          title={i18n.t('dop:how to start')}
          visible={tipVisible}
          footer={null}
          onCancel={() => toggleTip(false)}
          style={{ minWidth: '80%' }}
        >
          <StartTip />
        </Modal>
        {/* <Button onClick={() => goTo(goTo.pages.repoBackup, { ...params })}>{i18n.t('dop:repo backup')}</Button> */}
        <RepoDownload info={info} appDetail={appDetail} />
        <WithAuth pass={branchPerm.writeNormal.pass} tipProps={{ placement: 'bottom' }}>
          <Button
            type="primary"
            disabled={info.isLocked}
            onClick={() => {
              setVisible(true);
            }}
          >
            {i18n.t('dop:new branch')}
          </Button>
        </WithAuth>
      </div>
      {mode.addFile ? (
        <RepoEditor />
      ) : (
        <RepoTree info={info} tree={tree} isFetchingInfo={isFetchingInfo} isFetchingTree={isFetchingTree} />
      )}
      <FormModal
        visible={visible}
        name={i18n.t('dop:branch')}
        fieldsList={getFieldsList()}
        onOk={onCreateBranch}
        onCancel={() => setVisible(false)}
        beforeSubmit={beforeSubmit}
        modalProps={{
          destroyOnClose: true,
        }}
      />
    </div>
  );
};
export default RepoTreePage;
