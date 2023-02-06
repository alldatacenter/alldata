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

import { FormComponentProps, FormInstance } from 'core/common/interface';
import { isEqual, map, cloneDeep, keyBy, isEmpty } from 'lodash';
import { ErdaIcon } from 'common';
import React, { PureComponent } from 'react';
import classnames from 'classnames';
import { PLAN_NAME } from 'app/modules/addonPlatform/pages/common/configs';
import { convertAddonPlan } from '../yml-flow-util';
import { Input, Form, Select, Radio, Button, Alert } from 'antd';
import addon_png from 'app/images/resources/addon.png';
import i18n from 'i18n';
import './create-add-on.scss';

const { Item } = Form;
const { Option } = Select;
const RadioButton = Radio.Button;
const RadioGroup = Radio.Group;

interface IAddonGroup {
  groupName: string;
  data: IAddon[];
}

interface ICreateAddOnProps {
  onSubmit: (options: any) => void;
  cancel: () => void;
  addOn?: any;
  editing?: boolean;
  groupedAddonList: any[];
  className?: string;
  getAddonVersions: (addonName: string) => Promise<any>;
}

interface IAddon {
  name: string;
  instanceId: string;
  logoUrl: string;
}

interface IAddOnProps {
  reselect?: boolean;
  reselectFunc?: () => void;
  editing: boolean;
  className?: string | null;
  addon: IAddon;
  onClick?: (addon: IAddon) => void;
}

const getGroupData = (props: any) => {
  const { addOn } = props;
  if (!addOn) return null;
  const splits = addOn.plan.split(':');
  const config = splits[1];
  let addonName = splits[0];

  if (addonName === 'zookeeper') {
    addonName = 'terminus-zookeeper';
  }

  return {
    alias: addOn.name,
    name: addonName,
    config,
    version: addOn.options ? addOn.options.version : null,
  };
};

const convertData = (groupedAddonList: any[]) => {
  let result: any[] = [];
  const groups: any[] = [];
  map(groupedAddonList, (value: any[], key: string) => {
    const filterResult = value.filter((item: any) => (key === '第三方' && item.instanceId) || key !== '第三方');
    result = result.concat(filterResult);
    groups.push({
      groupName: key,
      data: filterResult,
    });
  });

  return {
    groups,
    groupedAddonList: result,
  };
};

const AddOn = ({ addon, className, onClick, editing, reselect, reselectFunc }: IAddOnProps) => {
  const [imgSrc, setImgSrc] = React.useState(addon.logoUrl);
  const onError = () => {
    setImgSrc(addon_png);
  };

  return (
    <div onClick={() => onClick && editing && onClick(addon)} className={classnames('dice-yml-add-on', className)}>
      <span className="add-on-icon-container">
        <img src={imgSrc} className="add-on-icon" alt="addon-image" onError={onError} />
      </span>
      <span className="add-on-info">
        <div className="add-on-info-name">
          <span className="display-name">{addon.displayName}</span>
          {addon.instanceId ? <span className="tag-default">{i18n.t('instance')}</span> : null}
          {reselect ? (
            <a onClick={reselectFunc} className="reselect">
              {i18n.t('dop:reselect')}
            </a>
          ) : null}
        </div>
        <div className="add-on-info-description">{addon.desc || ''}</div>
      </span>
      <div className="add-on-border-bottom" />
    </div>
  );
};

class CreateAddOn extends PureComponent<ICreateAddOnProps & FormComponentProps, any> {
  formRef = React.createRef<FormInstance>();

  state = {
    groups: [],
    packUpTabs: new Set(),
    searchValue: undefined,
    editAddon: null,
    selectedAddon: null,
    // 是否重置过， 重置过的就不显示警告了
    isReset: false,
    selectedAddonVersions: [],
    versionMap: {},
    selectedAddonPlans: [],
  };

  static getDerivedStateFromProps(nextProps: Readonly<ICreateAddOnProps>, prevState: any): any {
    const result = getGroupData(nextProps);
    if (
      !isEqual(nextProps.groupedAddonList, prevState.propsGroupedAddonList) ||
      !isEqual(result, prevState.editAddon)
    ) {
      const { groupedAddonList, groups } = convertData(nextProps.groupedAddonList);
      let addonResource: any = null;
      if (nextProps.addOn && result) {
        addonResource = groupedAddonList.find((item: IAddon) => {
          const compareVal = (result.name === 'custom' ? result.alias : result.name).toLowerCase();
          // custom的addon不能编辑alias，用alais比对
          return item.name.toLowerCase() === compareVal;
        });
      }

      const packUpTabs = new Set();
      groups.forEach((i: IAddonGroup) => {
        packUpTabs.add(i.groupName);
      });

      return {
        groups,
        packUpTabs,
        selectedAddon: addonResource,
        editAddon: result,
        propsGroupedAddonList: nextProps.groupedAddonList,
        originGroupedAddonList: groupedAddonList,
      };
    }

    return prevState;
  }

