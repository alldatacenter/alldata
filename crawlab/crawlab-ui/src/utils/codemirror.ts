import CodeMirror, {Editor, EditorConfiguration} from 'codemirror';

// import addons
import 'codemirror/addon/search/search.js';
import 'codemirror/addon/search/searchcursor';
import 'codemirror/addon/search/matchesonscrollbar.js';
import 'codemirror/addon/search/matchesonscrollbar.css';
import 'codemirror/addon/search/match-highlighter';
import 'codemirror/addon/edit/matchtags';
import 'codemirror/addon/edit/matchbrackets';
import 'codemirror/addon/edit/closebrackets';
import 'codemirror/addon/edit/closetag';
import 'codemirror/addon/comment/comment';
import 'codemirror/addon/hint/show-hint';

// import keymap
import 'codemirror/keymap/emacs.js';
import 'codemirror/keymap/sublime.js';
import 'codemirror/keymap/vim.js';
import {translate} from '@/utils/i18n';

// i18n
const t = translate;

const themes = [
  '3024-day',
  '3024-night',
  'abcdef',
  'ambiance',
  'ambiance-mobile',
  'ayu-dark',
  'ayu-mirage',
  'base16-dark',
  'base16-light',
  'bespin',
  'blackboard',
  'cobalt',
  'colorforth',
  'darcula',
  'dracula',
  'duotone-dark',
  'duotone-light',
  'eclipse',
  'elegant',
  'erlang-dark',
  'gruvbox-dark',
  'hopscotch',
  'icecoder',
  'idea',
  'isotope',
  'lesser-dark',
  'liquibyte',
  'lucario',
  'material',
  'material-darker',
  'material-ocean',
  'material-palenight',
  'mbo',
  'mdn-like',
  'midnight',
  'monokai',
  'moxer',
  'neat',
  'neo',
  'night',
  'nord',
  'oceanic-next',
  'panda-syntax',
  'paraiso-dark',
  'paraiso-light',
  'pastel-on-dark',
  'railscasts',
  'rubyblue',
  'seti',
  'shadowfox',
  'solarized',
  'ssms',
  'the-matrix',
  'tomorrow-night-bright',
  'tomorrow-night-eighties',
  'ttcn',
  'twilight',
  'vibrant-ink',
  'xq-dark',
  'xq-light',
  'yeti',
  'yonce',
  'zenburn',
];

const template = ``;

