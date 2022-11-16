import React from "react";
import { render, screen } from "@testing-library/react";
import {
  IFormDurationComponentProps,
  FormDurationComponent,
} from "../../../src/app/components/formHelpers/FormDurationComponent";

const fieldMock = {};
const metaMock = {};
const helperMock = {};

jest.mock("formik", () => ({
  ...jest.requireActual("formik"),
  useField: jest.fn(() => {
    return [fieldMock, metaMock, helperMock];
  }),
}));

describe("<FormDurationComponent />", () => {
  const renderSetup = (props: IFormDurationComponentProps) => {
    return render(<FormDurationComponent {...props} />);
  };

  it("should render FormDurationComponent", () => {

    const propertyChangeMock = jest.fn()
    const setFieldValueChangeMock = jest.fn()

    const props: IFormDurationComponentProps = {
      label: "DurationComp",
      description: "DurationComp Description",
      name: "DurationComp",
      fieldId: "DurationComp",
      isRequired: true,
      validated: "default",
      propertyChange: propertyChangeMock,
      setFieldValue: setFieldValueChangeMock
    };
    renderSetup(props);

    expect(screen.getByText(props.label)).toBeInTheDocument();
  });
});