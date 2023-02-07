import {ComponentOptionsMixin} from 'vue';
import CodeMirror from 'codemirror';
import 'codemirror/mode/meta';

export const mapElements = (elements: any) => {
  return Object.keys(elements).map((name) => [name, elements[name]] as [string, ComponentOptionsMixin]);
};

export const importStylesheets = () => {
  import('normalize.css/normalize.css');
  import('font-awesome/css/font-awesome.min.css');
  import('element-plus/theme-chalk/index.css');
  import('codemirror/lib/codemirror.css');
  import('@/styles/index.scss');
};

export const importScripts = () => {
  // TODO: implement
};

export const initWindowGlobals = () => {
  window.CodeMirror = CodeMirror;
};
