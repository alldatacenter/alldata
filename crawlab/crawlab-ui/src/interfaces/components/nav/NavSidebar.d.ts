interface NavSidebar {
  scroll: (id: string) => void;
}

type NavSidebarType = 'list' | 'tree';

interface NavSidebarContent {
  activeKey?: string;
  items: NavItem[],
  showCheckBox?: boolean;
}

interface NavSidebarProps extends NavSidebarContent {
  type: NavSidebarType;
  collapsed?: boolean;
  showActions?: boolean;
  defaultCheckedKeys?: string[];
  defaultExpandedKeys?: string[];
  defaultExpandAll?: boolean;
  noSearch?: boolean;
}
