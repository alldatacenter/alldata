import { Tabs as AntdTabs, TabsProps as AntdTabsProps } from 'antd';
import { DashboardTabStyle } from './DashboardTabStyle';
import { EditorTabsStyle } from './EditorTabStyle';
const { TabPane } = AntdTabs;

interface TabsProps extends AntdTabsProps {
  mode?: 'editor' | 'dashboard';
}

export function Tabs({ mode = 'editor', ...tabsProps }: TabsProps) {
  return (
    <>
      <AntdTabs {...tabsProps} />
      {mode === 'editor' ? <EditorTabsStyle /> : <DashboardTabStyle />}
    </>
  );
}

export { PaneWrapper } from './PaneWrapper';
export { TabPane };
