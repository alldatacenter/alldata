import React from "react";
import { render, screen } from "@testing-library/react";
import {
  IFormMaskOrTruncateComponentProps,
  FormMaskOrTruncateComponent,
} from "../../../src/app/components/formHelpers/FormMaskOrTruncateComponent";

const fieldMock = {};
const metaMock = {};
const helperMock = {};

jest.mock("formik", () => ({
  ...jest.requireActual("formik"),
  useField: jest.fn(() => {
    return [fieldMock, metaMock, helperMock];
  }),
}));

describe("<FormMaskOrTruncateComponent />", () => {
  const renderSetup = (props: IFormMaskOrTruncateComponentProps) => {
    return render(<FormMaskOrTruncateComponent {...props} />);
  };

  it("should render FormMaskOrTruncateComponent", () => {
    const propertyChangeMock = jest.fn()
    const setFieldValueMock = jest.fn()

    const props: IFormMaskOrTruncateComponentProps = {
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