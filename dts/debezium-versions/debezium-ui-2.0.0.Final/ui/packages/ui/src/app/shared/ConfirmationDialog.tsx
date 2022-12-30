import {
  Button,
  Modal,
  Split,
  SplitItem,
  Stack,
  StackItem,
  Text,
  Title,
} from '@patternfly/react-core';
import * as React from 'react';

/**
 * Confirmation type enum that maps to patternfly modal type
 */
export enum ConfirmationType {
  DANGER = 'danger',
  WARNING = 'warning',
  INFO = 'info',
  SUCCESS = 'success',
  DEFAULT = 'default',
}

/**
 * Button style enum that maps to patternfly button classes
 */
export enum ConfirmationButtonStyle {
  NORMAL = 'primary',
  SUCCESS = 'success',
  DANGER = 'danger',
  WARNING = 'warning',
  INFO = 'info',
  LINK = 'link',
}

/**
 * A dialog that can be used to obtain user confirmation when deleting an object.
 */
export interface IConfirmationDialogProps {
  /**
   * The style of button to use for the primary action
   */
  buttonStyle: ConfirmationButtonStyle;
  /**
   * The localized cancel button text.
   */
  i18nCancelButtonText: string;

  /**
   * The localized confirmation button text.
   */
  i18nConfirmButtonText: string;

  /**
   * The localized confirmation message.
   */
  i18nConfirmationMessage: string;

  /**
   * An optional localized message providing more details.
   */
  i18nDetailsMessage?: string;

  /**
   * The localized dialog title.
   */
  i18nTitle: string;

  /**
   * The confirmation type, or unset for default
   */
  type?: ConfirmationType;

  /**
   * A callback for when the cancel button is clicked. Caller should hide dialog.
   */
  onCancel: () => void;

  /**
   * A callback for when the confirmation button is clicked. Caller should hide dialog.
   */
  onConfirm: () => void;

  /**
   * Indicates if the dialog should be visible.
   */
  showDialog: boolean;
}

/**
 * A modal dialog to display for confirmation actions.
 */
export const ConfirmationDialog: React.FunctionComponent<
  IConfirmationDialogProps
> = ({
  buttonStyle,
  i18nCancelButtonText,
  i18nConfirmButtonText,
  i18nConfirmationMessage,
  i18nDetailsMessage,
  i18nTitle,
  type,
  onCancel,
  onConfirm,
  showDialog,
}) => {
  let buttonStyleMapped:
    | 'primary'
    | 'secondary'
    | 'tertiary'
    | 'danger'
    | 'link'
    | 'plain'
    | 'control' = 'primary';
  switch (buttonStyle) {
    case 'danger':
      buttonStyleMapped = 'danger';
      break;
    case 'info':
      buttonStyleMapped = 'secondary';
      break;
    case 'link':
      buttonStyleMapped = 'link';
      break;
    default:
  }
  return (
    <Modal
      title={i18nTitle}
      data-testid="confirmation-dialog"
      aria-label={`${i18nTitle} confirmation`}
      titleIconVariant={type}
      isOpen={showDialog}
      onClose={onCancel}
      actions={[
        <Button key="confirm" variant={buttonStyleMapped} onClick={onConfirm}>
          {i18nConfirmButtonText}
        </Button>,
        <Button key="cancel" variant="link" onClick={onCancel}>
          {i18nCancelButtonText}
        </Button>,
      ]}
      width={'50%'}
    >
      <Stack hasGutter={true}>
        <StackItem>
          <Split hasGutter={true}>
            <SplitItem isFilled={true}>
              <Title headingLevel="h4" size={'lg'}>
                {i18nConfirmationMessage}
              </Title>
            </SplitItem>
          </Split>
        </StackItem>
        {i18nDetailsMessage && (
          <StackItem>
            <Text>{i18nDetailsMessage}</Text>
          </StackItem>
        )}
      </Stack>
    </Modal>
  );
};
