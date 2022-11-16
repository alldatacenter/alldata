import React from "react";
import { render, screen } from "@testing-library/react";
import {
  IConnectorcOverviewProps,
  ConnectorOverview,
} from "../../../src/app/pages/connectors/ConnectorOverview";

describe("<ConnectorOverview />", () => {
  const renderSetup = (props: IConnectorcOverviewProps) => {
    return render(<ConnectorOverview {...props} />);
  };

  it("should render ConnectorOverview", () => {
    const props: IConnectorcOverviewProps = {
      i18nOverview: "Overview",
      i18nMessagePerSec: "MessagePerSec",
      i18nMaxLagInLastMin: "MaxLagInLastMin",
      i18nPercentiles: "Percentiles",
    };

    renderSetup(props);

    expect(screen.getByText(props.i18nOverview)).toBeInTheDocument();
  });
});