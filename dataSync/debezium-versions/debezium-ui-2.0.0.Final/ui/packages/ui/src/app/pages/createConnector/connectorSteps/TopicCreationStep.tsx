import { IValidationRef } from '..';
import topicCreationResponse from '../../../../../assets/mockResponse/topicCreation.json';
import './TopicCreationStep.css';
import {
  Alert,
  Button,
  Grid,
  GridItem,
  Text,
  TextVariants,
  Title,
  TitleSizes,
} from '@patternfly/react-core';
import { PlusCircleIcon } from '@patternfly/react-icons';
import { TopicDefaults, TopicGroupCard } from 'components';
import React, { FC } from 'react';
import { useTranslation } from 'react-i18next';
import MultiRef from 'react-multi-ref';
import { getFormattedTopicCreationProperties } from 'shared';

export interface ITopicGroupData {
  key: number;
  name?: string;
  config?: any;
}

export interface ITopicCreationStepProps {
  topicCreationEnabled: boolean;
  topicCreationValues: Map<string, any>;
  updateTopicCreationValues: (data: any) => void;
  setIsTopicCreationDirty: (data: boolean) => void;
  isTopicCreationDirty: boolean;
}

const TopicCreationDisabledAlert: FC = () => {
  const { t } = useTranslation();
  return (
    <>
      {t('topicCreationDisabledAlert')}
      &nbsp;&nbsp;{t('topicCreationSee')}&nbsp;
      <a
        href="https://debezium.io/documentation/reference/configuration/topic-auto-create-config.html"
        target="_blank"
      >
        {t('topicCreationDocumentation')}
      </a>
      &nbsp;{t('topicCreationMoreDetailsToEnable')}
    </>
  );
};

export const TopicCreationStep: React.FunctionComponent<
  ITopicCreationStepProps
