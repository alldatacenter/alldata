import React from "react";
import { render, screen } from "@testing-library/react";
import {
  IFormSelectComponentProps,
  FormSelectComponent,
} from "../../../src/app/components/formHelpers/FormSelectComponent";

const fieldMock = {};
const metaMock = {};
const helperMock = {};

jest.mock("formik", () => ({
  ...jest.requireActual("formik"),
  useField: jest.fn(() => {
    return [fieldMock, metaMock, helperMock];
  }),
}));

describe("<FormSelectComponent />", () => {
  const renderSetup = (props: IFormSelectComponentProps) => {
    return render(<FormSelectComponent {...props} />);
  };

  it("should render FormSelectComponent", () => {
    const propertyChangeMock = jest.fn()
    const setFieldValueChangeMock = jest.fn()

    const props: IFormSelectComponentProps = {
      label: "SelectComp",
      description: "SelectComp Description",
      name: "SelectComp",
      fieldId: "SelectComp",
      isRequired: true,
      helperTextInvalid: "Invalid text",
      options: [],
      propertyChange: propertyChangeMock,
      setFieldValue: setFieldValueChangeMock
    };
    renderSetup(props);

    expect(screen.getByText(props.label)).toBeInTheDocument();
  });
});