import React from "react";
import { render, screen } from "@testing-library/react";
import { PageLoader } from "../../src/app/components/PageLoader";

describe("<PageLoader/>", () => {
  it("should render default PageLoader", () => {
    render(<PageLoader />);
    screen.getByRole("progressbar");
  });

  it("should render PageLoader with expected spinner size", () => {
    const { container } = render(<PageLoader />);

    screen.getByRole("progressbar");
    // check spinner size i.e. 'lg'
    expect(container.getElementsByClassName("pf-m-lg").length).toBe(1);
  });
});
