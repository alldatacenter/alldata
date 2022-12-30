import React from "react";
import { render } from "@testing-library/react";
import { AppLayout } from "../../src/app/layout/AppLayout";
import { BrowserRouter } from 'react-router-dom';
describe("<AppLayout/>", () => {
  

  it("should render AppLayout with expected spinner size", () => {
    const children = <div>page content</div>
    const { container } = render(<BrowserRouter><AppLayout>{children}</AppLayout></BrowserRouter>);

    
    expect(container.getElementsByClassName("app-page").length).toBe(1);
  });
});
