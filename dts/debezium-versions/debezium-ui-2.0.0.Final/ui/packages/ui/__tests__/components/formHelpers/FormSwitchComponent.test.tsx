import React from "react";
import { render, screen } from "@testing-library/react";
import {
  IFormSwitchComponentProps,
  FormSwitchComponent,
} from "../../../src/app/components/formHelpers/FormSwitchComponent";

const fieldMock = {};
const metaMock = {};
const helperMock = {};

jest.mock("formik", () => ({
  ...jest.requireActual("formik"),
  useField: jest.fn(() => {
    return [fieldMock, metaMock, helperMock];
  }),
}));

describe("<FormSwitchComponent />", () => {
  const renderSetup = (props: IFormSwitchComponentProps) => {
    return render(<FormSwitchComponent {...props} />);
  };

  it("should render FormSwitchComponent", () => {
    const propertyChangeMock = jest.fn()
    const setFieldValueMock = jest.fn()

    const props: IFormSwitchComponentProps = {
      label: "SwitchCompLabel",
      description: "SwitchComp Description",
      name: "SwitchCompName",
      fieldId: "SwitchComp",
      isChecked: true,
      propertyChange: propertyChangeMock,
      setFieldValue: setFieldValueMock,
    };
    renderSetup(props);

    expect(screen.getAllByText(props.label).length).toBe(2);
  });
});