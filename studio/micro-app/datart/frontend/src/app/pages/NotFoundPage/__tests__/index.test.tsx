import React from 'react';
import { HelmetProvider } from 'react-helmet-async';
import { MemoryRouter } from 'react-router-dom';
import renderer from 'react-test-renderer';
import { NotFoundPage } from '..';

const renderPage = () =>
  renderer.create(
    <MemoryRouter>
      <HelmetProvider>
        <NotFoundPage />
      </HelmetProvider>
    </MemoryRouter>,
  );

describe('<NotFoundPage />', () => {
  // TODO(Owner): fix app tests...
  it.skip('should match snapshot', () => {
    const notFoundPage = renderPage();
    expect(notFoundPage.toJSON()).toMatchSnapshot();
  });

  it('should should contain Link', () => {});
});
