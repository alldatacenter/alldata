import './TopicGroupCard.css';
import {
  Button,
  Form,
  Grid,
  GridItem,
  Split,
  SplitItem,
  Title,
} from '@patternfly/react-core';
import { TrashIcon } from '@patternfly/react-icons';
import { NameInputField, TopicGroupConfig } from 'components';
import _ from 'lodash';
import React from 'react';
import { useTranslation } from 'react-i18next';
import { getFormattedTopicCreationProperties } from 'shared';

export interface ITopicGroupCardProps {
  topicGroupNo: number;
  topicGroupName: string;
  topicGroupNameList: string[];
  topicGroupConfig: any;
  deleteTopicGroup: (order: number) => void;
  updateTopicGroup: (key: number, field: string, value: any) => void;
  topicGroupsData: any;
  topicGroupOptionsData: any;
  setIsTopicCreationDirty: (data: boolean) => void;
}

export interface IOptionsValidationRef {
  validate: () => Promise<any>;
}

export const TopicGroupCard: React.FunctionComponent<any> = React.forwardRef(
  (props, ref) => {
    const { t } = useTranslation();
    const [nameIsValid, setNameIsValid] = React.useState<boolean>(true);

    const optionsRef =
      React.useRef() as React.MutableRefObject<IOptionsValidationRef>;

    const deleteCard = () => {
      props.deleteTopicGroup(props.topicGroupNo);
    };

    const updateName = (value: string, field?: string) => {
      if (field) {
        value === '' || props.topicGroupNameList.includes(value)
          ? setNameIsValid(false)
          : setNameIsValid(true);
        props.updateTopicGroup(props.topicGroupNo, 'name', value);
      }
    };

    React.useImperativeHandle(ref, () => ({
      check() {
        const validPromise = new Promise((resolve, reject) => {
          if (props.topicGroupName) {
            optionsRef?.current!.validate().then(
              (d) => {
                resolve('done');
              },
              (e) => {
                reject('fail');
              }
            );
          } else {
            setNameIsValid(false);
            reject('fail');
          }
        });
        return validPromise;
      },
    }));

    return (
      <Grid>
        <GridItem span={12}>
          <div
            className={'topic-group-block pf-u-mt-lg pf-u-p-sm pf-u-pb-lg'}
            id="topic-group-parent"
          >
            <Split>
              <SplitItem isFilled={true}>
                <Title headingLevel="h2">
                  Topic Group # {props.topicGroupNo} &nbsp;
                </Title>
                <Form>
                  <Grid hasGutter={true}>
                    <GridItem span={4}>
                      <NameInputField
                        label="Name"
                        description=""
                        fieldId="topic_group_name"
                        isRequired={true}
                        name="topic_group_name"
                        placeholder="Name"
                        inputType="text"
                        value={props.topicGroupName}
                        setFieldValue={updateName}
                        isInvalid={!nameIsValid}
                        invalidText={
                          props.topicGroupName
                            ? t('uniqueName')
                            : t('nameRequired')
                        }
                      />
                    </GridItem>
                  </Grid>
                </Form>
                <TopicGroupConfig
                  ref={optionsRef}
                  topicGroupNo={props.topicGroupNo}
                  topicGroupConfigProperties={getFormattedTopicCreationProperties(
                    props.topicGroupsData
                  )}
                  topicGroupConfigValues={props.topicGroupConfig}
                  topicGroupOptionProperties={getFormattedTopicCreationProperties(
                    props.topicGroupOptionsData
                  )}
                  updateTopicGroup={props.updateTopicGroup}
                  setIsTopicCreationDirty={props.setIsTopicCreationDirty}
                />
              </SplitItem>
              <SplitItem>
                <Button
                  variant="link"
                  icon={<TrashIcon />}
                  onClick={deleteCard}
                  id="tooltip-selector"
                />
              </SplitItem>
            </Split>
          </div>
        </GridItem>
      </Grid>
    );
  }
);
