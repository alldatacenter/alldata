import './TopicGroupConfig.css';
import {
  Button,
  Form,
  Grid,
  GridItem,
  Text,
  TextVariants,
  Title,
  TitleSizes,
} from '@patternfly/react-core';
import { PlusCircleIcon } from '@patternfly/react-icons';
import { FormComponent, TopicGroupOptionItem } from 'components';
import { Formik, useFormikContext } from 'formik';
import _ from 'lodash';
import React from 'react';
import { useTranslation } from 'react-i18next';
import { formatPropertyDefinitions } from 'shared';
import * as Yup from 'yup';

export interface ITopicGroupOption {
  name?: any;
  value?: any;
}

export interface ITopicGroupConfigProps {
  topicGroupNo: number;
  topicGroupConfigProperties: any[];
  topicGroupConfigValues?: any;
  topicGroupOptionProperties: any[];
  updateTopicGroup: (key: number, field: string, value: any) => void;
  setIsTopicCreationDirty: (data: boolean) => void;
}

const FormSubmit: React.FunctionComponent<any> = React.forwardRef(
  (props, ref) => {
    const { dirty, submitForm, validateForm } = useFormikContext();

    React.useImperativeHandle(ref, () => ({
      async validate() {
        const valid = await validateForm();
        const validPromise = new Promise((resolve, reject) => {
          if (_.isEmpty(valid)) {
            resolve('done');
          } else {
            reject('fail');
          }
        });
        submitForm();
        return validPromise;
      },
    }));
    React.useEffect(() => {
      if (dirty) {
        props.setIsTopicCreationDirty(true);
      }
    }, [dirty]);
    return null;
  }
);