  componentDidMount() {
    const { selectedAddon } = this.state;
    if (selectedAddon) {
      this.getAddonVersions();
    }
  }

  componentDidUpdate(prevProps, prevState) {
    const { selectedAddon } = this.state;
    if (selectedAddon && selectedAddon !== prevState.selectedAddon) {
      this.getAddonVersions();
    }
  }

  render() {
    const { editing, addOn } = this.props;

    const { selectedAddon, searchValue, isReset } = this.state;
    let alert;

    if (addOn && !selectedAddon && !isReset) {
      alert = (
        <Alert
          className="addon-error-tag"
          showIcon
          message={
            editing
              ? i18n.t('dop:the current instance does not exist, please add it again!')
              : i18n.t('dop:yml-addon-not-exist-tip')
          }
          type="error"
        />
      );
    }
    let content = (
      <React.Fragment>
        <Input.Search
          autoFocus
          disabled={!editing}
          onFocus={this.onFocus}
          onClick={this.openSelect}
          onChange={this.searchInputChange}
          value={searchValue}
          className="add-on-input"
          placeholder={`${i18n.t('dop:please choose')} Add-on`}
        />
      </React.Fragment>
    );

    if (selectedAddon) {
      content = (
        <React.Fragment>
          <AddOn reselect={editing} reselectFunc={this.clear} addon={selectedAddon} />
        </React.Fragment>
      );
    }
    const className = selectedAddon ? 'selected-addon' : null;

    const showContent = (
      <>
        <div className="add-on-head">{content}</div>
        {!selectedAddon ? this.renderSelectContent() : this.renderForm()}
      </>
    );

    return (
      <div className={classnames('add-on-select', className)}>
        {alert ? (
          editing ? (
            <>
              {alert}
              {showContent}
            </>
          ) : (
            alert
          )
        ) : (
          showContent
        )}
      </div>
    );
  }

  private getAddonVersions = () => {
    const { selectedAddon } = this.state;
    if (selectedAddon.addonName || selectedAddon.name) {
      this.props.getAddonVersions(selectedAddon.addonName || selectedAddon.name).then((data) => {
        this.setState({
          selectedAddonVersions: map(data, (item) => item.version),
          versionMap: keyBy(data, 'version'),
          selectedAddonPlans: Object.keys(data[0].spec.plan || { basic: {} }),
        });
      });
    }
  };

  private renderForm = () => {
    const { selectedAddon, editAddon, selectedAddonVersions, versionMap, selectedAddonPlans } = this.state;
    const { cancel, editing } = this.props;
    const form = this.formRef.current || {};
    const { getFieldValue, setFieldsValue } = form;
    if (!selectedAddon) {
      return null;
    }

    let nameValue;
    let versionValue;
    let planValue;

    if (editAddon) {
      if (editAddon.version) {
        versionValue = editAddon.version;
      }

      nameValue = editAddon.alias || editAddon.name;

      if (editAddon.config) {
        planValue = editAddon.config;
      }

      if (selectedAddon && selectedAddonPlans.length && !planValue) {
        planValue = selectedAddonPlans[0];
      }

      if (selectedAddon && selectedAddonVersions.length && !versionValue) {
        versionValue = selectedAddonVersions[0];
      }
    } else if (selectedAddon && selectedAddon.instanceId) {
      // 如果是 addon 实例，则只读
      nameValue = selectedAddon.name;
      versionValue = selectedAddon.version;
      planValue = selectedAddon.plan;
    }

    const name = (
      <Item
        name="alias"
        label={i18n.t('name')}
        initialValue={nameValue}
        rules={[
          {
            required: true,
            message: i18n.t('dop:please enter a name'),
          },
        ]}
      >
        <Input autoFocus disabled={this.isEditing()} placeholder={i18n.t('dop:please enter a name')} />
      </Item>
    );

    const version = (
      <Item
        name="version"
        label={i18n.t('version')}
        initialValue={versionValue}
        rules={[
          {
            required: true,
            message: i18n.t('dop:please select the version'),
          },
        ]}
      >
        <Select
          disabled={this.isEditing()}
          className="w-full"
          placeholder={i18n.t('dop:please select the version')}
          onSelect={() => setFieldsValue?.({ plan: undefined })}
        >
          {selectedAddonVersions.map((v: string) => (
            <Option key={v}>{v}</Option>
          ))}
        </Select>
      </Item>
    );

    // @ts-ignore
    let plans = [];
    if (selectedAddon.plan) {
      plans.push({
        plan: planValue,
        planCnName: PLAN_NAME[planValue],
      });
    } else if (getFieldValue?.('version') && !isEmpty(versionMap)) {
      plans = map(versionMap[getFieldValue?.('version')].spec.plan || { basic: {} }, (_, k) => ({
        plan: k,
        planCnName: PLAN_NAME[k],
      }));
    } else if (selectedAddonPlans?.length) {
      plans = map(selectedAddonPlans, (k) => ({ plan: k, planCnName: PLAN_NAME[k] }));
    } else {
      plans = map({ basic: {} }, (_, k) => ({
        plan: k,
        planCnName: PLAN_NAME[k],
      }));
    }
    const plan = (
      <Item
        name="plan"
        label={i18n.t('dop:configuration')}
        initialValue={convertAddonPlan(planValue)}
        rules={[
          {
            required: true,
            message: i18n.t('dop:please select configuration'),
          },
        ]}
      >
        <RadioGroup disabled={this.isEditing()}>
          {plans.map((p: any) => (
            <RadioButton key={p.plan} value={p.plan}>
              {p.planCnName}
            </RadioButton>
          ))}
        </RadioGroup>
      </Item>
    );

    return (
      <Form ref={this.formRef} layout="vertical" className="add-on-form">
        {name}
        {version}
        {plan}
        {editing ? (
          <Item className="add-on-form-btn-group">
            <Button className="mr-2" onClick={cancel}>
              {i18n.t('cancel')}
            </Button>
            <Button type="primary" onClick={this.submitAddon}>
              {i18n.t('save')}
            </Button>
          </Item>
        ) : null}
      </Form>
    );
  };

