import React from "react";
import { render } from "@testing-library/react";
import {
  IConnectorTaskStateProps,
  ConnectorTaskState,
} from "../../../src/app/pages/connectors/ConnectorTaskState";
import { Connector } from "@debezium/ui-models";

describe("<ConnectorTaskState />", () => {
  const renderSetup = (props: IConnectorTaskStateProps) => {
    return render(<ConnectorTaskState {...props} />);
  };

  it("should render ConnectorTaskState", () => {
    const myConn = {
      connectorStatus: 'RUNNING',
      connectorType: 'postgres',
      name: 'myConn',
      taskStates: []
    } as Connector;

    const props: IConnectorTaskStateProps = {
      connector: myConn
    };
    const { container } = renderSetup(props);

    expect(container.getElementsByClassName("taskStates").length).toBe(
      1
    );
  });
});