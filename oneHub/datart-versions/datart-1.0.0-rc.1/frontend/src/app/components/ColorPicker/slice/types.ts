import { ReactNode } from 'react';

export interface colorSelectionPropTypes {
  color?: string;
  onChange?: (color) => void;
}
export interface themeColorPropTypes {
  children: ReactNode;
  callbackFn: (Array) => void;
}
