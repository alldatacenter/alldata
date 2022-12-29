import { css } from 'styled-components/macro';
import { media, sizes } from '../media';

describe('media', () => {
  it('should return media query in css', () => {
    const mediaQuery = media.small`color: red;`.join('').replace(/ /g, '');
    const cssVersion = css`
      @media (min-width: ${sizes.small}px) {
        color: red;
      }
    `
      .join('')
      .replace(/ /g, '');
    expect(mediaQuery).toEqual(cssVersion);
  });
});
