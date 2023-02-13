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
import { fireEvent, render } from '@testing-library/react';
import BasicCheckbox from '../Basic/BasicCheckbox';

describe('<BasicCheckbox />', () => {
  let translator;
  beforeAll(() => {
    translator = label => `This is a ${label}`;
  });

  test('should render component correct', () => {
    const { getByText, getByRole } = render(
      <BasicCheckbox
        translate={translator}
        data={{ label: 'Component Label', value: true }}
      />,
    );
    expect(getByText('This is a Component Label')).toBeInTheDocument();
    expect(getByRole('checkbox')).toBeInTheDocument();
    expect(getByRole('checkbox')).toBeChecked();
  });

  test('should fire onChange event', async () => {
    const handleOnChangeEvent = jest.fn();
    const ancestors = [];
    const needRefresh = true;

    const { getByRole } = render(
      <BasicCheckbox
        ancestors={ancestors}
        onChange={handleOnChangeEvent}
        data={{
          label: 'Component Label',
          value: true,
          options: { needRefresh },
        }}
      />,
    );
    fireEvent.click(getByRole('checkbox'));
    expect(handleOnChangeEvent).toHaveBeenCalledWith(
      ancestors,
      false,
      needRefresh,
    );
  });

  test('should hide label when options hide label', () => {
    const { container } = render(
      <BasicCheckbox
        translate={translator}
        data={{
          label: 'Component Label',
          options: { hideLabel: true },
        }}
      />,
    );
    expect(container.querySelector('.ant-form-item-label label')).toBeNull();
  });
});
