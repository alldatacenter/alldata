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
import { render } from '@testing-library/react';
import BasicColorSelector from '../Basic/BasicColorSelector';

describe('<BasicColorSelector />', () => {
  let translator;
  beforeAll(() => {
    translator = label => `This is a ${label}`;
  });

  test('should render component correct', () => {
    const { container, getByText } = render(
      <BasicColorSelector
        translate={translator}
        data={{ label: 'Component Label', value: '#fafafa' }}
      />,
    );

    expect(getByText('This is a Component Label')).toBeInTheDocument();
    expect(container.querySelector('[color*="fafafa"]')).not.toBeNull();
  });

  test('should hide label when options hide label', () => {
    const { container } = render(
      <BasicColorSelector
        data={{
          label: 'Component Label',
          options: { hideLabel: true },
        }}
      />,
    );
    expect(container.querySelector('.ant-form-item-label label')).toBeNull();
  });
});
