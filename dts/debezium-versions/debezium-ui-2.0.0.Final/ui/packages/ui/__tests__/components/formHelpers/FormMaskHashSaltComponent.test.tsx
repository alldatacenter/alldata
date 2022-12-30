import React from "react";
import { render, screen } from "@testing-library/react";
import {
  IFormMaskHashSaltComponentProps,
  FormMaskHashSaltComponent,
} from "../../../src/app/components/formHelpers/FormMaskHashSaltComponent";

const fieldMock = {};
const metaMock = {};
const helperMock = {};

jest.mock("formik", () => ({
  ...jest.requireActual("formik"),
  useField: jest.fn(() => {
    return [fieldMock, metaMock, helperMock];
  }),
}));

describe("<FormMaskHashSaltComponent />", () => {
  const renderSetup = (props: IFormMaskHashSaltComponentProps) => {
    return render(<FormMaskHashSaltComponent {...props} />);
  };

  it("should render FormMaskHashSaltComponent", () => {
    const propertyChangeMock = jest.fn()
    const setFieldValueMock = jest.fn()

    const props: IFormMaskHashSaltComponentProps = {
      label: "CompLabel",
      description: "Comp Description",
      name: "CompName",
      fieldId: "Comp",
      isRequired: true,
      validated: "default",
      i18nAddDefinitionText: "AddDefnText",
      i18nAddDefinitionTooltip: "AddDefnTooltip",
      i18nRemoveDefinitionTooltip: "RemovedDefnTooltip",
      propertyChange: propertyChangeMock,
      setFieldValue: setFieldValueMock,
    };
  
    renderSetup(props);

    expect(screen.getByText(props.label)).toBeInTheDocument();
  });
});