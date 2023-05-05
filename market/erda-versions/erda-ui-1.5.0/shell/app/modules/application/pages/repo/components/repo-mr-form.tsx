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

import { Button, Menu, Dropdown, message, Tooltip } from 'antd';
import React from 'react';
import { RenderForm, FormModal, MemberSelector, ErdaIcon } from 'common';
import { connectCube } from 'common/utils';
import Markdown from 'common/utils/marked';
import MarkdownEditor from 'common/components/markdown-editor';
import SourceTargetSelect from './source-target-select';
import i18n from 'i18n';
import { connectUser } from 'app/user/common';
import { isEmpty } from 'lodash';
import repoStore from 'application/stores/repo';
import { FormInstance } from 'core/common/interface';
import './repo-mr-form.scss';
import routeInfoStore from 'core/stores/route';
import layoutStore from 'layout/stores/layout';

interface IModel {
  visible: boolean;
  tplContent: string;
  templateConfig: {
    path: string;
    names: string[];
  };
  onOk: (data: object) => void;
  onCancel: () => void;
}
const TplModel = ({ visible, tplContent, templateConfig, onOk, onCancel }: IModel) => {
  const { path } = templateConfig;

  const fieldsList = [
    {
      label: i18n.t('template name'),
      name: 'name',
      itemProps: {
        maxLength: 20,
        addonAfter: '.md',
      },
      rules: [
        {
          validator: (_rule: any, value: string, callback: Function) => {
            const duplicate = templateConfig.names.includes(`${value}.md`);
            if (duplicate) {
              callback(i18n.t('template already exists'));
            } else {
              callback();
            }
          },
        },
      ],
      extraProps: {
        extra: (
          <span>
            {i18n.t('file saving directory')}: <code className="mr-template-save-path">{path}</code>
          </span>
        ),
      },
    },
    {
      label: i18n.t('template preview'),
      name: 'content',
      required: false,
      initialValue: tplContent,
      getComp() {
        return (
          <article
            className="mr-template-preview"
            // eslint-disable-next-line react/no-danger
            dangerouslySetInnerHTML={{ __html: Markdown(tplContent || '') }}
          />
        );
      },
    },
  ];
  return (
    <FormModal
      width="620px"
      title={i18n.t('create template')}
      layout="vertical"
      fieldsList={fieldsList}
      visible={visible}
      onOk={onOk}
      onCancel={onCancel}
    />
  );
};

interface IBranchObj {
  sourceBranch: string;
  targetBranch: string;
  removeSourceBranch: boolean;
}

interface IProps {
  sideFold: boolean;
  info: REPOSITORY.IInfo;
  loginUser: ILoginUser;
  params: any;
  templateConfig: {
    names: [];
    branch: string;
    path: string;
  };
  mrStats: any;
  formData: any;
  editMode: boolean;
  operateMR: typeof repoStore.effects.operateMR;
  getRepoBlob: typeof repoStore.effects.getRepoBlob;
  getMRStats: typeof repoStore.effects.getMRStats;
  getCompareDetail: typeof repoStore.effects.getCompareDetail;
  createMR: typeof repoStore.effects.createMR;
  getRepoInfo: typeof repoStore.effects.getRepoInfo;
  clearMRStats: typeof repoStore.reducers.clearMRStats;
  commit: typeof repoStore.effects.commit;
  onBranchChange: (b: IBranchObj) => void;
  onShowComparison: () => Promise<any>;
  onCancel: () => void;
  onOk: (data: any) => void;
  getTemplateConfig: () => Promise<any>;
  moveToDiff: () => void;
}

interface IState {
  tplName: string;
  tplContent: string;
  tplModelVisible: boolean;
}

class RepoMRForm extends React.PureComponent<IProps, IState> {
  tplMap: object;

  form = React.createRef<FormInstance>();

  tplNameCache: string;

  constructor(props: IProps) {
    super(props);
    this.state = {
      tplName: '',
      tplContent: '',
      tplModelVisible: false,
    };
    this.tplMap = {};
  }

  componentDidMount() {
    if (!isEmpty(this.props.formData)) {
      const { sourceBranch, targetBranch, removeSourceBranch } = this.props.formData;
      this.getMRStats({ sourceBranch, targetBranch, removeSourceBranch });
    }
    this.props.getTemplateConfig().then((config) => {
      if (config.names.length) {
        this.selectTemplate(config.names[0]);
      }
    });
  }

  componentWillUnmount() {
    this.props.clearMRStats();
  }

  onBranchChange = (branch: IBranchObj, isBranchChange: boolean) => {
    if (isBranchChange) {
      this.props.onBranchChange && this.props.onBranchChange(branch);
      this.getMRStats(branch);
    }
  };

