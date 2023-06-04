import * as slice from '..';
import { ThemeState } from '../types';

describe('theme slice', () => {
  let state: ThemeState;

  beforeEach(() => {
    state = slice.initialState;
  });

  it('should return the initial state', () => {
    expect(slice.reducer(undefined, { type: '' })).toEqual(state);
  });

  it('should changeTheme', () => {
    expect(
      slice.reducer(state, slice.themeActions.changeTheme('dark')),
    ).toEqual<ThemeState>({ selected: 'dark' });
  });

  describe('selectors', () => {
    it('selectTheme', () => {
      // FIXME RootState 规范化
      // let state: RootState = {};
      // expect(selectTheme(state)).toEqual<DefaultTheme>(themes.light);
      // state = {
      //   theme: { selected: 'system' },
      // };
      // expect(selectTheme(state)).toEqual<DefaultTheme>(themes.light);
      // state = {
      //   theme: { selected: 'dark' },
      // };
      // expect(selectTheme(state)).toEqual<DefaultTheme>(themes.dark);
    });

    it('selectThemeKey', () => {
      // FIXME RootState 规范化
      // let state: RootState = {};
      // expect(selectThemeKey(state)).toEqual<ThemeKeyType>(
      //   slice.initialState.selected,
      // );
      // state = {
      //   theme: { selected: 'system' },
      // };
      // expect(selectThemeKey(state)).toEqual<ThemeKeyType>(
      //   state.theme!.selected,
      // );
    });
  });
});
