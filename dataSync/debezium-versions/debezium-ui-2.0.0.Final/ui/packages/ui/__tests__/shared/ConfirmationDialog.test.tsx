import React from "react";
import { render, screen } from "@testing-library/react";
import {
  IConfirmationDialogProps,
  ConfirmationDialog,
  ConfirmationButtonStyle,
} from "../../src/app/shared/ConfirmationDialog";

describe("<ConfirmationDialog/>", () => {
  const renderSetup = (props: IConfirmationDialogProps) => {
    return render(<ConfirmationDialog {...props} />);
  };

  it("render ConfirmationDialog - showDialog true", () => {
    const onCancelMock = jest.fn()
    const onConfirmMock = jest.fn()

    const props: IConfirmationDialogProps = {
      buttonStyle: ConfirmationButtonStyle.NORMAL,
      i18nCancelButtonText: "Cancel",
      i18nConfirmButtonText: "Confirm",
      i18nConfirmationMessage: "Confirm Message",
      i18nTitle: "Dialog Title",
      onCancel: onCancelMock,
      onConfirm: onConfirmMock,
      showDialog: true,
    };
    renderSetup(props);

    expect(screen.queryByText(props.i18nTitle)).toBeInTheDocument();
  });

  it("render ConfirmationDialog - showDialog false", () => {
    const onCancelMock = jest.fn()
    const onConfirmMock = jest.fn()

    const props: IConfirmationDialogProps = {
      buttonStyle: ConfirmationButtonStyle.NORMAL,
      i18nCancelButtonText: "Cancel",
      i18nConfirmButtonText: "Confirm",
      i18nConfirmationMessage: "Confirm Message",
      i18nTitle: "Dialog Title",
      onCancel: onCancelMock,
      onConfirm: onConfirmMock,
      showDialog: false,
    };
    renderSetup(props);

    expect(screen.queryByTestId('confirmation-dialog')).toBeNull();
  });

});
