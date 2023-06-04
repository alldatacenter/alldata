import { Store } from '@reduxjs/toolkit';
import { render } from '@testing-library/react';
import * as React from 'react';
import { Provider } from 'react-redux';
import { configureAppStore } from 'redux/configureStore';
import { useTheme } from 'styled-components';
import { selectTheme } from '../slice/selectors';
import { ThemeProvider } from '../ThemeProvider';

const renderThemeProvider = (store: Store, Child: React.FunctionComponent) =>
  render(
    <Provider store={store}>
      <ThemeProvider>
        <Child />
      </ThemeProvider>
    </Provider>,
  );

describe('<ThemeProvider />', () => {
  let store: ReturnType<typeof configureAppStore>;

  beforeEach(() => {
    store = configureAppStore();
  });

  it('should render its children', () => {
    const text = 'Test';
    const children = () => <h1>{text}</h1>;
    const { queryByText } = renderThemeProvider(store, children);
    expect(queryByText(text)).toBeInTheDocument();
  });

  it('should render selected theme', () => {
    let theme: any;
    const children = () => {
      // eslint-disable-next-line react-hooks/rules-of-hooks
      theme = useTheme();
      return <h1>a</h1>;
    };
    renderThemeProvider(store, children);
    expect(theme).toBe(selectTheme(store.getState() as any));
  });
});
