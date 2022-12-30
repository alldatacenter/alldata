import './MaskHashSaltItem.css';
import {
  Flex,
  FlexItem,
  Grid,
  GridItem,
  Select,
  SelectOption,
  SelectVariant,
  TextInput,
  Tooltip,
} from '@patternfly/react-core';
import { MinusCircleIcon } from '@patternfly/react-icons';
import * as React from 'react';

export interface IMaskHashSaltItemProps {
  rowId: number;
  columnsValue: string;
  hashValue: string;
  saltValue: string;
  canDelete: boolean;
  i18nRemoveDefinitionTooltip: string;
  maskHashSaltItemChanged: (rowId: number, maskHashSaltValue: string) => void;
  deleteMaskHashSaltItem: (rowId: number) => void;
}

export const MaskHashSaltItem: React.FunctionComponent<
  IMaskHashSaltItemProps
> = (props) => {
  const [isOpen, setOpen] = React.useState<boolean>(false);

  const onToggle = (open: boolean) => {
    setOpen(open);
  };

  const selectOptions = [
    { value: 'Choose...', isPlaceholder: true },
    { value: 'MD2' },
    { value: 'MD5' },
    { value: 'SHA-1' },
    { value: 'SHA-256' },
    { value: 'SHA-384' },
    { value: 'SHA-512' },
  ];

  const handleColumnsChange = (val: any) => {
    handleItemValueChange(val, props.hashValue, props.saltValue);
  };

  const handleHashChange = (event: any, selection: any, isPlaceholder: any) => {
    const hashVal = isPlaceholder ? '' : selection;
    setOpen(false);
    handleItemValueChange(props.columnsValue, hashVal, props.saltValue);
  };

  const handleSaltChange = (val: any) => {
    handleItemValueChange(props.columnsValue, props.hashValue, val);
  };

  const handleItemValueChange = (columns: any, hash: any, salt: any) => {
    const newValue = columns + '&&' + hash + '||' + salt;
    props.maskHashSaltItemChanged(props.rowId, newValue);
  };

  const handleRemoveItemClick = () => {
    props.deleteMaskHashSaltItem(props.rowId);
  };

  const handleKeyPress = (keyEvent: KeyboardEvent) => {
    // do not allow entry of '.' or '-'
    if (keyEvent.key === '.' || keyEvent.key === '-') {
      keyEvent.preventDefault();
    }
  };

  return (
    <Grid>
      <GridItem span={5}>
        <Flex className={'mask-hash-salt-item-column'}>
          <FlexItem
            className={
              'mask-hash-salt-item-label mask-hash-salt-item-column-input'
            }
          >
            <span>Columns:</span>{' '}
            <TextInput
              data-testid={`${props.rowId}columns`}
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
          <FlexItem
            spacer={{ default: 'spacerXs' }}
            className="mask-hash-salt-item-label"
          >
            <span>Hash:</span>{' '}
            <Select
              variant={SelectVariant.single}
              aria-label="Select Input"
              onToggle={onToggle}
              onSelect={handleHashChange}
              selections={props.hashValue}
              isOpen={isOpen}
            >
              {selectOptions.map((option, index) => (
                <SelectOption
                  key={index}
                  value={option.value}
                  isPlaceholder={option.isPlaceholder}
                />
              ))}
            </Select>
          </FlexItem>
        </Flex>
      </GridItem>
      <GridItem span={3}>
        <Flex className="pf-l-flex-nowrap">
          <FlexItem
            spacer={{ default: 'spacerXs' }}
            className="mask-hash-salt-item-label"
          >
            <span>Salt:</span>{' '}
            <TextInput
              data-testid={`${props.rowId}salt`}
              id={`${props.rowId}salt`}
              type={'text'}
              onChange={handleSaltChange}
              value={props.saltValue}
              onKeyPress={(event) => handleKeyPress(event as any)}
            />
          </FlexItem>
        </Flex>
      </GridItem>
      {props.canDelete ? (
        <GridItem span={1}>
          <Flex className={'mask-hash-salt-item-remove-button'}>
            <FlexItem>
              <Tooltip
                position="right"
                content={props.i18nRemoveDefinitionTooltip}
              >
                <MinusCircleIcon
                  className={'mask-hash-salt-item-remove-button-icon'}
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
