// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import React from 'react';
import { BackToTop } from 'common';
import { act } from 'react-dom/test-utils';
import { render, unmountComponentAtNode } from 'react-dom';

describe('BackToTop', () => {
  Element.prototype.scrollTo = function (opt?: ScrollToOptions | number) {
    if (typeof opt !== 'number') {
      const { top, left } = opt as ScrollToOptions;
      this.scrollTop = top || 0;
      this.scrollLeft = left || 0;
    }
  };
  it('should back to top', async () => {
    document.body.innerHTML = '<div id="main" style="height: 400px; overflow-y: auto"></div>';
    const div = document.getElementById('main') as HTMLDivElement;
    const scrollToSpy = jest.spyOn(window, 'scrollTo').mockImplementation((x: number, y: number) => {
      div.scrollLeft = x;
      div.scrollTop = y;
    });
    act(() => {
      render(
        <div id="child" style={{ height: '1000px' }}>
          <BackToTop />
        </div>,
        div,
      );
    });
    window.scrollTo(0, 500);
    expect(div.scrollTop).toBe(500);
    act(() => {
      div.dispatchEvent(new Event('scroll'));
    });
    const button = document.querySelector('.scroll-top-btn') as Element;
    act(() => {
      button.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    expect(div.scrollTop).toBe(0);
    scrollToSpy.mockRestore();
    unmountComponentAtNode(div);
  });
});
