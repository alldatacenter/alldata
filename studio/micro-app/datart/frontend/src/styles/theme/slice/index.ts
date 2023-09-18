import { PayloadAction } from '@reduxjs/toolkit';
import { useInjectReducer } from 'utils/@reduxjs/injectReducer';
import { createSlice } from 'utils/@reduxjs/toolkit';
import { getThemeFromStorage } from '../utils';
import { ThemeKeyType, ThemeState } from './types';

export const initialState: ThemeState = {
  selected: getThemeFromStorage(),
};

const themeSlice = createSlice({
  name: 'theme',
  initialState,
  reducers: {
    changeTheme(state, action: PayloadAction<ThemeKeyType>) {
      state.selected = action.payload;
    },
  },
});

export default themeSlice;

export const { actions: themeActions, reducer } = themeSlice;

export const useThemeSlice = () => {
  useInjectReducer({ key: themeSlice.name, reducer: themeSlice.reducer });
  return { actions: themeSlice.actions };
};
