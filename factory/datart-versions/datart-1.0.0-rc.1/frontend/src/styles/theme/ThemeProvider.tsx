import React, { useLayoutEffect } from 'react';
import { useSelector } from 'react-redux';
import { ThemeProvider as OriginalThemeProvider } from 'styled-components';
import { useThemeSlice } from './slice';
import { selectTheme, selectThemeKey } from './slice/selectors';
import { changeAntdTheme } from './utils';

export const ThemeProvider = (props: { children: React.ReactChild }) => {
  useThemeSlice();

  const theme = useSelector(selectTheme);
  const themeKey = useSelector(selectThemeKey);

  useLayoutEffect(() => {
    changeAntdTheme(themeKey);
  }, []); // eslint-disable-line

  return (
    <OriginalThemeProvider theme={theme}>
      {React.Children.only(props.children)}
    </OriginalThemeProvider>
  );
};