  getMRStats = (branch: IBranchObj) => {
    const { sourceBranch, targetBranch } = branch;
    if (sourceBranch) {
      this.props.getMRStats({
        sourceBranch: sourceBranch || this.props.info.defaultBranch,
        targetBranch,
      });
    }
  };

  onCancel = () => {
    this.form.current?.resetFields();
    this.props.clearMRStats();
    this.props.onCancel();
  };

  onCompare = ({ sourceBranch, targetBranch }: IBranchObj) => {
    this.props.getCompareDetail({ compareA: sourceBranch, compareB: targetBranch });
    this.props.onShowComparison && this.props.onShowComparison();
  };

  selectTemplate = (name: string) => {
    const { templateConfig, getRepoBlob } = this.props;
    const { branch, path } = templateConfig;
    if (this.tplMap[name]) {
      this.handleTplChange({
        tplName: name,
        tplContent: this.tplMap[name],
      });
      return;
    }
    getRepoBlob({ path: `/${branch}/${path}/${name}` }).then((data) => {
      this.setState({
        tplName: name,
        tplContent: data.content,
      });
      this.tplMap[name] = data.content;
      this.tplNameCache = name;
    });
  };

  handleTplChange = ({ tplContent, tplName }: any) => {
    this.form.current?.setFieldsValue({ description: tplContent });
    this.setState({ tplContent, tplName });
  };

  toggleTplModel = (visible: boolean) => {
    this.setState({ tplModelVisible: visible });
  };

  getTplSelect = () => {
    const { templateConfig } = this.props;
    const { tplName } = this.state;
    const tplNames = templateConfig.names.filter((file: string) => file.endsWith('.md'));

    const menu = (
      <Menu selectedKeys={[`tpl_${tplName}`]}>
        {tplNames.length ? (
          tplNames.map((name: string) => {
            return (
              <Menu.Item key={`tpl_${name}`} onClick={() => this.selectTemplate(name)}>
                {name.replace('.md', '')}
              </Menu.Item>
            );
          })
        ) : (
          <Menu.Item key="empty" disabled>
            {i18n.t('no template')}
          </Menu.Item>
        )}
        <Menu.Divider />
        <Menu.Item key="clear" onClick={() => this.handleTplChange({ tplContent: '', tplName: '' })}>
          {i18n.t('clear content')}
        </Menu.Item>
        {/* <Menu.Item key="save" disabled={!this.state.tplContent} onClick={() => this.toggleTplModel(true)}>
          保存为模板
        </Menu.Item> */}
        <Menu.Item
          key="reset"
          disabled={!tplName || !tplNames.length}
          onClick={() =>
            this.handleTplChange({
              tplName: tplName || this.tplNameCache,
              tplContent: this.tplMap[tplName || this.tplNameCache],
            })
          }
        >
          {i18n.t('recover template')}
        </Menu.Item>
      </Menu>
    );
    return (
      <Dropdown overlay={menu}>
        <span className="inline-flex items-center text-xs mr-2 cursor-pointer">
          {tplName ? `${i18n.t('selected template')}:${tplName.replace('.md', '')}` : i18n.t('select template')}{' '}
          <ErdaIcon type="down" size="16" />
        </span>
      </Dropdown>
    );
  };