const getOptionsDefinitions = (): FileEditorOptionDefinition[] => [
  {
    name: 'theme',
    title: t('components.file.editor.settings.form.title.theme'),
    description: t('components.file.editor.settings.form.description.theme'),
    type: 'select',
    data: {
      options: themes,
    },
  },
  {
    name: 'indentUnit',
    title: t('components.file.editor.settings.form.title.indentUnit'),
    description: t('components.file.editor.settings.form.description.indentUnit'),
    type: 'input-number',
    data: {
      min: 1,
    }
  },
  {
    name: 'smartIndent',
    title: t('components.file.editor.settings.form.title.smartIndent'),
    description: t('components.file.editor.settings.form.description.smartIndent'),
    type: 'switch',
  },
  {
    name: 'tabSize',
    title: t('components.file.editor.settings.form.title.tabSize'),
    description: t('components.file.editor.settings.form.description.tabSize'),
    type: 'input-number',
    data: {
      min: 1,
    }
  },
  {
    name: 'indentWithTabs',
    title: t('components.file.editor.settings.form.title.indentWithTabs'),
    description: t('components.file.editor.settings.form.description.indentWithTabs'),
    type: 'switch',
  },
  {
    name: 'electricChars',
    title: t('components.file.editor.settings.form.title.electricChars'),
    description: t('components.file.editor.settings.form.description.electricChars'),
    type: 'switch',
  },
  {
    name: 'keyMap',
    title: t('components.file.editor.settings.form.title.keyMap'),
    description: t('components.file.editor.settings.form.description.keyMap'),
    type: 'select',
    data: {
      options: [
        'default',
        'emacs',
        'sublime',
        'vim',
      ]
    },
  },
  {
    name: 'lineWrapping',
    title: t('components.file.editor.settings.form.title.lineWrapping'),
    description: t('components.file.editor.settings.form.description.lineWrapping'),
    type: 'switch',
  },
  {
    name: 'lineNumbers',
    title: t('components.file.editor.settings.form.title.lineNumbers'),
    description: t('components.file.editor.settings.form.description.lineNumbers'),
    type: 'switch',
  },
  {
    name: 'showCursorWhenSelecting',
    title: t('components.file.editor.settings.form.title.showCursorWhenSelecting'),
    description: t('components.file.editor.settings.form.description.showCursorWhenSelecting'),
    type: 'switch',
  },
  {
    name: 'lineWiseCopyCut',
    title: t('components.file.editor.settings.form.title.lineWiseCopyCut'),
    description: t('components.file.editor.settings.form.description.lineWiseCopyCut'),
    type: 'switch',
  },
  {
    name: 'pasteLinesPerSelection',
    title: t('components.file.editor.settings.form.title.pasteLinesPerSelection'),
    description: t('components.file.editor.settings.form.description.pasteLinesPerSelection'),
    type: 'switch',
  },
  {
    name: 'undoDepth',
    title: t('components.file.editor.settings.form.title.undoDepth'),
    description: t('components.file.editor.settings.form.description.undoDepth'),
    type: 'input-number',
    data: {
      min: 1,
    },
  },
  {
    name: 'cursorBlinkRate',
    title: t('components.file.editor.settings.form.title.cursorBlinkRate'),
    description: t('components.file.editor.settings.form.description.cursorBlinkRate'),
    type: 'input-number',
    data: {
      min: 10,
    },
  },
  {
    name: 'cursorScrollMargin',
    title: t('components.file.editor.settings.form.title.cursorScrollMargin'),
    description: t('components.file.editor.settings.form.description.cursorScrollMargin'),
    type: 'input-number',
    data: {
      min: 0,
    },
  },
  {
    name: 'cursorHeight',
    title: t('components.file.editor.settings.form.title.cursorHeight'),
    description: t('components.file.editor.settings.form.description.cursorHeight'),
    type: 'input-number',
    data: {
      min: 0,
      step: 0.01,
    },
  },
  {
    name: 'maxHighlightLength',
    title: t('components.file.editor.settings.form.title.maxHighlightLength'),
    description: t('components.file.editor.settings.form.description.maxHighlightLength'),
    type: 'input-number',
    data: {
      min: 1,
    },
  },
  {
    name: 'spellcheck',
    title: t('components.file.editor.settings.form.title.spellcheck'),
    description: t('components.file.editor.settings.form.description.spellcheck'),
    type: 'switch',
  },
  {
    name: 'autocorrect',
    title: t('components.file.editor.settings.form.title.autocorrect'),
    description: t('components.file.editor.settings.form.description.autocorrect'),
    type: 'switch',
  },
  {
    name: 'autocapitalize',
    title: t('components.file.editor.settings.form.title.autocapitalize'),
    description: t('components.file.editor.settings.form.description.autocapitalize'),
    type: 'switch',
  },
  {
    name: 'highlightSelectionMatches',
    title: t('components.file.editor.settings.form.title.highlightSelectionMatches'),
    description: t('components.file.editor.settings.form.description.highlightSelectionMatches'),
    type: 'switch',
  },
  {
    name: 'matchBrackets',
    title: t('components.file.editor.settings.form.title.matchBrackets'),
    description: t('components.file.editor.settings.form.description.matchBrackets'),
    type: 'switch',
  },
  {
    name: 'matchTags',
    title: t('components.file.editor.settings.form.title.matchTags'),
    description: t('components.file.editor.settings.form.description.matchTags'),
    type: 'switch',
  },
  {
    name: 'autoCloseBrackets',
    title: t('components.file.editor.settings.form.title.autoCloseBrackets'),
    description: t('components.file.editor.settings.form.description.autoCloseBrackets'),
    type: 'switch',
  },
  {
    name: 'autoCloseTags',
    title: t('components.file.editor.settings.form.title.autoCloseTags'),
    description: t('components.file.editor.settings.form.description.autoCloseTags'),
    type: 'switch',
  },
  {
    name: 'showHint',
    title: t('components.file.editor.settings.form.title.showHint'),
    description: t('components.file.editor.settings.form.description.showHint'),
    type: 'switch',
  },
];

const themeCache = new Set<string>();

export const getCodemirrorEditor = (el: HTMLElement, options: EditorConfiguration): Editor => {
  return CodeMirror(el, options);
};

export const initTheme = async (name?: string) => {
  if (!name) name = 'darcula';
  if (themeCache.has(name)) return;
  await import(`codemirror/theme/${name}.css`);
  themeCache.add(name);
};

export const getThemes = () => {
  return themes;
};

export const getCodeMirrorTemplate = () => {
  return template;
};

export const getOptionDefinition = (name: string): FileEditorOptionDefinition | undefined => {
  return getOptionsDefinitions().find(d => d.name === name);
};
