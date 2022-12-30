import { Services } from '@debezium/ui-services';
import {
  Alert,
  Button,
  Divider,
  EmptyState,
  EmptyStateBody,
  EmptyStateIcon,
  EmptyStateVariant,
  Grid,
  GridItem,
  Modal,
  ModalVariant,
  SelectGroup,
  SelectOption,
  Title,
} from '@patternfly/react-core';
import { CubesIcon, PlusCircleIcon } from '@patternfly/react-icons';
import { PageLoader, TransformCard } from 'components';
import _ from 'lodash';
import React, { FC } from 'react';
import { useTranslation } from 'react-i18next';
import MultiRef from 'react-multi-ref';
import { ApiError, fetch_retry, WithLoader } from 'shared';

export interface ITransformData {
  key: number;
  name?: string;
  type?: string;
  config?: any;
}
export interface ITransformStepProps {
  transformsValues: Map<string, any>;
  updateTransformValues: (data: any) => void;
  setIsTransformDirty: (data: boolean) => void;
  selectedConnectorType: string;
  clusterId: string;
}

const TransformAlert: FC = () => {
  const { t } = useTranslation();
  return (
    <>
      {t('transformAlert')}
      {' See '}
      <a
        href="https://debezium.io/documentation/reference/transformations/index.html"
        target="_blank"
      >
        {t('documentation')}
      </a>{' '}
      {t('moreDetails')}
    </>
  );
};

const getOptions = (response, connectorType) => {
  const TransformData: any[] = [];
  !_.isEmpty(response) &&
    response.forEach((data) => {
      data.transform.includes('io.debezium')
        ? TransformData.unshift(data)
        : TransformData.push(data);
    });
  const dbzTransform: JSX.Element[] = [];
  const apacheTransform: JSX.Element[] = [];
  TransformData.forEach((data, index) => {
    data.transform.includes('io.debezium')
      ? dbzTransform.push(
          <SelectOption key={index} value={`${data.transform}`} />
        )
      : apacheTransform.push(
          <SelectOption key={index} value={`${data.transform}`} />
        );
  });

  return [
    <SelectGroup label="Debezium" key="group1">
      {dbzTransform}
    </SelectGroup>,
    <Divider key="divider" />,
    <SelectGroup label="Apache Kafka" key="group2">
      {apacheTransform}
    </SelectGroup>,
  ];
};

