import React from "react";
import { render } from "@testing-library/react";
import {
  IBasicSelectInputProps,
  BasicSelectInput,
} from "../../src/app/components/BasicSelectInput";

describe("<BasicSelectInput/>", () => {
  const renderSetup = (props: IBasicSelectInputProps) => {
    return render(<BasicSelectInput {...props} />);
  };

  it("should render default ConnectorTypeComponent ", () => {
    const propertyChange = jest.fn();
    const props: IBasicSelectInputProps = {
      label: "my",
      fieldId: "my",
      options: ["1", "2"],
      propertyChange,
    };
    const { container } = renderSetup(props);

    expect(container.getElementsByClassName("basic-select-input").length).toBe(
      1
    );
  });
});
