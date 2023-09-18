import darkTheme from 'antd/dist/dark-theme';
import lightTheme from 'antd/dist/default-theme';
import { StorageKeys } from 'globalConstants';
import { ThemeKeyType } from './slice/types';
import { themes } from './themes';

/* istanbul ignore next line */
export const isSystemDark = window?.matchMedia
  ? window.matchMedia('(prefers-color-scheme: dark)')?.matches
  : undefined;

export function saveTheme(theme: ThemeKeyType) {
  window.localStorage && localStorage.setItem(StorageKeys.Theme, theme);
}

/* istanbul ignore next line */
export function getThemeFromStorage(): ThemeKeyType {
  let theme = 'light' as ThemeKeyType;
  try {
    const storedTheme =
      window.localStorage && localStorage.getItem(StorageKeys.Theme);
    if (storedTheme) {
      theme = storedTheme as ThemeKeyType;
    }
  } catch (error) {
    throw error;
  }
  return theme;
}

export function getTokenVariableMapping(themeKey: string) {
  const currentTheme = themes[themeKey];
  return {
    '@primary-color': currentTheme.primary,
    '@success-color': currentTheme.success,
    '@processing-color': currentTheme.processing,
    '@error-color': currentTheme.error,
    '@highlight-color': currentTheme.highlight,
    '@warning-color': currentTheme.warning,
    '@body-background': currentTheme.bodyBackground,
    '@text-color': currentTheme.textColor,
    '@text-color-secondary': currentTheme.textColorLight,
    '@heading-color': currentTheme.textColor,
    '@disabled-color': currentTheme.textColorDisabled,
  };
}

export function getVarsToBeModified(themeKey: string) {
  const tokenVariableMapping = getTokenVariableMapping(themeKey);
  return {
    ...(themeKey === 'light' ? lightTheme : darkTheme),
    ...tokenVariableMapping,
  };
}

export async function changeAntdTheme(themeKey: string) {
  try {
    await (window as any).less.modifyVars(getVarsToBeModified(themeKey));
  } catch (error) {
    console.log(error);
  }
}
