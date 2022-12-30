import React from "react";
import { render } from "@testing-library/react";
import {
  IKafkaConnectCluster,
  KafkaConnectCluster,
} from "../../src/app/components/KafkaConnectCluster";

describe("<KafkaConnectCluster/>", () => {
  const renderSetup = (props: IKafkaConnectCluster) => {
    return render(<KafkaConnectCluster {...props} />);
  };

  it("should render KafkaConnectCluster ", () => {
    const handleChangeMock = jest.fn()

    const props: IKafkaConnectCluster = {
      handleChange: handleChangeMock,
    };
    const { container } = renderSetup(props);

    expect(container.getElementsByClassName("kafka-connect__cluster").length).toBe(
      1
    );
  });
});
