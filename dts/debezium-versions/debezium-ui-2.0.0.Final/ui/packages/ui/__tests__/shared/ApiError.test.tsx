import React from "react";
import { render } from "@testing-library/react";
import {
  IApiErrorProps,
  ApiError,
} from "../../src/app/shared/ApiError";

describe("<ApiError/>", () => {
  const renderSetup = (props: IApiErrorProps) => {
    return render(<ApiError {...props} />);
  };

  it("should render ApiError ", () => {
    const props: IApiErrorProps = {
      error: "TheError",
    };
    const { container } = renderSetup(props);
    
    expect(container.getElementsByClassName("app-page-section-border-bottom").length).toBe(1);
  });
});