export const TopicGroupConfig: React.FunctionComponent<any> = React.forwardRef(
  (props, ref) => {
    const { t } = useTranslation();

    const optionPropList = formatPropertyDefinitions(
      props.topicGroupOptionProperties
    );

    const getInitialTopicGroupOptions = () => {
      const initialOptions: ITopicGroupOption[] = [];

      const userValues = { ...props.topicGroupConfigValues };
      // Array of Option property names
      for (const prop of optionPropList) {
        if (userValues[prop.name]) {
          const option = {} as ITopicGroupOption;
          option.name = prop.name;
          option.value = userValues[prop.name];
          initialOptions.push(option);
        }
      }
      return initialOptions;
    };

    const [topicGroupOptions, setTopicGroupOptions] = React.useState<
      ITopicGroupOption[]
    >(getInitialTopicGroupOptions);

    const getInitialValues = () => {
      const combinedValue: any = {};
      const userValues = { ...props.topicGroupConfigValues };

      for (const prop of props.topicGroupConfigProperties) {
        if (!combinedValue[prop.name]) {
          if (_.isEmpty(userValues)) {
            prop.defaultValue === undefined
              ? (combinedValue[prop.name] =
                  prop.type === 'INT' || prop.type === 'LONG' ? 0 : '')
              : (combinedValue[prop.name] = prop.defaultValue);
          } else {
            combinedValue[prop.name] = userValues[prop.name];
          }
        }
      }
      return combinedValue;
    };

    const initialValues = getInitialValues();

    const basicValidationSchema = {};

    const topicGroupConfigurationList = [...props.topicGroupConfigProperties];

    topicGroupConfigurationList.map((key: any) => {
      if (key.type === 'STRING') {
        basicValidationSchema[key.name] = Yup.string();
      } else if (key.type === 'PASSWORD') {
        basicValidationSchema[key.name] = Yup.string();
      } else if (
        key.type === 'INT' ||
        key.type === 'LONG' ||
        key.type === 'NON-NEG-INT' ||
        key.type === 'NON-NEG-LONG' ||
        key.type === 'POS-INT'
      ) {
        basicValidationSchema[key.name] = Yup.number().strict();
      }
      if (key.isMandatory) {
        basicValidationSchema[key.name] = basicValidationSchema[
          key.name
        ].required(`${key.name} required`);
      }
    });
    const validationSchema = Yup.object().shape({ ...basicValidationSchema });

    const handleTopicGroupOptionNameChanged = (
      rowId: number,
      topicGroupOptionName: string
    ) => {
      const newOptions = [...topicGroupOptions];
      newOptions[rowId].name = topicGroupOptionName;
      setTopicGroupOptions(newOptions);
      props.setIsTopicCreationDirty(true);
    };

    const handleTopicGroupOptionValueChanged = (
      rowId: number,
      topicGroupOptionValue: any
    ) => {
      const newOptions = [...topicGroupOptions];
      newOptions[rowId].value = topicGroupOptionValue;
      setTopicGroupOptions(newOptions);
      props.setIsTopicCreationDirty(true);
    };

    const handleDeleteTopicGroupOptionItem = (rowId: number) => {
      const newOptions = [...topicGroupOptions];
      newOptions.splice(rowId, 1);
      setTopicGroupOptions(newOptions);
      props.setIsTopicCreationDirty(true);
    };

    const addTopicGroupOption = () => {
      const newOptions = [...topicGroupOptions];
      const newOption = { name: undefined, value: undefined };
      newOptions.push(newOption);
      setTopicGroupOptions(newOptions);
      props.setIsTopicCreationDirty(true);
    };

    const handleSubmit = (value: any) => {
      const basicValue = {};
      for (const basicVal of props.topicGroupConfigProperties) {
        basicValue[basicVal.name.replace(/&/g, '.')] = value[basicVal.name];
      }
      for (const option of topicGroupOptions) {
        if (option.name) {
          basicValue[option.name] = option.value;
        }
      }
      props.updateTopicGroup(props.topicGroupNo, 'config', basicValue);
    };

    return (
      <>
        <Formik
          initialValues={initialValues}
          validationSchema={validationSchema}
          onSubmit={(values) => {
            handleSubmit(values);
          }}
          enableReinitialize={true}
        >
          {({ errors, touched, setFieldValue }) => (
            <Form className="pf-c-form">
              <Grid hasGutter={true}>
                {props.topicGroupConfigProperties.map((prop, index) => {
                  return (
                    <GridItem
                      key={index}
                      lg={prop.gridWidthLg}
                      sm={prop.gridWidthSm}
                    >
                      <FormComponent
                        propertyDefinition={prop}
                        // tslint:disable-next-line: no-empty
                        propertyChange={() => {}}
                        setFieldValue={setFieldValue}
                        helperTextInvalid={errors[prop.name]}
                        invalidMsg={[]}
                        validated={
                          errors[prop.name] && touched[prop.name]
                            ? 'error'
                            : 'default'
                        }
                        // tslint:disable-next-line: no-empty
                        clearValidationError={() => {}}
                      />
                    </GridItem>
                  );
                })}
                <Title headingLevel="h6" size={TitleSizes.lg}>
                  {t('topicGroupOptionsTitle')}
                </Title>
                <div
                  className={'topic-group-option-block pf-u-p-sm pf-u-pb-lg'}
                >
                  {topicGroupOptions.length === 0 ? (
                    <Text component={TextVariants.h2}>
                      {t('topicGroupOptionsNoneDefinedText')}
                    </Text>
                  ) : undefined}
                  {topicGroupOptions.map((option, index) => {
                    return (
                      <TopicGroupOptionItem
                        key={index}
                        rowId={index}
                        itemName={option.name}
                        itemValue={option.value}
                        topicGroupOptionProperties={optionPropList}
                        topicGroupOptionNameChanged={
                          handleTopicGroupOptionNameChanged
                        }
                        topicGroupOptionValueChanged={
                          handleTopicGroupOptionValueChanged
                        }
                        deleteTopicGroupOptionItem={
                          handleDeleteTopicGroupOptionItem
                        }
                        topicGroupOptions={topicGroupOptions}
                      />
                    );
                  })}
                  <Grid hasGutter={true}>
                    <GridItem span={2}>
                      <Button
                        variant="secondary"
                        className="pf-u-mt-lg"
                        icon={<PlusCircleIcon />}
                        onClick={addTopicGroupOption}
                      >
                        {t('addTopicGroupOption')}
                      </Button>
                    </GridItem>
                  </Grid>
                </div>
                <FormSubmit
                  ref={ref}
                  setIsTopicCreationDirty={props.setIsTopicCreationDirty}
                />
              </Grid>
            </Form>
          )}
        </Formik>
      </>
    );
  }
);
