import React from "react";
import { render } from "@testing-library/react";
import {
  IConnectorIconProps,
  ConnectorIcon,
} from "../../src/app/components/ConnectorIcon";

describe("<ConnectorIcon />", () => {
  const renderSetup = (props: IConnectorIconProps) => {
    return render(<ConnectorIcon {...props} />);
  };

  it("should render default ConnectorIcon ", () => {
    const props: IConnectorIconProps = {
      connectorType: "postgres",
      alt: "postgres",
      className: "my-test",
    };
    const { container } = renderSetup(props);

    expect(container.getElementsByClassName("my-test").length).toBe(
      1
    );
  });
});