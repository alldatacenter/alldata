import React from "react";
import { render, screen } from "@testing-library/react";
import { IToastAlertComponentProps, ToastAlertComponent } from "../../src/app/components/ToastAlertComponent";

describe("<ToastAlertComponent/>", () => {

  const renderSetup = (props: IToastAlertComponentProps) => {
    return render(<ToastAlertComponent {...props} />);
  };

  it("should render ToastAlertComponent, empty alerts", () => {
    const removeAlertMock = jest.fn()

    const props: IToastAlertComponentProps = {
      alerts: [],
      removeAlert: removeAlertMock,
      i18nDetails: "Details"
    };
    renderSetup(props);

    expect(screen.queryByText(props.i18nDetails)).not.toBeInTheDocument();
  });
});
