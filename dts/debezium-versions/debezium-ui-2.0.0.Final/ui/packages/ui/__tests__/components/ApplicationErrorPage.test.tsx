import React from "react";
import { render } from "@testing-library/react";
import {
  IApplicationErrorPageProps,
  ApplicationErrorPage,
} from "../../src/app/components/ApplicationErrorPage";

describe("<ApplicationErrorPage/>", () => {
  const renderSetup = (props: IApplicationErrorPageProps) => {
    return render(<ApplicationErrorPage {...props} />);
  };

  it("should render default ApplicationErrorPage ", () => {
    const props: IApplicationErrorPageProps = {
      title: "Title",
      msg: "Message",
    };
    const { getByTestId } = renderSetup(props);
    const button = getByTestId("error-btn-artifacts");
    expect(button).toBeInTheDocument();
  });
});
