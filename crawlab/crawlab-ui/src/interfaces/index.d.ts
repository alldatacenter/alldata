declare module '*.js';
declare module '*.jpg';
declare module '*.png';
declare module '*.svg';

export declare global {
  type ElFormValidator = (rule: any, value: any, callback: any) => void;

  interface ElFormRule {
    required: boolean;
    trigger: string;
    validator: ElFormValidator;
  }
}

// export modules interfaces.
// NOTE: please note we excluded components and views as they are already exported from .vue files
export * from './common';
export * from './components';
export * from './directives';
export * from './element';
export * from './i18n';
export * from './layout';
export * from './models';
export * from './router';
export * from './package';
export * from './services';
export * from './store';
export * from './echarts';
