import React from "react";
import { render } from "@testing-library/react";
import {
  IConnectorNameTypeHeaderProps,
  ConnectorNameTypeHeader,
} from "../../../src/app/components/connectorStepHelpers/ConnectorNameTypeHeader";

describe("<ConnectorNameTypeHeader />", () => {
  const renderSetup = (props: IConnectorNameTypeHeaderProps) => {
    return render(<ConnectorNameTypeHeader {...props} />);
  };

  it("should render ConnectorNameTypeHeader", () => {
    const props: IConnectorNameTypeHeaderProps = {
      connectorName: "connectorName",
      connectorType: "postgres",
      showIcon: true
    };
    const { container } = renderSetup(props);

    expect(container.getElementsByClassName("connector-name-type-header_divider").length).toBe(
      1
    );
  });
});