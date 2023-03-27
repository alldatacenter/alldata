import './CustomPropertiesStep.css';
import { ConnectionValidationResult } from '@debezium/ui-models';
import { Services } from '@debezium/ui-services';
import {
  Alert,
  Bullseye,
  Button,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  EmptyState,
  EmptyStateBody,
  EmptyStateIcon,
  EmptyStateVariant,
  Grid,
  GridItem,
  Label,
  LabelGroup,
  Level,
  LevelItem,
  Modal,
  ModalVariant,
  TextInput,
  Title,
  Tooltip,
} from '@patternfly/react-core';
import {
  CircleIcon,
  CubesIcon,
  MinusCircleIcon,
  PlusCircleIcon,
} from '@patternfly/react-icons';
import _ from 'lodash';
import React, { FC } from 'react';
import { useTranslation } from 'react-i18next';
import { fetch_retry, mapToObject } from 'shared';

export interface ICustomProperties {
  key: string;
  value: string;
}

export interface ICustomPropertiesStepProps {
  basicProperties: Map<string, string>;
  customProperties: { [key: string]: string };
  updateCustomPropertiesValues: (data: any) => void;
  setIsCustomPropertiesDirty: (data: boolean) => void;
  isCustomPropertiesDirty: boolean;
  selectedConnectorType: string;
  clusterId: string;
  propertyValues: Map<string, string>;
}

const TransformAlert: FC = () => {
  const { t } = useTranslation();
  return <>{t('customPropertiesDescription')}</>;
};

export const CustomPropertiesStep: React.FunctionComponent<
  ICustomPropertiesStepProps
