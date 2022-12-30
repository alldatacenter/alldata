import React from "react";
import { render } from "@testing-library/react";
import { PageNotFound } from "../../src/app/components/PageNotFound";

describe("<PageNotFound/>", () => {

  it("should render PageNotFound", () => {
    const { getByTestId } = render(<PageNotFound />);
    const button = getByTestId("btn-not-found-home");
    expect(button).toBeInTheDocument();
  });
});
