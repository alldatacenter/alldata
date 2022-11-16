import React from "react";
import { render } from "@testing-library/react";
import {
  IConnectorStatusProps,
  ConnectorStatus,
} from "../../../src/app/pages/connectors/ConnectorStatus";
import { ConnectorState } from "../../../src/app/shared";

describe("<ConnectorStatus />", () => {
  const renderSetup = (props: IConnectorStatusProps) => {
    return render(<ConnectorStatus {...props} />);
  };

  it("should render ConnectorStatus", () => {
    const props: IConnectorStatusProps = {
      currentStatus: ConnectorState.RUNNING
    };

    const { getByTestId } = renderSetup(props);
    const label = getByTestId("connector-status-label");
    expect(label).toBeInTheDocument();
  });
});