> = (props) => {
  const { t } = useTranslation();

  const [properties, setProperties] = React.useState<
    Map<string, ICustomProperties>
  >(new Map<string, ICustomProperties>());

  const [validationInProgress, setValidationInProgress] = React.useState(false);
  const [validationResult, setValidationResult] = React.useState<boolean>(true);

  const [isModalOpen, setIsModalOpen] = React.useState<boolean>(false);

  const addProperty = () => {
    const propertiesCopy = new Map(properties);
    propertiesCopy.set('' + Math.floor(Math.random() * 100000), {
      key: '',
      value: '',
    });
    setProperties(propertiesCopy);
    props.setIsCustomPropertiesDirty(true);
  };

  const deleteProperty = (order) => {
    const propertiesCopy = new Map(properties);
    propertiesCopy.delete(order);
    if (propertiesCopy.size === 0) {
      handleModalToggle();
    } else {
      props.setIsCustomPropertiesDirty(true);
      setProperties(propertiesCopy);
    }
  };

  React.useEffect(() => {
    if (!_.isEmpty(props.customProperties)) {
      const propertiesCopy = new Map();
      Object.keys(props.customProperties).forEach((key) => {
        propertiesCopy.set('' + Math.floor(Math.random() * 100000), {
          key,
          value: props.customProperties[key],
        });
      });
      setProperties(propertiesCopy);
    }
  }, []);

  const handleModalToggle = () => {
    setIsModalOpen(!isModalOpen);
  };

  const updateProperties = (key: string, field: string, value: any) => {
    const propertiesCopy = new Map(properties);
    const propertyCopy = properties.get(key);

    propertyCopy![field] = value;
    props.setIsCustomPropertiesDirty(true);
    propertiesCopy.set(key, propertyCopy!);

    setProperties(propertiesCopy);
  };

  const saveCustomProperties = () => {
    const customPropertiesValue = {};
    properties.forEach((val) => {
      if (val.key && val.value) {
        customPropertiesValue[val.key] = val.value;
      }
    });
    props.setIsCustomPropertiesDirty(false);
    props.updateCustomPropertiesValues(customPropertiesValue);
  };

  /**
   * Validate the Custom Properties set
   * @returns
   */
  const validateCustomProperties = () => {
    setValidationInProgress(true);
    const customPropertiesValue = {};
    properties.forEach((val) => {
      if (val.key && val.value) {
        customPropertiesValue[val.key] = val.value;
      }
    });

    const connectorService = Services.getConnectorService();
    fetch_retry(connectorService.validateConnection, connectorService, [
      props.selectedConnectorType,
      {
        ...mapToObject(new Map(props.basicProperties)),
        ...customPropertiesValue,
      },
    ])
      .then((result: ConnectionValidationResult) => {
        if (result.status === 'INVALID') {
          setValidationResult(false);
        } else {
          setValidationResult(true);
          saveCustomProperties();
        }
        setValidationInProgress(false);
      })
      .catch((err: React.SetStateAction<Error>) => {
        setValidationInProgress(false);
        alert('Error Validation Connection Properties !: ' + err);
      });
  };

  const clearProperties = () => {
    setProperties(new Map());
    props.updateCustomPropertiesValues({});
    props.setIsCustomPropertiesDirty(false);
    handleModalToggle();
  };

  const handlePropertyChange = (
    value: string,
    event: React.FormEvent<HTMLInputElement>
  ) => {
    event.currentTarget.id.includes('key')
      ? updateProperties(event.currentTarget.id.split('_')[1], 'key', value)
      : updateProperties(event.currentTarget.id.split('_')[1], 'value', value);
  };

  return (
    <div>
      {properties.size === 0 ? (
        <EmptyState variant={EmptyStateVariant.small}>
          <EmptyStateIcon icon={CubesIcon} />
          <Title headingLevel="h4" size="lg">
            {t('customProperties')}
          </Title>
          <EmptyStateBody>
            <TransformAlert />
          </EmptyStateBody>
          <Button
            variant="secondary"
            className="pf-u-mt-lg"
            icon={<PlusCircleIcon />}
            onClick={addProperty}
          >
            {t('configureCustomProperties')}
          </Button>
        </EmptyState>
      ) : (
        <>
          {!validationResult && (
            <Alert
              variant="danger"
              isInline={true}
              title={<p>{t('incorrectCustomProperties')}</p>}
            />
          )}
          <Grid style={{ marginTop: '10px' }} hasGutter={true}>
            <GridItem span={8}>
              <>
                {Array.from(properties.keys()).map((key, index) => {
                  return (
                    <Grid key={key} style={{ marginTop: '10px' }}>
                      <GridItem span={5}>
                        <TextInput
                          value={properties.get(key)!.key}
                          type="text"
                          placeholder="Key"
                          id={'key_' + key}
                          onChange={handlePropertyChange}
                          aria-label="text input example"
                        />
                      </GridItem>
                      <GridItem span={1}>
                        <Bullseye>&nbsp; : &nbsp;</Bullseye>
                      </GridItem>

                      <GridItem span={5} style={{ fontSize: 'x-large' }}>
                        <TextInput
                          value={properties.get(key)!.value}
                          type="text"
                          placeholder="Value"
                          id={'value_' + key}
                          onChange={handlePropertyChange}
                          aria-label="text input example"
                        />
                      </GridItem>
                      <GridItem
                        span={1}
                        style={{ paddingLeft: '15px', fontSize: 'x-large' }}
                      >
                        <Level style={{ flexWrap: 'unset' }}>
                          <LevelItem
                            style={
                              !props.isCustomPropertiesDirty
                                ? {}
                                : { display: 'none' }
                            }
                          >
                            <Tooltip
                              position="top"
                              content={
                                props.customProperties.hasOwnProperty(
                                  properties.get(key)!.key
                                )
                                  ? 'Saved'
                                  : 'Either key or value is empty'
                              }
                            >
                              <Alert
                                variant={
                                  props.customProperties.hasOwnProperty(
                                    properties.get(key)!.key
                                  )
                                    ? 'success'
                                    : 'warning'
                                }
                                isInline={true}
                                isPlain={true}
                                title=""
                              />
                            </Tooltip>
                          </LevelItem>
                          <LevelItem>
                            <Button
                              variant="link"
                              icon={<MinusCircleIcon />}
                              onClick={() => deleteProperty(key)}
                            />
                          </LevelItem>
                        </Level>
                      </GridItem>
                    </Grid>
                  );
                })}
                <Button
                  variant="secondary"
                  className="pf-u-mt-lg pf-u-mr-sm"
                  isLoading={validationInProgress}
                  onClick={validateCustomProperties}
                >
                  {t('apply')}
                </Button>
                <Button
                  variant="link"
                  className="pf-u-mt-lg pf-u-mr-sm"
                  icon={<PlusCircleIcon />}
                  onClick={addProperty}
                >
                  {t('addCustomProperties')}
                </Button>
              </>
            </GridItem>
            <GridItem span={4} className="properties_section">
              <LabelGroup style={{ marginBottom: '15px' }}>
                <Label icon={<CircleIcon />}>
                  {t('alreadyConfiguredProperties')}
                </Label>
                <Label icon={<CircleIcon />} color="blue">
                  {t('customProperties')}
                </Label>
              </LabelGroup>
              <DescriptionList
                isCompact={true}
                isFluid={true}
                isHorizontal={true}
                style={{ gap: '0' }}
              >
                <>
                  {Array.from(props.propertyValues.keys()).map((key, index) => {
                    return (
                      <DescriptionListGroup
                        key={key + index}
                        style={
                          props.customProperties.hasOwnProperty(key)
                            ? { color: '#0066CC' }
                            : {}
                        }
                      >
                        <DescriptionListTerm>{key}</DescriptionListTerm>
                        <DescriptionListDescription>
                          {props.propertyValues.get(key)}
                        </DescriptionListDescription>
                      </DescriptionListGroup>
                    );
                  })}
                </>
              </DescriptionList>
            </GridItem>
          </Grid>
        </>
      )}
      <Modal
        variant={ModalVariant.small}
        title={'Remove custom property'}
        isOpen={isModalOpen}
        onClose={handleModalToggle}
        actions={[
          <Button key="confirm" variant="primary" onClick={clearProperties}>
            {t('confirm')}
          </Button>,
          <Button key="cancel" variant="link" onClick={handleModalToggle}>
            {t('cancel')}
          </Button>,
        ]}
      >
        {t('removeCustomPropertiesMsg')}
      </Modal>
    </div>
  );
};
