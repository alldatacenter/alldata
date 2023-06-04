import {
  Affix,
  Button,
  Card,
  Form,
  message,
  Popconfirm,
  Spin,
  Tooltip,
} from 'antd';
import { DetailPageHeader } from 'app/components/DetailPageHeader';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { getFolders } from 'app/pages/MainPage/pages/VizPage/slice/thunks';
import { CommonFormTypes } from 'globalConstants';
import {
  FC,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useRouteMatch } from 'react-router-dom';
import styled from 'styled-components/macro';
import { BORDER_RADIUS, SPACE_LG, SPACE_SM } from 'styles/StyleConstants';
import { getInsertedNodeIndex } from 'utils/utils';
import { selectIsOrgOwner } from '../../../slice/selectors';
import { DEFAULT_VALUES, FileTypes, JobTypes, TimeModes } from '../constants';
import { useToScheduleDetails } from '../hooks';
import { SaveFormContext } from '../SaveFormContext';
import { useScheduleSlice } from '../slice';
import {
  selectDeleteLoading,
  selectEditingSchedule,
  selectSaveLoading,
  selectScheduleDetailLoading,
  selectSchedules,
  selectUnarchiveLoading,
} from '../slice/selectors';
import {
  addSchedule,
  deleteSchedule,
  editSchedule,
  getScheduleDetails,
  getSchedules,
  unarchiveSchedule,
} from '../slice/thunks';
import { FormValues } from '../types';
import {
  getCronExpressionByPartition,
  getTimeValues,
  toEchoFormValues,
  toScheduleSubmitParams,
} from '../utils';
import { BasicBaseForm } from './BasicBaseForm';
import { EmailSettingForm } from './EmailSettingForm';
import { ScheduleErrorLog } from './ScheduleErrorLog';
import { SendContentForm } from './SendContentForm';
import { WeChatSettingForm } from './WeChatSettingForm';

