/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import '@testing-library/jest-dom';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import BasicFont from '../Basic/BasicFont';

describe('<BasicFont />', () => {
  let translator;
  beforeAll(() => {
    translator = label => `This is a ${label}`;
  });

  test('should render component correct', async () => {
    const { getByText, container } = render(
      <BasicFont
        ancestors={[]}
        translate={translator}
        data={{
          key: 'font',
          comType: 'font',
          label: 'Component Label',
          value: {
            color: '#ccc',
          },
        }}
      />,
    );
    const allSelectors = await screen.findAllByRole('combobox');
    expect(getByText('This is a Component Label')).toBeInTheDocument();
    expect(container.querySelector('[color*="#ccc"]')).not.toBeNull();
    expect(allSelectors).toHaveLength(4);
  });

  test('should use default translator', async () => {
    const { getByText } = render(
      <BasicFont
        ancestors={[]}
        data={{
          key: 'font',
          comType: 'font',
          label: 'Component Label',
          value: true,
        }}
      />,
    );
    expect(getByText('Component Label')).toBeInTheDocument();
  });

  test('should hide label when options hide label', async () => {
    render(
      <BasicFont
        ancestors={[]}
        translate={translator}
        data={{
          key: 'font',
          comType: 'font',
          label: 'Component Label',
          options: { hideLabel: true },
        }}
      />,
    );
    await waitFor(() => {
      expect(
        screen.queryByText('This is a Component Label'),
      ).not.toBeInTheDocument();
    });
  });

  test('should render customize font families', async () => {
    render(
      <BasicFont
        ancestors={[]}
        translate={translator}
        data={{
          key: 'font',
          comType: 'font',
          label: 'Component Label',
          options: {
            hideLabel: true,
            fontFamilies: [
              { name: 'fontA', value: 'fontA' },
              { name: 'fontB', value: 'fontB' },
            ] as any,
          },
        }}
      />,
    );
    const allSelectors = await screen.findAllByRole('combobox');
    userEvent.click(allSelectors[0]);

    await waitFor(() => {
      expect(
        screen.getByRole('option', { name: 'This is a fontA' }),
      ).toBeInTheDocument();
    });
  });

  test('should hide font size selector', async () => {
    const { container } = render(
      <BasicFont
        ancestors={[]}
        translate={translator}
        data={{
          key: 'font',
          comType: 'font',
          label: 'Component Label',
          options: {
            showFontSize: false,
            showFontStyle: false,
            showFontColor: false,
          },
        }}
      />,
    );
    const allSelectors = await screen.findAllByRole('combobox');
    expect(allSelectors).toHaveLength(2);
    expect(
      container.querySelector('[class^="ColorTag__ColorPicker"]'),
    ).toBeNull();
  });

  test.skip('should call onChange event', async () => {
    const handleOnChangeEvent = jest.fn();
    const { debug } = render(
      <BasicFont
        ancestors={[]}
        onChange={handleOnChangeEvent}
        data={{
          key: 'font',
          comType: 'font',
          label: 'Component Label',
          options: {
            hideLabel: true,
            fontFamilies: [
              { name: 'fontA', value: 'fontA' },
              { name: 'fontB', value: 'fontB' },
            ] as any,
          },
        }}
      />,
    );
    const allSelectors = await screen.findAllByRole('combobox');
    userEvent.click(allSelectors[0]);
    await waitFor(() => {
      expect(screen.getByRole('option', { name: 'fontA' })).toBeInTheDocument();
    });

    userEvent.click(screen.getByRole('option', { name: 'fontA' }));
    debug();
    await waitFor(() => {
      // expect(handleOnChangeEvent).toHaveBeenCalledWith([], {});
      expect(handleOnChangeEvent).toHaveBeenCalledTimes(1);
    });
  });
});
