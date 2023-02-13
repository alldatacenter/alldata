// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import React, { useEffect } from 'react';
import { map, isEmpty } from 'lodash';
import classnames from 'classnames';
import i18n from 'i18n';
import { DatePicker, Spin, Pagination, Button } from 'antd';
import { Holder, Icon as CustomIcon } from 'common';
import { useUpdate, useFormModal } from 'common/use-hooks';
import { getTimeRanges } from 'common/utils';
import projectReportStore from 'msp/stores/project-report';
import { useLoading } from 'core/stores/loading';

import './project-report.scss';
import { useMount } from 'react-use';

const { RangePicker } = DatePicker;

interface IProps {
  type: 'weekly' | 'daily';
}

const ProjectReport = ({ type }: IProps) => {
  const [state, updater] = useUpdate({
    activeReportKey: '',
    reports: [],
    reportDetail: '',
    dateRange: [],
  });
  const [projectReportsPaging, reportSeeting] = projectReportStore.useStore((s) => [
    s.projectReportsPaging,
    s.reportSeeting,
  ]);
  const [isFetching, isFetchingDetail] = useLoading(projectReportStore, ['getProjectReport', 'getProjectReportDetail']);
  const { getProjectReport, getProjectReportDetail, getProjectReportSetting, setProjectReportSetting } =
    projectReportStore.effects;
  useMount(() => {
    getProjectReportSetting();
  });

  const [FormModal, toggle] = useFormModal();

  useMount(() => {
    fetchReports({ type });
  });

  useEffect(() => {
    const [start, end] = state.dateRange;
    fetchReports({ type, start, end });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state.dateRange]);

  const { pageNo, total } = projectReportsPaging;

  const fetchReports = (payload: any) => {
    getProjectReport(payload).then((reports: PROJECT_REPORT.IReport[]) => {
      updater.reports(reports);
      if (!isEmpty(reports)) {
        handleChangeReport(reports[0].key);
      } else {
        updater.reportDetail('');
      }
    });
  };

  const handleChangeReport = (key: string) => {
    getProjectReportDetail({ type, key }).then((res) => {
      updater.activeReportKey(key);
      updater.reportDetail(res);
    });
  };

  const handleRangeChange = (dates: any[]) => {
    if (dates[0] && dates[1]) {
      updater.dateRange([dates[0].valueOf(), dates[1].valueOf()]);
    } else {
      updater.dateRange([]);
    }
  };

  const handleChangePage = (num: number) => {
    if (num !== pageNo) {
      const [start, end] = state.dateRange;
      fetchReports({ type, pageNo: num, start, end });
    }
  };

  const handleSetProjectReportSetting = (data: any) => {
    const setting =
      type === 'weekly'
        ? {
            weeklyReportEnable: data.weeklyReportEnable,
            weeklyReportConfig: JSON.stringify({ emails: data.emails.split(',') }),
          }
        : {
            dailyReportEnable: data.dailyReportEnable,
            dailyReportConfig: JSON.stringify({ dingdingURLs: data.dingdingURLs.split(',') }),
          };
    setProjectReportSetting(setting);
    toggle();
  };

  const fieldsList = {
    weekly: [
      {
        label: i18n.t('msp:enable weekly report'),
        name: 'weeklyReportEnable',
        initialValue: reportSeeting.weeklyReportEnable,
        type: 'switch',
      },
      {
        label: i18n.t('msp:mail recipient'),
        name: 'emails',
        type: 'textArea',
        required: false,
        pattern:
          /^([A-Za-z0-9_\-.\u4e00-\u9fa5])+@([A-Za-z0-9_\-.])+\.([A-Za-z]{2,8})([,]([A-Za-z0-9_\-.\u4e00-\u9fa5])+@([A-Za-z0-9_\-.])+\.([A-Za-z]{2,8}))*$/,
        message: i18n.t('please enter a valid email address'),
        initialValue: reportSeeting.weeklyReportConfig
          ? JSON.parse(reportSeeting.weeklyReportConfig).emails.join(',')
          : '',
        itemProps: {
          placeholder: i18n.t('msp:please enter your email address, separated by comma'),
          maxLength: 1000,
          autoSize: { minRows: 3, maxRows: 7 },
        },
      },
    ],
    daily: [
      {
        label: i18n.t('msp:enable daily report'),
        name: 'dailyReportEnable',
        initialValue: reportSeeting.dailyReportEnable,
        type: 'switch',
      },
      {
        label: i18n.t('DingTalk address'),
        name: 'dingdingURLs',
        type: 'textArea',
        required: false,
        // pattern: /^(https:\/\/oapi\.dingtalk\.com\/robot\/send\?access_token=(.*[^,]))([,]https:\/\/oapi\.dingtalk\.com\/robot\/send\?access_token=(.*[^,]))*$/,
        // message: i18n.t('Please enter a valid DingTalk address.'),
        initialValue: reportSeeting.dailyReportConfig
          ? JSON.parse(reportSeeting.dailyReportConfig).dingdingURLs.join(',')
          : '',
        itemProps: {
          placeholder: i18n.t('msp:Please enter your DingTalk address, separated by comma.'),
          maxLength: 1000,
          autoSize: { minRows: 3, maxRows: 7 },
        },
      },
    ],
  };

  return (
    <div className="project-report-container">
      <div className="project-report-list">
        <div className="top-button-group">
          <Button onClick={toggle}>
            {type === 'weekly' ? i18n.t('msp:weekly report settings') : i18n.t('msp:daily report settings')}
          </Button>
        </div>
        <div className="search-table-section">
          <div className="search-table-header pr-5">
            <RangePicker onChange={handleRangeChange} ranges={getTimeRanges()} />
          </div>
          <div className="search-table-content">
            <Spin spinning={isFetching}>
              <Holder when={isEmpty(state.reports)}>
                <ul className="report-list-container">
                  {map(state.reports, (item: PROJECT_REPORT.IReport) => (
                    <li
                      className={classnames({
                        'report-list-item': true,
                        'text-base': true,
                        'pl-5': true,
                        'font-medium': true,
                        'text-left': true,
                        'hover-active-bg': true,
                        active: state.activeReportKey === item.key,
                      })}
                      key={item.key}
                      onClick={() => {
                        handleChangeReport(item.key);
                      }}
                    >
                      <CustomIcon className="mr-2" type="rw" />
                      {type === 'weekly'
                        ? `${item.start.split(' ')[0]}-${item.end.split(' ')[0]}`
                        : `${item.start.split(' ')[0]}`}
                    </li>
                  ))}
                </ul>
                {total && (
                  <Pagination
                    className="project-report-pagination text-right mt-3"
                    simple
                    defaultCurrent={1}
                    total={total}
                    onChange={handleChangePage}
                  />
                )}
              </Holder>
            </Spin>
          </div>
        </div>
      </div>
      <div className="project-report-detail pl-8">
        <Spin spinning={isFetchingDetail}>
          <Holder when={!state.reportDetail}>
            {/* eslint-disable-next-line react/no-danger */}
            <div dangerouslySetInnerHTML={{ __html: state.reportDetail }} />
          </Holder>
        </Spin>
      </div>
      <FormModal
        name={type === 'weekly' ? i18n.t('msp:weekly report settings') : i18n.t('msp:daily report settings')}
        fieldsList={fieldsList[type]}
        formData={reportSeeting}
        modalProps={{ destroyOnClose: true }}
        onOk={handleSetProjectReportSetting}
      />
    </div>
  );
};

export default ProjectReport;
