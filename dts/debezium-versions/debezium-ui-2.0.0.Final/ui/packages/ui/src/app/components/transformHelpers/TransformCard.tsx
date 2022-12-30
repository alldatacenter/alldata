import './TransformCard.css';
import {
  Button,
  Dropdown,
  DropdownItem,
  DropdownToggle,
  ExpandableSection,
  Form,
  Grid,
  GridItem,
  Split,
  SplitItem,
  Title,
  Tooltip,
} from '@patternfly/react-core';
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  GripVerticalIcon,
  TrashIcon,
} from '@patternfly/react-icons';
import {
  NameInputField,
  TypeSelectorComponent,
  TransformConfig,
} from 'components';
import _ from 'lodash';
import React from 'react';
import { useTranslation } from 'react-i18next';
import { getFormattedConfig } from 'shared';

export interface ITransformCardProps {
  transformNo: number;
  transformName: string;
  transformType: string;
  transformConfig: any;
  transformNameList: string[];
  isTop: boolean;
  isBottom: boolean;
  deleteTransform: (order: number) => void;
  moveTransformOrder: (order: number, position: string) => void;
  updateTransform: (key: number, field: string, value: any) => void;
  transformsOptions: any;
  transformsData: any;
  setIsTransformDirty: (data: boolean) => void;
}

export interface IConfigValidationRef {
  validate: () => Promise<any>;
}

