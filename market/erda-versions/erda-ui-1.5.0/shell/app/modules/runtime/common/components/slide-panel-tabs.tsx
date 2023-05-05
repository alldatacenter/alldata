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
import { map, isEmpty } from 'lodash';
import { Drawer, Tabs } from 'antd';
import { CompSwitcher } from 'common';
import commonStore from 'common/stores/common';

import './slide-panel-tabs.scss';

const { TabPane } = Tabs;

interface ITabInstance {
  instance: {
    ipAddress: string;
  };
}

export interface IWithTabs {
  defaultActiveKey: string;
  TopTabRight: React.ComponentClass;
  contents: Array<{
    Comp: React.ComponentClass;
    props: ITabInstance;
    tab: JSX.Element;
    key: any;
  }>;
}

interface IProps {
  [prop: string]: any;
  withTabs?: IWithTabs;
  content: JSX.Element;
  visible: boolean;
  slidePanelComps: COMMON.SlideComp[];
  closeSlidePanel: () => void;
}

interface IState {
  activeKey?: string;
  defaultActiveKey?: string;
  showContent: boolean;
}

class PureSlidePanel extends React.Component<IProps, IState> {
  state = {
    activeKey: undefined,
    defaultActiveKey: '',
    showContent: false,
  };

  static getDerivedStateFromProps({ withTabs }: IProps, prevState: IState) {
    if (withTabs && withTabs.defaultActiveKey !== prevState.defaultActiveKey) {
      const { defaultActiveKey } = withTabs;
      return { ...prevState, activeKey: defaultActiveKey, defaultActiveKey };
    }
    return null;
  }

  onTabChange = (key: any) => {
    this.setState({ activeKey: key });
  };

  render() {
    const { withTabs, content, visible, closeSlidePanel, title = null, slidePanelComps, ...rest } = this.props;
    const { activeKey } = this.state;

    let sliderContent = null;
    if (!isEmpty(withTabs)) {
      const { contents = [], TopTabRight = null } = withTabs || {};
      const tabBarExtraContent = TopTabRight ? <TopTabRight /> : null;
      sliderContent = (
        <Tabs
          defaultActiveKey={activeKey}
          key={activeKey}
          onChange={this.onTabChange}
          animated={false}
          tabBarExtraContent={tabBarExtraContent}
        >
          {map(contents, ({ Comp, props, key }) => {
            const { ipAddress } = props.instance;
            return (
              <TabPane tab={ipAddress} key={key}>
                <Comp {...props} />
              </TabPane>
            );
          })}
        </Tabs>
      );
    } else {
      sliderContent = content;
    }

    return (
      <Drawer
        className="slider"
        title={slidePanelComps.length ? slidePanelComps[slidePanelComps.length - 1].getTitle() : title}
        visible={visible}
        placement="right"
        width="80%"
        destroyOnClose
        onClose={closeSlidePanel}
        afterVisibleChange={(vis: boolean) => {
          if (vis && !this.state.showContent) this.setState({ showContent: true });
        }}
        {...rest}
      >
        {this.state.showContent ? <CompSwitcher comps={slidePanelComps}>{sliderContent}</CompSwitcher> : null}
      </Drawer>
    );
  }
}

export const SlidePanel = (p: any) => {
  const slidePanelComps = commonStore.useStore((s) => s.slidePanelComps);
  return <PureSlidePanel {...p} slidePanelComps={slidePanelComps} />;
};
