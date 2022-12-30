import React from "react";
import { render,screen } from "@testing-library/react";
import {
  IConnectionPropertiesErrorProps,
  ConnectionPropertiesError,
} from "../../src/app/components/ConnectionPropertiesError";

describe("<ConnectionPropertiesError/>", () => {
  const renderSetup = (props: IConnectionPropertiesErrorProps) => {
    return render(<ConnectionPropertiesError {...props} />);
  };

  it("should render ConnectionPropertiesError, empty connection props", () => {
    const props: IConnectionPropertiesErrorProps = {
      connectionPropsMsg: [],
      i18nFieldValidationErrorMsg: "Error Message",
      i18nValidationErrorMsg: "Validation Error Message",
    };
    renderSetup(props);

    expect(screen.getByText(props.i18nValidationErrorMsg)).toBeInTheDocument();
  });
});