export const EditorPage: FC = () => {
  const [form] = Form.useForm();
  const [container, setContainer] = useState<HTMLDivElement | null>(null);
  const dispatch = useDispatch();
  const [jobType, setJobType] = useState(JobTypes.Email);
  const [fileType, setFileType] = useState<FileTypes[]>(
    DEFAULT_VALUES.type || [],
  );
  const [periodUnit, setPeriodUnit] = useState<TimeModes>(TimeModes.Minute);
  const [periodInput, setPeriodInput] = useState(false);
  const { params } = useRouteMatch<{ scheduleId: string; orgId: string }>();
  const editingSchedule = useSelector(selectEditingSchedule);
  const loading = useSelector(selectScheduleDetailLoading);
  const saveLoading = useSelector(selectSaveLoading);
  const unarchiveLoading = useSelector(selectUnarchiveLoading);
  const deleteLoading = useSelector(selectDeleteLoading);
  const schedules = useSelector(selectSchedules);
  const { showSaveForm } = useContext(SaveFormContext);
  const { toDetails } = useToScheduleDetails();
  const isArchived = editingSchedule?.status === 0;
  const t = useI18NPrefix('schedule.editor.index');
  const isOwner = useSelector(selectIsOrgOwner);
  const { actions } = useScheduleSlice();
  const { scheduleId, orgId } = params;
  const isAdd = useMemo(() => {
    return scheduleId === 'add';
  }, [scheduleId]);
  const active = useMemo(() => {
    return isAdd ? false : editingSchedule?.active;
  }, [editingSchedule, isAdd]);
  const refreshScheduleList = useCallback(() => {
    dispatch(getSchedules(orgId));
  }, [dispatch, orgId]);
  const onFinish = useCallback(() => {
    form.validateFields().then((values: FormValues) => {
      if (!(values?.folderContent && values?.folderContent?.length > 0)) {
        message.error(t('tickToSendContent'));
        return;
      }
      let index = getInsertedNodeIndex(values, schedules);
      const params = toScheduleSubmitParams(
        { ...values, index, parentId: editingSchedule?.parentId || null },
        orgId,
      );
      if (isAdd) {
        showSaveForm({
          scheduleType: 'title',
          type: CommonFormTypes.Add,
          visible: true,
          simple: true,
          parentIdLabel: t('parent'),
          onSave: (val, onClose) => {
            dispatch(
              addSchedule({
                params: { ...params, parentId: val.parentId },
                resolve: (id: string) => {
                  message.success(t('addSuccess'));
                  toDetails(orgId, id);
                  onClose();
                },
              }),
            );
          },
        });
      } else {
        dispatch(
          editSchedule({
            scheduleId: editingSchedule?.id as string,
            params: {
              ...params,
              index: editingSchedule?.index || null,
              id: editingSchedule?.id as string,
            },
            resolve: () => {
              message.success(t('saveSuccess'));
              dispatch(getScheduleDetails(editingSchedule?.id!));
              refreshScheduleList();
            },
          }),
        );
      }
    });
  }, [
    form,
    schedules,
    editingSchedule?.parentId,
    editingSchedule?.id,
    editingSchedule?.index,
    orgId,
    isAdd,
    t,
    showSaveForm,
    dispatch,
    toDetails,
    refreshScheduleList,
  ]);

  const onResetForm = useCallback(() => {
    form.resetFields();
    setJobType(DEFAULT_VALUES.jobType as JobTypes);
    setPeriodUnit(TimeModes.Minute);
    setPeriodInput(false);
    setFileType([FileTypes.Image]);
    dispatch(actions.clearEditingSchedule());
  }, [form, dispatch, actions]);

  useEffect(() => {
    return () => {
      onResetForm();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    dispatch(getFolders(orgId));
    if (scheduleId === 'add') {
      onResetForm();
    } else if (scheduleId) {
      dispatch(getScheduleDetails(scheduleId));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [scheduleId, dispatch]);
  const onJobTypeChange = useCallback(
    (v: JobTypes) => {
      setJobType(v);
      setFileType([FileTypes.Image]);
      form.setFieldsValue({
        type: [FileTypes.Image],
        imageWidth: DEFAULT_VALUES.imageWidth,
      });
    },
    [form],
  );
  const onFileTypeChange = useCallback(
    (v: FileTypes[]) => {
      setFileType(v);
      form.setFieldsValue({
        imageWidth:
          v && v.includes(FileTypes.Image)
            ? DEFAULT_VALUES.imageWidth
            : undefined,
      });
    },
    [form],
  );
  const onPeriodUnitChange = useCallback(
    (v: TimeModes) => {
      setPeriodUnit(v);
      switch (v) {
        case TimeModes.Minute:
          form.setFieldsValue({ minute: 10 });
          break;
      }
    },
    [form],
  );
  const onPeriodInputChange = useCallback(
    (v: boolean) => {
      if (v) {
        form.setFieldsValue({
          cronExpression: getCronExpressionByPartition(form.getFieldsValue()),
        });
      } else {
        const timeValues = getTimeValues(form.getFieldValue('cronExpression'));
        form.setFieldsValue(timeValues);
        setPeriodUnit(timeValues?.periodUnit);
      }
      setPeriodInput(v);
    },
    [form],
  );

  const unarchive = useCallback(() => {
    if (unarchiveLoading) return;
    const { id, name } = editingSchedule!;
    showSaveForm({
      scheduleType: 'folder',
      type: CommonFormTypes.Edit,
      visible: true,
      simple: false,
      initialValues: { id, name, parentId: null },
      parentIdLabel: t('parent'),
      onSave: (values, onClose) => {
        let index = getInsertedNodeIndex(values, schedules);
        dispatch(
          unarchiveSchedule({
            schedule: { ...values, id, index },
            resolve: () => {
              message.success(t('restoredSuccess'));
              toDetails(orgId);
              onClose();
            },
          }),
        );
      },
    });
  }, [
    unarchiveLoading,
    editingSchedule,
    showSaveForm,
    t,
    schedules,
    dispatch,
    toDetails,
    orgId,
  ]);

  const del = useCallback(
    archive => () => {
      dispatch(
        deleteSchedule({
          id: editingSchedule!.id,
          archive,
          resolve: () => {
            message.success(
              `${t('success')}${archive ? t('moveToTrash') : t('delete')}`,
            );
            toDetails(orgId);
          },
        }),
      );
    },
    [dispatch, toDetails, orgId, editingSchedule, t],
  );

  useEffect(() => {
    if (editingSchedule) {
      const _type = editingSchedule?.type as JobTypes,
        echoValues = toEchoFormValues(editingSchedule);
      form.setFieldsValue(echoValues);
      setFileType(echoValues?.type as FileTypes[]);
      setJobType(_type);
      setPeriodUnit(echoValues?.periodUnit as TimeModes);
      setPeriodInput(!!echoValues?.setCronExpressionManually);
    }
    return () => {
      setJobType(DEFAULT_VALUES.jobType as JobTypes);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editingSchedule]);

  return (
    <Container ref={setContainer}>
      <Affix offsetTop={0} target={() => container}>
        <DetailPageHeader
          title={isAdd ? t('newTimedTask') : editingSchedule?.name}
          actions={
            isArchived ? (
              isOwner ? (
                <>
                  <Button loading={unarchiveLoading} onClick={unarchive}>
                    {t('restore')}
                  </Button>
                  <Popconfirm title={t('sureToDelete')} onConfirm={del(false)}>
                    <Button loading={deleteLoading} danger>
                      {t('delete')}
                    </Button>
                  </Popconfirm>
                </>
              ) : (
                <></>
              )
            ) : (
              <>
                <Tooltip
                  placement="bottom"
                  title={active ? t('allowModificationAfterStopping') : ''}
                >
                  <Button
                    loading={saveLoading}
                    type="primary"
                    onClick={form.submit}
                    disabled={active}
                  >
                    {t('save')}
                  </Button>
                </Tooltip>
                {!isAdd && (
                  <Tooltip
                    placement="bottom"
                    title={active ? t('allowMoveAfterStopping') : ''}
                  >
                    <Popconfirm
                      title={t('sureMoveRecycleBin')}
                      onConfirm={del(true)}
                    >
                      <Button loading={deleteLoading} disabled={active} danger>
                        {t('moveToTrash')}
                      </Button>
                    </Popconfirm>
                  </Tooltip>
                )}
              </>
            )
          }
        />
      </Affix>
      <EditorWrapper>
        <Spin spinning={loading}>
          <Form
            wrapperCol={{ span: 19 }}
            labelCol={{ span: 5 }}
            initialValues={DEFAULT_VALUES}
            form={form}
            onFinish={onFinish}
            scrollToFirstError
          >
            <FormAreaWrapper>
              {!isAdd && editingSchedule?.id ? (
                <ScheduleErrorLog scheduleId={editingSchedule?.id} />
              ) : null}
              <FormCard title={t('basicSettings')}>
                <FormWrapper>
                  <BasicBaseForm
                    isAdd={isAdd}
                    orgId={orgId}
                    onJobTypeChange={onJobTypeChange}
                    initialName={editingSchedule?.name}
                    periodUnit={periodUnit}
                    onPeriodUnitChange={onPeriodUnitChange}
                    periodInput={periodInput}
                    onPeriodInputChange={onPeriodInputChange}
                  />
                </FormWrapper>
              </FormCard>

              {jobType === JobTypes.Email ? (
                <FormCard title={t('emailSetting')}>
                  <FormWrapper>
                    <EmailSettingForm
                      fileType={fileType}
                      onFileTypeChange={onFileTypeChange}
                    />
                  </FormWrapper>
                </FormCard>
              ) : (
                <FormCard title={t('enterpriseWeChatSettings')}>
                  <FormWrapper>
                    <WeChatSettingForm />
                  </FormWrapper>
                </FormCard>
              )}
              <FormCard title={t('sendContentSettings')}>
                <FormWrapper>
                  <SendContentForm />
                </FormWrapper>
              </FormCard>
            </FormAreaWrapper>
          </Form>
        </Spin>
      </EditorWrapper>
    </Container>
  );
};

const Container = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
`;

const EditorWrapper = styled.div`
  flex: 1;
  padding: ${SPACE_LG};
  overflow-y: auto;
`;

const FormAreaWrapper = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  .image_width_form_item_wrapper {
    display: flex;
    flex-direction: row;
    flex-wrap: nowrap;
    align-items: flex-start;
    .ant-row.ant-form-item {
      width: 70%;
    }
    .ant-input-number {
      width: 100%;
    }
    .image_width_unit {
      display: inline-block;
      height: 30px;
      margin-left: ${SPACE_SM};
      line-height: 30px;
    }
  }
`;

const FormCard = styled(Card)`
  &.ant-card {
    margin-top: ${SPACE_LG};
    background-color: ${p => p.theme.componentBackground};
    border-radius: ${BORDER_RADIUS};
    box-shadow: ${p => p.theme.shadowBlock};
  }
`;
const FormWrapper = styled.div`
  width: 860px;
`;
