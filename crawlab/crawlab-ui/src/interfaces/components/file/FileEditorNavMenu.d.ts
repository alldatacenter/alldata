interface FileEditorNavMenuProps {
  activeItem?: FileNavItem;
  items: FileNavItem[];
  defaultExpandAll: boolean;
  defaultExpandedKeys: string[];
  style?: Partial<CSSStyleDeclaration>;
}

interface FileEditorNavMenuClickStatus {
  clicked: boolean;
  item?: FileNavItem;
}

interface FileEditorNavMenuCache<T = any> {
  [key: string]: T;
}