export const TransformsStep: React.FunctionComponent<ITransformStepProps> = (
  props
) => {
  const { t } = useTranslation();

  const [transforms, setTransforms] = React.useState<
    Map<number, ITransformData>
  >(new Map<number, ITransformData>());

  const [isModalOpen, setIsModalOpen] = React.useState<boolean>(false);

  const [responseData, setResponseData] = React.useState({});

  const [loading, setLoading] = React.useState(true);
  const [apiError, setApiError] = React.useState<boolean>(false);
  const [errorMsg, setErrorMsg] = React.useState<Error>(new Error());

  const nameTypeCheckRef = new MultiRef();

  const addTransform = () => {
    const transformsCopy = new Map(transforms);
    transformsCopy.set(transformsCopy.size + 1, {
      key: Math.random() * 10000,
      config: {},
    });
    setTransforms(transformsCopy);
    props.setIsTransformDirty(true);
  };

  const deleteTransformCallback = React.useCallback(
    (order) => {
      const transformsCopy = new Map(transforms);
      transformsCopy.delete(order);
      const transformResult = new Map<number, any>();
      if (transforms.size > 1) {
        for (const [key, value] of transformsCopy.entries()) {
          if (key > order) {
            transformResult.set(+key - 1, value);
          } else if (key < order) {
            transformResult.set(+key, value);
          }
        }
        props.setIsTransformDirty(true);
        setTransforms(transformResult);
      } else {
        setIsModalOpen(true);
      }
    },
    [transforms]
  );

  const clearTransform = () => {
    setTransforms(new Map());
    props.updateTransformValues(new Map());
    props.setIsTransformDirty(false);
    handleModalToggle();
  };

  const moveTransformOrder = React.useCallback(
    (order, position) => {
      const transformsCopy = new Map<number, ITransformData>(transforms);
      switch (position) {
        case 'top':
          transformsCopy.set(1, transforms.get(order)!);
          let i = 1;
          while (i < order) {
            transformsCopy.set(i + 1, transforms.get(i)!);
            i++;
          }
          break;
        case 'up':
          transformsCopy.set(order - 1, transforms.get(order)!);
          transformsCopy.set(order, transforms.get(order - 1)!);
          break;
        case 'down':
          transformsCopy.set(order + 1, transforms.get(order)!);
          transformsCopy.set(order, transforms.get(order + 1)!);
          break;
        case 'bottom':
          transformsCopy.set(transforms.size, transforms.get(order)!);
          let j = transforms.size;
          while (j > order) {
            transformsCopy.set(j - 1, transforms.get(j)!);
            j--;
          }
          break;
        default:
          break;
      }
      setTransforms(transformsCopy);
      props.setIsTransformDirty(true);
    },
    [transforms]
  );

  const getNameList = (): string[] => {
    const nameList: string[] = [];
    transforms.forEach((val) => {
      val.name && nameList.push(val.name);
    });
    return nameList;
  };

  const saveTransforms = () => {
    const cardsValid: any[] = [];
    nameTypeCheckRef.map.forEach((input: any) => {
      cardsValid.push(input.check());
    });

    Promise.all(cardsValid).then(
      (d) => {
        props.setIsTransformDirty(false);
      },
      (e) => {
        props.setIsTransformDirty(true);
      }
    );
  };

  const handleModalToggle = () => {
    setIsModalOpen(!isModalOpen);
  };

  const updateTransformCallback = React.useCallback(
    (key: number, field: string, value: any) => {
      const transformsCopy = new Map(transforms);
      const transformCopy = transforms.get(key);
      if (field === 'name' || field === 'type') {
        transformCopy![field] = value;
        props.setIsTransformDirty(true);
        transformsCopy.set(key, transformCopy!);
      } else {
        transformCopy!.config = value;
        transformsCopy.set(key, transformCopy!);
        saveTransform(transformsCopy);
      }
      setTransforms(transformsCopy);
    },
    [transforms]
  );

  const saveTransform = (data: Map<number, ITransformData>) => {
    const transformValues = new Map();
    data.forEach((val) => {
      if (val.name && val.type) {
        transformValues.has('transforms')
          ? transformValues.set(
              'transforms',
              transformValues.get('transforms') + ',' + val.name
            )
          : transformValues.set('transforms', val.name);
        transformValues.set(`transforms.${val.name}.type`, val.type);
        for (const [key, value] of Object.entries(val.config)) {
          transformValues.set(`transforms.${val.name}.${key}`, value);
        }
      }
    });
    props.updateTransformValues(transformValues);
  };

  React.useEffect(() => {
    if (props.transformsValues.size > 0) {
      const transformsVal = new Map();
      const transformList = props.transformsValues
        .get('transforms')
        ?.split(',');
      transformList.forEach((tName, index) => {
        const transformData: ITransformData = { key: Math.random() * 10000 };
        transformData.name = tName;
        transformData.type = props.transformsValues.get(
          `transforms.${tName}.type`
        );
        transformData.config = {};
        for (const [key, value] of props.transformsValues.entries()) {
          if (key.includes(tName) && !key.includes('type')) {
            const fieldName = key.split(`transforms.${tName}.`)[1];
            transformData.config[fieldName] = value;
          }
        }
        transformsVal.set(index + 1, transformData);
        setTransforms(transformsVal);
      });
    }
    props.setIsTransformDirty(false);
  }, []);

  React.useEffect(() => {
    const connectorService = Services.getConnectorService();
    fetch_retry(connectorService.getTransform, connectorService, [
      props.clusterId,
    ])
      .then((cConnectors: any[]) => {
        setLoading(false);
        setResponseData(cConnectors);
      })
      .catch((err: React.SetStateAction<Error>) => {
        setApiError(true);
        setErrorMsg(err);
      });
  }, []);

  return (
    <WithLoader
      error={apiError}
      loading={loading}
      loaderChildren={<PageLoader />}
      errorChildren={
        <ApiError
          i18nErrorTitle={t('apiErrorTitle')}
          i18nErrorMsg={t('apiErrorMsg')}
          error={errorMsg}
        />
      }
    >
      {() => (
        <div>
          {transforms.size === 0 ? (
            <EmptyState variant={EmptyStateVariant.small}>
              <EmptyStateIcon icon={CubesIcon} />
              <Title headingLevel="h4" size="lg">
                {t('noTransformAdded')}
              </Title>
              <EmptyStateBody>
                <TransformAlert />
              </EmptyStateBody>
              <Button
                variant="secondary"
                className="pf-u-mt-lg"
                icon={<PlusCircleIcon />}
                onClick={addTransform}
              >
                {t('addTransform')}
              </Button>
            </EmptyState>
          ) : (
            <>
              <Alert
                variant="info"
                isInline={true}
                title={
                  <p>
                    <TransformAlert />
                  </p>
                }
              />
              <Grid>
                <GridItem span={9}>
                  {Array.from(transforms.keys()).map((key, index) => {
                    return (
                      <TransformCard
                        key={transforms.get(key)?.key}
                        transformNo={key}
                        ref={nameTypeCheckRef.ref(transforms.get(key)?.key)}
                        transformName={transforms.get(key)?.name || ''}
                        transformType={transforms.get(key)?.type || ''}
                        transformConfig={transforms.get(key)?.config || {}}
                        transformNameList={getNameList()}
                        deleteTransform={deleteTransformCallback}
                        moveTransformOrder={moveTransformOrder}
                        isTop={key === 1}
                        isBottom={key === transforms.size}
                        updateTransform={updateTransformCallback}
                        transformsOptions={getOptions(
                          responseData,
                          props.selectedConnectorType
                        )}
                        transformsData={responseData}
                        setIsTransformDirty={props.setIsTransformDirty}
                      />
                    );
                  })}
                </GridItem>
              </Grid>
              <Button
                variant="secondary"
                className="pf-u-mt-lg pf-u-mr-sm"
                onClick={saveTransforms}
              >
                {t('apply')}
              </Button>
              <Button
                variant="secondary"
                className="pf-u-mt-lg"
                icon={<PlusCircleIcon />}
                onClick={addTransform}
              >
                {t('addTransform')}
              </Button>
            </>
          )}
          <Modal
            variant={ModalVariant.small}
            title={t('deleteTransform')}
            isOpen={isModalOpen}
            onClose={handleModalToggle}
            actions={[
              <Button key="confirm" variant="primary" onClick={clearTransform}>
                {t('confirm')}
              </Button>,
              <Button key="cancel" variant="link" onClick={handleModalToggle}>
                {t('cancel')}
              </Button>,
            ]}
          >
            {t('deleteTransformMsg')}
          </Modal>
        </div>
      )}
    </WithLoader>
  );
};
