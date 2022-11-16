import React from "react";
import { render, screen } from "@testing-library/react";
import {
  IConnectorTypeComponentProps,
  ConnectorTypeComponent,
} from "../../src/app/components/ConnectorTypeComponent";

describe("<ConnectorTypeComponent/>", () => {
  const renderSetup = (props: IConnectorTypeComponentProps) => {
    return render(<ConnectorTypeComponent {...props} />);
  };

  it("should render default ConnectorTypeComponent ", () => {
    const props: IConnectorTypeComponentProps = {
      showIcon: true,
    };
    const { container } = renderSetup(props);

    expect(container.getElementsByClassName("connector-type-icon").length).toBe(
      0
    );
  });

  it("should render ConnectorTypeComponent for mysql", () => {
    const props: IConnectorTypeComponentProps = {
      connectorType: "mysql",
      showIcon: true,
    };
    const { container } = renderSetup(props);
    screen.getByText("MySQL");
    expect(container.getElementsByClassName("connector-type-icon").length).toBe(
      1
    );
  });

  it("should render ConnectorTypeComponent for mongodb", () => {
    const props: IConnectorTypeComponentProps = {
      connectorType: "mongodb",
      showIcon: true,
    };
    const { container } = renderSetup(props);

    expect(container.getElementsByClassName("connector-type-icon").length).toBe(
      1
    );
  });

  it("should render ConnectorTypeComponent for postgres", () => {
    const props: IConnectorTypeComponentProps = {
      connectorType: "postgres",
      showIcon: true,
    };
    const { container } = renderSetup(props);

    expect(container.getElementsByClassName("connector-type-icon").length).toBe(
      1
    );
  });

  it("should render ConnectorTypeComponent for sqlserver", () => {
    const props: IConnectorTypeComponentProps = {
      connectorType: "sqlserver",
      showIcon: true,
    };
    const { container } = renderSetup(props);

    expect(container.getElementsByClassName("connector-type-icon").length).toBe(
      1
    );
  });
});
