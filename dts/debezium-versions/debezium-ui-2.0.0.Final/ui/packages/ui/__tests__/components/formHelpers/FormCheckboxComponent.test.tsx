import React from "react";
import { render, screen } from "@testing-library/react";
import {
  IFormCheckboxComponentProps,
  FormCheckboxComponent,
} from "../../../src/app/components/formHelpers/FormCheckboxComponent";

const fieldMock = {};
const metaMock = {};
const helperMock = {};

jest.mock("formik", () => ({
  ...jest.requireActual("formik"),
  useField: jest.fn(() => {
    return [fieldMock, metaMock, helperMock];
  }),
}));

describe("<FormCheckboxComponent />", () => {
  const renderSetup = (props: IFormCheckboxComponentProps) => {
    return render(<FormCheckboxComponent {...props} />);
  };

  it("should render FormCheckboxComponent", () => {
    const propertyChangeMock = jest.fn()
    const setFieldValueChangeMock = jest.fn()

    const props: IFormCheckboxComponentProps = {
      label: "CheckMe",
      fieldId: "CheckMe",
      name: "CheckMe",
      description: "Checkbox Description",
      isChecked: true,
      propertyChange: propertyChangeMock,
      setFieldValue: setFieldValueChangeMock
    };
    renderSetup(props);

    expect(screen.getByText(props.label)).toBeInTheDocument();
  });
});