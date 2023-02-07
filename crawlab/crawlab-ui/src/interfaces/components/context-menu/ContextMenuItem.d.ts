interface ContextMenuItem {
  title: string;
  icon?: string | string[];
  action?: () => void;
  className?: string;
}
