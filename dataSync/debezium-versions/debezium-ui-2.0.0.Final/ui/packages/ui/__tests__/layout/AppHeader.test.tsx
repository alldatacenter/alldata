import React from "react";
import { render } from "@testing-library/react";
import { AppHeader, IAppHeader } from "../../src/app/layout/AppHeader";
import { BrowserRouter } from 'react-router-dom';

describe("<AppHeader/>", () => {
  
  const renderSetup = (props: IAppHeader) => {
    return render(<BrowserRouter><AppHeader {...props} /></BrowserRouter>);
  };

  it("should render AppHeader ", () => {
    const handleClusterChangeMock = jest.fn()

    const props: IAppHeader = {
      handleClusterChange: handleClusterChangeMock,
    };
    const { container } = renderSetup(props);

    expect(container.getElementsByClassName("brandLogo").length).toBe(
      1
    );
  });
});
