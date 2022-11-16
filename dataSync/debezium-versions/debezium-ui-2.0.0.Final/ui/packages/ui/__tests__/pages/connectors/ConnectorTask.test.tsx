import React from "react";
import { render } from "@testing-library/react";
import {
  IConnectorTaskProps,
  ConnectorTask,
} from "../../../src/app/pages/connectors/ConnectorTask";

describe("<ConnectorTask />", () => {
  const renderSetup = (props: IConnectorTaskProps) => {
    return render(<ConnectorTask {...props} />);
  };

  it("should render ConnectorTask", () => {
    const connectorTaskToRestartMock = jest.fn()
    const showConnectorTaskToRestartMock = jest.fn()

    const props: IConnectorTaskProps = {
      status: "RUNNING",
      connName: "MyConn",
      taskId: "0",
      i18nTask: "Task",
      i18nRestart: "Restart",
      i18nTaskStatusDetail: "TaskStatusDetail",
      i18nTaskErrorTitle: "TaskErrorTitle",
      i18nMoreInformation: "MoreInformation",
      connectorTaskToRestart: connectorTaskToRestartMock,
      showConnectorTaskToRestartDialog: showConnectorTaskToRestartMock
    };

    const { getByTestId } = renderSetup(props);
    const label = getByTestId("connector-status-id-div");
    expect(label).toBeInTheDocument();
  });
});
