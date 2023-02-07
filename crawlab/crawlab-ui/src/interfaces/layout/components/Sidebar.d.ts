export declare global {
  interface MenuItem extends TreeNode<MenuItem> {
    title: string;
    path?: string;
    icon?: string | string[];
    hidden?: boolean;
  }
}
