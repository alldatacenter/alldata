interface FileEditorProps {
  content: string;
  navItems: FileNavItem[];
  activeNavItem?: FileNavItem;
  defaultExpandedKeys: string[];
}