export const TransformCard = React.forwardRef<any, ITransformCardProps>(
  (props, ref) => {
    const { t } = useTranslation();

    const [isOpen, setIsOpen] = React.useState<boolean>(false);
    const [isExpanded, setIsExpanded] = React.useState<boolean>(true);

    const [nameIsValid, setNameIsValid] = React.useState<boolean>(true);
    const [typeIsThere, setTypeIsThere] = React.useState<boolean>(true);

    const [submitted, setSubmitted] = React.useState<boolean>(false);
    const [configComplete, setConfigComplete] = React.useState<boolean>(false);

    const onToggle = (isExpandedVal: boolean) => {
      setIsExpanded(isExpandedVal);
    };

    const deleteCard = () => {
      props.deleteTransform(props.transformNo);
    };

    // tslint:disable-next-line: no-shadowed-variable
    const onPositionToggle = (isOpenVal) => {
      setIsOpen(isOpenVal);
    };

    const onPositionSelect = (event) => {
      setIsOpen(!isOpen);
      props.moveTransformOrder(props.transformNo, event.currentTarget.id);
      onFocus();
    };

    const onFocus = () => {
      const element = document.getElementById('transform-order-toggle');
      element?.focus();
    };

    const dropdownItems = [
      <DropdownItem
        key="move_top"
        component="button"
        id="top"
        isDisabled={props.isTop}
      >
        {t('moveTop')}
      </DropdownItem>,
      <DropdownItem
        key="move_up"
        component="button"
        id="up"
        isDisabled={props.isTop || (props.isTop && props.isBottom)}
      >
        {t('moveUp')}
      </DropdownItem>,
      <DropdownItem
        key="move_down"
        component="button"
        id="down"
        isDisabled={(props.isTop && props.isBottom) || props.isBottom}
      >
        {t('moveDown')}
      </DropdownItem>,
      <DropdownItem
        key="move_bottom"
        component="button"
        id="bottom"
        isDisabled={props.isBottom}
      >
        {t('moveBottom')}
      </DropdownItem>,
    ];

    const updateNameType = (value: string, field?: string) => {
      if (field) {
        value === '' || props.transformNameList.includes(value)
          ? setNameIsValid(false)
          : setNameIsValid(true);
        props.updateTransform(props.transformNo, 'name', value);
      } else {
        props.updateTransform(props.transformNo, 'type', value);
      }
    };

    const configRef =
      React.useRef() as React.MutableRefObject<IConfigValidationRef>;

    React.useImperativeHandle(ref, () => ({
      check() {
        const validPromise = new Promise((resolve, reject) => {
          if (!props.transformName) {
            setNameIsValid(false);
            setConfigComplete(false);
            reject('fail');
          } else if (nameIsValid && props.transformType) {
            configRef?.current!.validate().then(
              (d) => {
                resolve('done');
              },
              (e) => {
                reject('fail');
              }
            );
          } else if (!props.transformType) {
            setTypeIsThere(false);
            reject('fail');
          }
        });
        setSubmitted(true);
        return validPromise;
      },
    }));

    const isConfigComplete = React.useCallback((val) => {
      setConfigComplete(val);
    }, []);

    React.useEffect(() => {
      props.transformType && setTypeIsThere(true);
    }, [props.transformType]);

    return (
      <Grid>
        <GridItem span={12}>
          <div
            className={'transform-block pf-u-mt-lg pf-u-p-sm pf-u-pb-lg'}
            id="transform-parent"
          >
            <Split>
              <SplitItem className={'pf-u-pr-sm'}>
                <Tooltip content={<div>{t('reorderTransform')}</div>}>
                  <Dropdown
                    className={'position_toggle'}
                    onSelect={onPositionSelect}
                    isOpen={isOpen}
                    isPlain={true}
                    dropdownItems={dropdownItems}
                    toggle={
                      <DropdownToggle
                        toggleIndicator={null}
                        onToggle={onPositionToggle}
                        aria-label="Applications"
                        id="transform-order-toggle"
                      >
                        <GripVerticalIcon />
                      </DropdownToggle>
                    }
                  />
                </Tooltip>
              </SplitItem>
              <SplitItem isFilled={true}>
                <Title headingLevel="h2">
                  Transformation # {props.transformNo} &nbsp;
                  {configComplete && (
                    <CheckCircleIcon style={{ color: '#3E8635' }} />
                  )}
                  {submitted && !configComplete && (
                    <ExclamationCircleIcon style={{ color: '#C9190B' }} />
                  )}
                </Title>
                <Form>
                  <Grid hasGutter={true}>
                    <GridItem span={4}>
                      <NameInputField
                        label="Name"
                        description={t('transformNameDescription')}
                        fieldId="transform_name"
                        isRequired={true}
                        name="transform_name"
                        placeholder="Name"
                        inputType="text"
                        value={props.transformName}
                        setFieldValue={updateNameType}
                        isInvalid={!nameIsValid}
                        invalidText={
                          props.transformName
                            ? t('uniqueName')
                            : t('nameRequired')
                        }
                      />
                    </GridItem>

                    <GridItem span={8}>
                      <TypeSelectorComponent
                        label="Type"
                        description={t('transformTypeDescription')}
                        fieldId="transform_type"
                        isRequired={true}
                        isDisabled={props.transformName === ''}
                        options={props.transformsOptions}
                        value={props.transformType}
                        setFieldValue={updateNameType}
                        isInvalid={!typeIsThere}
                        invalidText={t('typeRequired')}
                      />
                    </GridItem>
                  </Grid>
                </Form>
                {props.transformType && (
                  <ExpandableSection
                    toggleText={isExpanded ? t('hideConfig') : t('showConfig')}
                    onToggle={onToggle}
                    isExpanded={isExpanded}
                  >
                    <TransformConfig
                      ref={configRef}
                      transformConfigOptions={getFormattedConfig(
                        props.transformsData,
                        props.transformType
                      )}
                      transformConfigValues={props.transformConfig}
                      updateTransform={props.updateTransform}
                      transformNo={props.transformNo}
                      setIsTransformDirty={props.setIsTransformDirty}
                      transformType={props.transformType}
                      setConfigComplete={isConfigComplete}
                    />
                  </ExpandableSection>
                )}
              </SplitItem>
              <SplitItem>
                <Tooltip content={<div>{t('deleteTransform')}</div>}>
                  <Button
                    variant="link"
                    icon={<TrashIcon />}
                    onClick={deleteCard}
                    id="tooltip-selector"
                  />
                </Tooltip>
              </SplitItem>
            </Split>
          </div>
        </GridItem>
      </Grid>
    );
  }
);