> = (props) => {
  const { t } = useTranslation();
  const [topicGroups, setTopicGroups] = React.useState<
    Map<number, ITopicGroupData>
  >(new Map<number, ITopicGroupData>());
  const [topicDefaults, setTopicDefaults] = React.useState({});

  const groupNameCheckRef = new MultiRef();
  const topicDefaultsSaveRef =
    React.useRef() as React.MutableRefObject<IValidationRef>;

  const TOPIC_CREATION_GROUPS = 'topic_creation_groups';

  const addTopicGroup = () => {
    const topicGroupsCopy = new Map(topicGroups);
    topicGroupsCopy.set(topicGroupsCopy.size + 1, {
      key: Math.random() * 10000,
      config: {},
    });
    setTopicGroups(topicGroupsCopy);
    props.setIsTopicCreationDirty(true);
  };

  const deleteTopicGroupCallback = React.useCallback(
    (order) => {
      const topicGroupsCopy = new Map(topicGroups);
      topicGroupsCopy.delete(order);
      const topicGroupsResult = new Map<number, any>();
      for (const [key, value] of topicGroupsCopy.entries()) {
        if (key > order) {
          topicGroupsResult.set(+key - 1, value);
        } else if (key < order) {
          topicGroupsResult.set(+key, value);
        }
      }
      setTopicGroups(topicGroupsResult);
      props.setIsTopicCreationDirty(true);
    },
    [topicGroups]
  );

  const getNameList = (): string[] => {
    const nameList: string[] = [];
    topicGroups.forEach((val) => {
      val.name && nameList.push(val.name);
    });
    return nameList;
  };

  const saveTopicCreation = () => {
    const cardsValid: any[] = [];
    groupNameCheckRef.map.forEach((input: any) => {
      cardsValid.push(input.check());
    });

    Promise.all(cardsValid).then(
      (d) => {
        props.setIsTopicCreationDirty(false);
      },
      (e) => {
        props.setIsTopicCreationDirty(true);
      }
    );
    topicDefaultsSaveRef?.current?.validate();
  };

  const updateTopicGroupCallback = React.useCallback(
    (key: number, field: string, value: any) => {
      const topicGroupsCopy = new Map(topicGroups);
      const topicGroupCopy = topicGroups.get(key);
      if (field === 'name') {
        topicGroupCopy![field] = value;
        props.setIsTopicCreationDirty(true);
      } else {
        topicGroupCopy!.config = value;
      }
      topicGroupsCopy.set(key, topicGroupCopy!);
      setTopicGroups(topicGroupsCopy);
    },
    [topicGroups]
  );

  const updateTopicDefaultsCallback = React.useCallback(
    (value: any) => {
      setTopicDefaults(value);
    },
    [topicDefaults]
  );

  React.useEffect(() => {
    const topicCreateValues = new Map();
    if (topicGroups.size > 0) {
      topicGroups.forEach((val) => {
        if (val.name) {
          topicCreateValues.has(TOPIC_CREATION_GROUPS)
            ? topicCreateValues.set(
                TOPIC_CREATION_GROUPS,
                topicCreateValues.get(TOPIC_CREATION_GROUPS) + ',' + val.name
              )
            : topicCreateValues.set(TOPIC_CREATION_GROUPS, val.name);
          for (const [key, value] of Object.entries(val.config)) {
            topicCreateValues.set(`topic_creation_${val.name}_${key}`, value);
          }
        }
      });
      topicCreateValues.set('topic_creation_default_partitions', -1);
      topicCreateValues.set('topic_creation_default_replication_factor', -1);
    }
    if (Object.keys(topicDefaults).length > 0) {
      for (const [key, value] of Object.entries(topicDefaults)) {
        topicCreateValues.set(`topic_creation_${key}`, value);
      }
    }
    props.updateTopicCreationValues(topicCreateValues);
  }, [topicGroups, topicDefaults]);

  React.useEffect(() => {
    if (props.topicCreationValues.size > 0) {
      // Set the topic creation groups
      const topicGroupsVal = new Map();
      const topicGroupList = props.topicCreationValues
        .get(TOPIC_CREATION_GROUPS)
        ?.split(',');
      if (topicGroupList) {
        topicGroupList.forEach((tName, index) => {
          const topicGroupData: ITopicGroupData = {
            key: Math.random() * 10000,
          };
          topicGroupData.name = tName;
          topicGroupData.config = {};
          for (const [key, value] of props.topicCreationValues.entries()) {
            if (key.includes(`_${tName}_`)) {
              const fieldName = key.split(`topic_creation_${tName}_`)[1];
              topicGroupData.config[fieldName] = value;
            }
          }
          topicGroupsVal.set(index + 1, topicGroupData);
          setTopicGroups(topicGroupsVal);
        });
      }
      // Set the topic creation default properties
      const topicDefaultsVal = {};
      for (const [key, value] of props.topicCreationValues.entries()) {
        if (key.startsWith('topic_creation_default_')) {
          const fieldName = key.split('topic_creation_')[1];
          topicDefaultsVal[fieldName] = value;
        }
      }
      setTopicDefaults(topicDefaultsVal);
    }
    props.setIsTopicCreationDirty(false);
  }, []);

  return (
    // tslint:disable: no-string-literal
    <div>
      {!props.topicCreationEnabled ? (
        <Alert
          variant="info"
          isInline={true}
          className={'topic-creation-step-alert'}
          title={
            <p>
              <TopicCreationDisabledAlert />
            </p>
          }
        />
      ) : (
        <>
          <Text component={TextVariants.h2}>
            {t('topicCreationPageHeadingText')}
          </Text>
          <Grid>
            <GridItem span={10}>
              <Title
                className={'topic-creation-step-defaults-title'}
                headingLevel="h5"
                size={TitleSizes.lg}
              >
                {t('topicCreationDefaultsTitle')}
              </Title>
              <TopicDefaults
                ref={topicDefaultsSaveRef}
                topicDefaultProperties={getFormattedTopicCreationProperties(
                  topicCreationResponse['defaults']
                )}
                topicDefaultValues={topicDefaults}
                updateTopicDefaults={updateTopicDefaultsCallback}
                setIsTopicCreationDirty={props.setIsTopicCreationDirty}
              />
            </GridItem>
            <GridItem span={8}>
              <Title
                className={'topic-creation-step-topic-groups-title'}
                headingLevel="h5"
                size={TitleSizes.lg}
              >
                {t('topicGroupsTitle')}
              </Title>
              {topicGroups.size === 0 ? (
                <Text component={TextVariants.h2}>
                  {t('topicGroupsNoneDefinedText')}
                </Text>
              ) : (
                Array.from(topicGroups.keys()).map((key, index) => {
                  return (
                    <TopicGroupCard
                      key={topicGroups.get(key)?.key}
                      topicGroupNo={key}
                      ref={groupNameCheckRef.ref(topicGroups.get(key)?.key)}
                      topicGroupName={topicGroups.get(key)?.name || ''}
                      topicGroupNameList={getNameList()}
                      topicGroupConfig={topicGroups.get(key)?.config || {}}
                      deleteTopicGroup={deleteTopicGroupCallback}
                      updateTopicGroup={updateTopicGroupCallback}
                      topicGroupsData={
                        topicCreationResponse['groups']['properties']
                      }
                      topicGroupOptionsData={
                        topicCreationResponse['groups']['options']
                      }
                      setIsTopicCreationDirty={props.setIsTopicCreationDirty}
                    />
                  );
                })
              )}
            </GridItem>
          </Grid>
          <Button
            variant="secondary"
            isDisabled={!props.isTopicCreationDirty}
            className="pf-u-mt-lg pf-u-mr-sm"
            onClick={saveTopicCreation}
          >
            {t('apply')}
          </Button>
          <Button
            variant="secondary"
            className="pf-u-mt-lg"
            icon={<PlusCircleIcon />}
            onClick={addTopicGroup}
          >
            {t('addTopicGroup')}
          </Button>
        </>
      )}
    </div>
    // tslint:enable: no-string-literal
  );
};
