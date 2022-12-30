import React from "react";
import { render, screen } from "@testing-library/react";
import {
  INoPreviewFilterFieldProps,
  NoPreviewFilterField,
} from "../../../src/app/components/connectorStepHelpers/NoPreviewFilterField";

describe("<NoPreviewFilterField />", () => {
  const renderSetup = (props: INoPreviewFilterFieldProps) => {
    return render(<NoPreviewFilterField {...props} />);
  };

  it("should render NoPreviewFilterField", () => {
    const props: INoPreviewFilterFieldProps = {
      i18nShowFilter: "showFilter",
      i18nHideFilter: "hideFilter"
    };
    renderSetup(props);

    expect(screen.getByText(props.i18nShowFilter)).toBeInTheDocument();
  });
});