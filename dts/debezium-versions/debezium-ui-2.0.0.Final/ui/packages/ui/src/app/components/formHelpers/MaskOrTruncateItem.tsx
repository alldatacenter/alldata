import './MaskOrTruncateItem.css';
import {
  Flex,
  FlexItem,
  Grid,
  GridItem,
  TextInput,
  Tooltip,
} from '@patternfly/react-core';
import { MinusCircleIcon } from '@patternfly/react-icons';
import * as React from 'react';

export interface IMaskOrTruncateItemProps {
  rowId: number;
  columnsValue: string;
  nValue: string;
  canDelete: boolean;
  i18nRemoveDefinitionTooltip: string;
  maskTruncateItemChanged: (rowId: number, maskTruncateValue: string) => void;
  deleteMaskTruncateItem: (rowId: number) => void;
}

export const MaskOrTruncateItem: React.FunctionComponent<
  IMaskOrTruncateItemProps
> = (props) => {
  const handleColumnsChange = (val: any) => {
    handleItemValueChange(val, props.nValue);
  };

  const handleNChange = (val: any) => {
    handleItemValueChange(props.columnsValue, val);
  };

  const handleItemValueChange = (columns: any, n: any) => {
    const newValue = columns + '&&' + n;
    props.maskTruncateItemChanged(props.rowId, newValue);
  };

  const handleRemoveItemClick = () => {
    props.deleteMaskTruncateItem(props.rowId);
  };

  const handleKeyPress = (keyEvent: KeyboardEvent) => {
    // do not allow entry of '.' or '-'
    if (keyEvent.key === '.' || keyEvent.key === '-') {
      keyEvent.preventDefault();
    }
  };

  return (
    <Grid>
      <GridItem span={8}>
        <Flex className={'mask-or-truncate-item-column'}>
          <FlexItem
            className={
              'mask-hash-salt-item-label mask-or-truncate-item-column-input'
            }
          >
            <span>Columns:</span>{' '}
            <TextInput
              id={`${props.rowId}columns`}
              type={'text'}
              onChange={handleColumnsChange}
              value={props.columnsValue}
              onKeyPress={(event) => handleKeyPress(event as any)}
            />
          </FlexItem>
        </Flex>
      </GridItem>
      <GridItem span={3}>
        <Flex>
          <FlexItem className={'mask-hash-salt-item-label'}>
            <span>n:</span>{' '}
            <TextInput
              id={`${props.rowId}n`}
              min={'1'}
              type={'number'}
              onChange={handleNChange}
              value={props.nValue}
              onKeyPress={(event) => handleKeyPress(event as any)}
            />
          </FlexItem>
        </Flex>
      </GridItem>
      {props.canDelete ? (
        <GridItem span={1}>
          <Flex className={'mask-or-truncate-item-remove-button'}>
            <FlexItem>
              <Tooltip
                position="right"
                content={props.i18nRemoveDefinitionTooltip}
              >
                <MinusCircleIcon
                  className={'mask-or-truncate-item-remove-button-icon'}
                  onClick={handleRemoveItemClick}
                />
              </Tooltip>
            </FlexItem>
          </Flex>
        </GridItem>
      ) : null}
    </Grid>
  );
};