  private isEditing() {
    const { editing } = this.props;
    const { editAddon, selectedAddon } = this.state;

    return !editing || (editAddon && editAddon.instanceId) || (selectedAddon && selectedAddon.instanceId);
  }

  private submitAddon = () => {
    const { selectedAddon, editAddon } = this.state;
    const { onSubmit, cancel } = this.props;
    const form = this.formRef.current;

    form
      ?.validateFields()
      .then((values: any) => {
        onSubmit({
          ...values,
          originName: editAddon ? editAddon.alias : null,
          plan: `${selectedAddon.addonName || selectedAddon.name}:${values.plan}`,
        });
        cancel();
      })
      .catch(({ errorFields }: { errorFields: Array<{ name: any[]; errors: any[] }> }) => {
        form?.scrollToField(errorFields[0].name);
      });
  };

  private openSelect = () => {
    const { editing } = this.props;
    if (!editing) {
      return;
    }
    this.setState({
      isSelected: !this.state.isSelected,
    });
  };

  private onFocus = (e: any) => {
    const { editing } = this.props;
    if (!editing) {
      return;
    }
    e.stopPropagation();
  };

  private clear = () => {
    this.setState({
      isReset: true,
      searchValue: null,
      isSelected: false,
      selectedAddon: null,
    });
  };

  private renderSelectContent = () => {
    const { editing } = this.props;
    const { packUpTabs, groups, selectedAddon, searchValue } = this.state;

    return groups.map((group: IAddonGroup) => {
      let addonsContent = [];
      let headClass = 'empty-content';
      if (packUpTabs.has(group.groupName)) {
        addonsContent = group.data.map((addon: IAddon) => {
          if (searchValue && !addon.name.includes(searchValue) && !addon.displayName.includes(searchValue)) {
            return null;
          }

          headClass = null;
          let activeClass = null;
          // @ts-ignore
          if (selectedAddon && selectedAddon.instanceId === addon.instanceId) {
            activeClass = 'add-on-selected';
          }
          return (
            <AddOn
              editing={editing}
              className={activeClass}
              addon={addon}
              key={addon.instanceId || addon.id}
              onClick={this.selectedAddonAction}
            />
          );
        });
      }

      const icon = packUpTabs.has(group.groupName) ? (
        <ErdaIcon type="down" className="head-icon" size="18px" />
      ) : (
        <ErdaIcon type="up" className="head-icon" size="18px" />
      );

      const content = packUpTabs.has(group.groupName) ? <div className="addon-group-body">{addonsContent}</div> : null;

      return (
        <div key={group.groupName} className="yml-addon-group">
          <div
            className={classnames('addon-group-head', headClass)}
            onClick={() => this.triggerGroupTab(group.groupName)}
          >
            {icon}
            <span className="group-name">{group.groupName}</span>
          </div>
          {content}
        </div>
      );
    });
  };

  private triggerGroupTab = (name: string) => {
    const { packUpTabs } = this.state;

    if (packUpTabs.has(name)) {
      packUpTabs.delete(name);
    } else {
      packUpTabs.add(name);
    }

    this.setState({
      packUpTabs: cloneDeep(packUpTabs),
    });
  };

  private searchInputChange = (e: any) => {
    const { editing } = this.props;
    if (!editing) {
      return;
    }
    this.setState({
      searchValue: e.target.value,
    });
  };

  private selectedAddonAction = (addon: IAddon) => {
    this.setState({
      selectedAddon: addon,
      searchValue: addon.name,
    });
  };
}

export default CreateAddOn;