  getFieldsList = () => {
    const {
      info,
      mrStats,
      formData,
      moveToDiff,
      params: { appId },
      sideFold,
    } = this.props;
    const { tplContent } = this.state;
    let disableSubmitTip: string | null = null;
    if (mrStats.hasError) {
      disableSubmitTip = i18n.t('dop:merge request has errors');
    }
    const {
      sourceBranch,
      targetBranch,
      removeSourceBranch = true,
      title,
      description,
      assigneeId,
    } = (formData || {}) as any;

    const fieldExtraProps = {
      style: {
        marginBottom: '16px',
      },
    };
    const fieldsList = [
      {
        label: '',
        getComp: () => <div className="section-title">{i18n.t('dop:choose branch')}</div>,
        extraProps: fieldExtraProps,
      },
      {
        label: '',
        formItemLayout: { labelCol: { span: 0 }, wrapperCol: { span: 18, offset: 3 } },
        name: 'branch',
        getComp: () => (
          <SourceTargetSelect
            mrStats={mrStats}
            defaultSourceBranch={sourceBranch}
            defaultTargetBranch={targetBranch || info.defaultBranch}
            defaultRemoveSourceBranch={removeSourceBranch}
            disableSourceBranch={!!formData}
            onChange={this.onBranchChange}
            onCompare={this.onCompare}
            branches={info.branches || []}
            moveToDiff={moveToDiff}
          />
        ),
        rules: [
          {
            validator: (_rule: any, value: any, callback: Function) => {
              if (!value || !value.sourceBranch) {
                callback(i18n.t('dop:no comparison branch selected'));
                return;
              }
              callback();
            },
          },
        ],
        extraProps: fieldExtraProps,
      },
      {
        label: '',
        getComp: () => <div className="section-title">{i18n.t('basic information')}</div>,
        extraProps: {
          style: {
            marginTop: '32px',
            marginBottom: '0',
          },
        },
      },
      {
        label: i18n.t('title'),
        name: 'title',
        initialValue: title || '',
        itemProps: {
          maxLength: 200,
        },
        extraProps: fieldExtraProps,
      },
      {
        label: i18n.t('description'),
        name: 'description',
        initialValue: description || tplContent || '',
        getComp: () => (
          <MarkdownEditor
            onChange={(content) => this.setState({ tplContent: content })}
            extraRight={this.getTplSelect()}
          />
        ),
        extraProps: fieldExtraProps,
      },
      {
        label: i18n.t('designated person'),
        name: 'assigneeId',
        initialValue: assigneeId,
        getComp: () => {
          return <MemberSelector scopeId={appId} scopeType="app" showSelfChosen />;
        },
        extraProps: fieldExtraProps,
      },
      {
        label: '',
        isTailLayout: true,
        tailFormItemLayout: {
          wrapperCol: {
            span: 21,
          },
        },
        getComp: ({ form }: { form: FormInstance }) => (
          <div className={`page-bottom-bar ${sideFold ? 'fold' : 'unfold'}`}>
            <Tooltip title={disableSubmitTip}>
              <Button type="primary" disabled={!!disableSubmitTip} onClick={() => this.handleSubmit(form)}>
                {i18n.t('submit')}
              </Button>
            </Tooltip>
            <Button className="ml-3" onClick={this.onCancel}>
              {i18n.t('cancel')}
            </Button>
          </div>
        ),
      },
    ];
    return fieldsList;
  };

  createTemplate = (values: any) => {
    const { templateConfig, getTemplateConfig } = this.props;
    const { tplContent } = this.state;
    const { branch, path } = templateConfig;
    this.props
      .commit({
        message: `Add merge request template: ${values.name}`,
        branch,
        actions: [
          {
            action: 'add',
            content: tplContent,
            path: `${path}/${values.name}.md`,
            pathType: 'blob',
          },
        ],
      })
      .then((res: any) => {
        if (res.success) {
          message.success(i18n.t('template was created successfully'));
          this.toggleTplModel(false);
          getTemplateConfig();
        }
      });
  };

  handleSubmit = (form: FormInstance) => {
    form.validateFields().then((values: any) => {
      const { createMR, operateMR, getRepoInfo, onOk, formData, info } = this.props;
      const { branch, ...rest } = values;
      const { sourceBranch, targetBranch, removeSourceBranch } = branch;
      // 源分支为默认分支时禁止删除
      const sourceIsDefaultBranch = info.defaultBranch === sourceBranch;
      const data = {
        ...rest,
        sourceBranch,
        targetBranch,
        removeSourceBranch: sourceIsDefaultBranch ? false : removeSourceBranch,
      };
      if (formData) {
        data.action = 'edit';
        operateMR(data).then((result) => {
          // 更新列表tab上的统计数据
          getRepoInfo();
          onOk(result);
          this.form.current?.resetFields();
          this.props.clearMRStats();
        });
      } else {
        createMR(data).then((result) => {
          getRepoInfo();
          onOk(result);
          this.form.current?.resetFields();
          this.props.clearMRStats();
        });
      }
    });
  };

  render() {
    const { templateConfig } = this.props;
    const { tplModelVisible, tplContent } = this.state;
    return (
      <div className="repo-mr-form">
        <TplModel
          visible={tplModelVisible}
          tplContent={tplContent}
          templateConfig={templateConfig}
          onOk={this.createTemplate}
          onCancel={() => {
            this.toggleTplModel(false);
          }}
        />
        <RenderForm ref={this.form} layout="vertical" list={this.getFieldsList()} />
      </div>
    );
  }
}

const Mapper = () => {
  const [info, mrStats, templateConfig] = repoStore.useStore((s) => [s.info, s.mrStats, s.templateConfig]);
  const params = routeInfoStore.useStore((s) => s.params);
  const sideFold = layoutStore.useStore((s) => s.sideFold);
  const { getRepoInfo, getRepoBlob, getCompareDetail, getMRStats, createMR, operateMR, getTemplateConfig, commit } =
    repoStore.effects;
  const { clearMRStats } = repoStore.reducers;
  return {
    sideFold,
    info,
    mrStats,
    params,
    templateConfig,
    getRepoInfo,
    getRepoBlob,
    getCompareDetail,
    getMRStats,
    clearMRStats,
    createMR,
    operateMR,
    getTemplateConfig,
    commit,
  };
};

export default connectCube(connectUser(RepoMRForm), Mapper);
