import { ConnectorDrawer } from './ConnectorDrawer';
import { ConnectorOverview } from './ConnectorOverview';
import { ConnectorStatus } from './ConnectorStatus';
import { ConnectorTask } from './ConnectorTask';
import { ConnectorTaskState } from './ConnectorTaskState';
import './ConnectorsTableComponent.css';
import { Connector } from '@debezium/ui-models';
import { Services } from '@debezium/ui-services';
import {
  Button,
  Dropdown,
  DropdownItem,
  DropdownPosition,
  DropdownToggle,
  EmptyState,
  EmptyStateBody,
  EmptyStateIcon,
  EmptyStateVariant,
  Flex,
  FlexItem,
  Label,
  Title,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from '@patternfly/react-core';
import {
  CubesIcon,
  FilterIcon,
  SortAmountDownAltIcon,
  SortAmountDownIcon,
} from '@patternfly/react-icons';
import {
  cellWidth,
  expandable,
  IAction,
  IExtraData,
  IRowData,
  Table,
  TableBody,
  TableHeader,
} from '@patternfly/react-table';
import { PageLoader, ToastAlertComponent, ConnectorIcon } from 'components';
import { AppLayoutContext } from 'layout';
import React, { SyntheticEvent } from 'react';
import isEqual from 'react-fast-compare';
import { useTranslation } from 'react-i18next';
import {
  ApiError,
  ConfirmationButtonStyle,
  ConfirmationDialog,
  ConfirmationType,
  ConnectorTypeId,
  fetch_retry,
  WithLoader,
} from 'shared';

type ICreateConnectorCallbackFn = (
  connectorNames: string[],
  clusterId: number
) => void;

export interface IConnectorsTableComponentProps {
  clusterId: number;
  title: string;
  i18nApiErrorTitle?: string;
  i18nApiErrorMsg?: string;
  createConnectorCallback: ICreateConnectorCallbackFn;
}

export const ConnectorsTableComponent: React.FunctionComponent<
  IConnectorsTableComponentProps
> = (props: IConnectorsTableComponentProps) => {
  const enum Action {
    DELETE = 'DELETE',
    PAUSE = 'PAUSE',
    RESUME = 'RESUME',
    RESTART = 'RESTART',
    VIEW = 'VIEW',
    RESTART_TASK = 'RESTART_TASK',
    NONE = 'NONE',
  }
  const [connectors, setConnectors] = React.useState<Connector[]>(
    [] as Connector[]
  );
  const [tableRows, setTableRows] = React.useState<any[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [apiError, setApiError] = React.useState<boolean>(false);
  const [errorMsg, setErrorMsg] = React.useState<Error>(new Error());

  const appLayoutContext = React.useContext(AppLayoutContext);
  const [currentAction, setCurrentAction] = React.useState(Action.NONE);
  const [currentActionConnector, setCurrentActionConnector] =
    React.useState('');

  const [alerts, setAlerts] = React.useState<any[]>([]);
  const [connectorTaskToRestart, setConnectorTaskToRestart] = React.useState<
    any[]
  >([]);

  const [selectedConnector, setSelectedConnector] = React.useState<Connector>();

  const { t } = useTranslation();
  const [isSortingDropdownOpen, setIsSortingDropdownOpen] =
    React.useState(false);
  const [currentCategory, setCurrentCategory] = React.useState<string>('Name');
  const [desRowOrder, setDesRowOrder] = React.useState<boolean>(false);

  const [expandedRows, setExpandedRows] = React.useState<number[]>([]);

  const addAlert = (type: string, heading: string, msg?: string) => {
    const alertsCopy = [...alerts];
    const uId = new Date().getTime();
    const newAlert = {
      title: heading,
      variant: type,
      key: uId,
      message: msg ? msg : '',
    };
    alertsCopy.push(newAlert);
    setAlerts(alertsCopy);
  };

  const removeAlert = (key: string) => {
    setAlerts([...alerts.filter((el) => el.key !== key)]);
  };

  const resetCurrentAction = () => {
    setCurrentAction(Action.NONE);
    setCurrentActionConnector('');
  };

  const setCurrentActionAndName = (
    action: Action,
    connName: string,
    connector?: any
  ) => {
    if (action === Action.VIEW) {
      setSelectedConnector({
        connectorStatus: connector.connStatus,
        connectorType: '',
        name: connector.connName,
        taskStates: {},
      });
      console.log(connector);
    } else {
      setCurrentAction(action);
      setCurrentActionConnector(connName);
    }
  };

  const doPerformCurrentAction = () => {
    const connName = currentActionConnector;
    const connectorService = Services.getConnectorService();
    resetCurrentAction(); // unset to dismiss dialog
    switch (currentAction) {
      case Action.DELETE:
        connectorService
          .deleteConnector(appLayoutContext.clusterId, connName)
          .then((cConnectors: Connector[]) => {
            addAlert(
              'success',
              t('connectorDeletedSuccess', { connectorName: connName })
            );
            // Remove connName and reset connectors - if successful
            const newConnectors = connectors.filter(function (conn) {
              return conn.name !== connName;
            });
            setConnectors(newConnectors);
          })
          .catch((err) => {
            addAlert(
              'danger',
              t('connectorDeletionFailed', { connectorName: connName }),
              err?.message
            );
          });
        break;
      case Action.PAUSE:
        connectorService
          .pauseConnector(appLayoutContext.clusterId, connName, {})
          .then((cConnectors: any) => {
            addAlert(
              'success',
              t('connectorPausedSuccess', { connectorName: connName })
            );
            setConnectorStatus(connName, 'PAUSED');
          })
          .catch((err) => {
            addAlert(
              'danger',
              t('connectorPauseFailed', { connectorName: connName }),
              err?.message
            );
          });
        break;
      case Action.RESUME:
        connectorService
          .resumeConnector(appLayoutContext.clusterId, connName, {})
          .then((cConnectors: any) => {
            addAlert(
              'success',
              t('connectorResumedSuccess', { connectorName: connName })
            );
            setConnectorStatus(connName, 'RUNNING');
          })
          .catch((err) => {
            addAlert(
              'danger',
              t('connectorResumeFailed', { connectorName: connName }),
              err?.message
            );
          });
        break;
      case Action.RESTART:
        connectorService
          .restartConnector(appLayoutContext.clusterId, connName, {})
          .then((cConnectors: any) => {
            addAlert(
              'success',
              t('connectorRestartSuccess', { connectorName: connName })
            );
            setConnectorStatus(connName, 'RUNNING');
          })
          .catch((err) => {
            addAlert(
              'danger',
              t('connectorRestartFailed', { connectorName: connName }),
              err?.message
            );
          });
        break;
      case Action.RESTART_TASK:
        const [connectorName, connectorTaskId] = connectorTaskToRestart;
        connectorService
          .restartConnectorTask(
            appLayoutContext.clusterId,
            connectorName,
            connectorTaskId,
            {}
          )
          .then((cConnectors: any) => {
            addAlert(
              'success',
              t('connectorTaskRestartSuccess', { connectorName: connName })
            );
          })
          .catch((err) => {
            addAlert(
              'danger',
              t('connectorTaskRestartFailed', { connectorName: connName }),
              err?.message
            );
          });
        break;
      default:
        break;
    }
  };

  const createConnector = () => {
    const connectorNames = connectors.map((conn) => {
      return conn.name;
    });
    props.createConnectorCallback(connectorNames, props.clusterId);
  };
  const prevStateRef = React.useRef([] as Connector[]);
  React.useEffect(() => {
    prevStateRef.current = connectors;
  });
  const prevState = prevStateRef.current;

  React.useEffect(() => {
    if (!isEqual(prevState, connectors)) {
      updateTableRows([...connectors], desRowOrder);
    }
  }, [connectors]);

  const getConnectorsList = () => {
    const connectorService = Services.getConnectorService();
    fetch_retry(connectorService.getConnectors, connectorService, [
      props.clusterId,
    ])
      .then((cConnectors: Connector[]) => {
        setLoading(false);
        setConnectors(cConnectors.filter((c) => c !== null));
      })
      .catch((err: React.SetStateAction<Error>) => {
        setApiError(true);
        setErrorMsg(err);
      });
  };

  const taskToRestart = (connName: string, taskId: string) => {
    setConnectorTaskToRestart([connName, taskId]);
  };

  const showConnectorTaskToRestartDialog = () => {
    setCurrentAction(Action.RESTART_TASK);
  };

  const getTaskStates = (conn: Connector) => {
    const taskElements: any = [];
    const statesMap = new Map(Object.entries(conn.taskStates));

    statesMap.forEach((taskState: any, id: string) => {
      taskElements.push(
        <ConnectorTask
          key={id}
          status={taskState.taskStatus}
          connName={conn.name}
          taskId={id}
          errors={taskState.errors}
          i18nTask={t('task')}
          i18nRestart={t('restart')}
          i18nTaskStatusDetail={t('taskStatusDetail')}
          i18nTaskErrorTitle={t('taskErrorTitle')}
          i18nMoreInformation={t('moreInformation')}
          connectorTaskToRestart={taskToRestart}
          showConnectorTaskToRestartDialog={showConnectorTaskToRestartDialog}
        />
      );
    });
    return taskElements;
  };

  const onConnectorDrawer = (c: Connector) => {
    console.log(c);
    setSelectedConnector(c);
  };

  const deselectConnector = () => {
    setSelectedConnector(undefined);
  };

  /**
   * Immediately update the specified connector status in the table
   * @param connName the connector
   * @param status the new status
   */
  const setConnectorStatus = (
    connName: string,
    status: 'PAUSED' | 'RUNNING'
  ) => {
    const updatedRows = [...connectors];
    let doUpdateTable = false;
    for (const conn of updatedRows) {
      if (conn.name === connName && conn.connectorStatus !== status) {
        conn.connectorStatus = status;
        doUpdateTable = true;
        break;
      }
    }
    if (doUpdateTable) {
      updateTableRows(updatedRows, desRowOrder);
    }
  };

  React.useEffect(() => {
    const timeout = setTimeout(
      removeAlert,
      10 * 1000,
      alerts[alerts.length - 1]?.key
    );
    return () => clearTimeout(timeout);
  }, [alerts]);

  React.useEffect(() => {
    const getConnectorsInterval = setInterval(() => getConnectorsList(), 10000);
    return () => clearInterval(getConnectorsInterval);
  }, [currentCategory, props.clusterId]);

  const columns = [
    {
      title: '',
      columnTransforms: [cellWidth(10)],
      cellFormatters: [expandable],
    },
    {
      title: t('name'),
      columnTransforms: [cellWidth(40)],
    },
    {
      title: t('status'),
      columnTransforms: [cellWidth(20)],
    },
    {
      title: t('tasks'),
      columnTransforms: [cellWidth(30)],
    },
  ];

  const sortFieldsItem = [
    { title: t('name'), isPlaceholder: true },
    { title: t('status') },
    { title: t('tasks') },
  ];

  const updateTableRows = (
    conns: Connector[],
    isReverse: boolean,
    sortBy: string = currentCategory
  ) => {
    let sortedConns: Connector[] = [];

    switch (sortBy) {
      case t('status'):
        // Sort connectors by name for the table
        sortedConns = conns.sort((thisConn, thatConn) => {
          return thisConn.name.localeCompare(thatConn.name);
        });
        // Sort connectors by status for the table
        sortedConns = conns.sort((thisConn, thatConn) => {
          return thisConn.connectorStatus.localeCompare(
            thatConn.connectorStatus
          );
        });
        break;
      case t('tasks'):
        // Sort connectors by name for the table
        sortedConns = conns.sort((thisConn, thatConn) => {
          return thisConn.name.localeCompare(thatConn.name);
        });
        // Sort connectors by tasks for the table
        sortedConns = conns.sort((thisConn, thatConn) => {
          return thisConn.taskStates[0].taskStatus.localeCompare(
            thatConn.taskStates[0].taskStatus
          )
            ? -1
            : 1;
        });
        break;
      default:
        // Sort connectors by name for the table
        sortedConns = conns.sort((thisConn, thatConn) => {
          return thisConn.name.localeCompare(thatConn.name);
        });
    }
    if (isReverse) {
      sortedConns = [...sortedConns].reverse();
    }

    // Create table rows
    const rows: any[] = [];
    let counter = 0;
    sortedConns.forEach((conn, index) => {
      const row = {
        isOpen: expandedRows.includes(index * 2),
        cells: [
          {
            title: (
              <ConnectorIcon
                connectorType={
                  conn.connectorType === 'PostgreSQL'
                    ? ConnectorTypeId.POSTGRES
                    : conn.connectorType
                }
                alt={conn.name}
                width={40}
                height={40}
              />
            ),
          },

          {
            title: (
              <a>
                <b
                  data-testid={'connector-name'}
                  onClick={() => onConnectorDrawer(conn)}
                >
                  {conn.name}
                </b>
              </a>
            ),
          },
          {
            title: <ConnectorStatus currentStatus={conn.connectorStatus} />,
          },
          {
            title: <ConnectorTaskState connector={conn} />,
          },
        ],
        connName: conn.name,
        connStatus: conn.connectorStatus,
      };
      const child = {
        parent: counter,
        cells: [
          { title: <div>{''}</div> },
          { title: <div>{''}</div> },
          {
            title: (
              <ConnectorOverview
                i18nOverview={t('overview')}
                i18nMessagePerSec={t('messagePerSec')}
                i18nMaxLagInLastMin={t('maxLagInLastMin')}
                i18nPercentiles={t('percentiles')}
              />
            ),
          },
          {
            title: (
              <Flex>
                <FlexItem style={{ width: '100%' }}>
                  <Flex
                    justifyContent={{ default: 'justifyContentSpaceBetween' }}
                  >
                    <FlexItem flex={{ default: 'flex_1' }}>
                      <Label className="no-bg">
                        <b data-testid="task-id">Task Id</b>
                      </Label>
                    </FlexItem>
                    <FlexItem flex={{ default: 'flex_2' }}>
                      <Label className="no-bg">
                        <b data-testid="task-status">Status</b>
                      </Label>
                    </FlexItem>
                    <FlexItem flex={{ default: 'flex_1' }}>{''}</FlexItem>
                  </Flex>
                  {getTaskStates(conn)}
                </FlexItem>
              </Flex>
            ),
          },
        ],
      };
      rows.push(row);
      rows.push(child);
      counter += 2;
    });
    setTableRows(rows);
  };

  const tableActionResolver = (row: IRowData, extraData: IExtraData) => {
    let returnVal = [] as IAction[];
    returnVal = [
      {
        title: t('pause'),
        onClick: (event: any, rowId: any, rowData: any, extra: any) => {
          setCurrentActionAndName(Action.PAUSE, rowData.connName);
        },
        isDisabled: row.connStatus === 'RUNNING' ? false : true,
      },
      {
        title: t('resume'),
        onClick: (event: any, rowId: any, rowData: any, extra: any) => {
          setCurrentActionAndName(Action.RESUME, rowData.connName);
        },
        isDisabled: row.connStatus === 'PAUSED' ? false : true,
      },
      {
        title: t('restart'),
        onClick: (event: any, rowId: any, rowData: any, extra: any) => {
          setCurrentActionAndName(Action.RESTART, rowData.connName);
        },
        isDisabled:
          row.connStatus === 'UNASSIGNED' || row.connStatus === 'DESTROYED'
            ? true
            : false,
      },
      {
        title: t('view'),
        onClick: (event: any, rowId: any, rowData: any, extra: any) => {
          setCurrentActionAndName(Action.VIEW, rowData.connName, rowData);
        },
        isDisabled:
          row.connStatus === 'UNASSIGNED' || row.connStatus === 'DESTROYED'
            ? true
            : false,
      },
      {
        isSeparator: true,
      },
      {
        title: t('delete'),
        onClick: (event: any, rowId: any, rowData: any, extra: any) => {
          setCurrentActionAndName(Action.DELETE, rowData.connName);
        },
        isDisabled: false,
      },
    ];
    return returnVal;
  };

  const toggleRowOrder = () => {
    updateTableRows(connectors, !desRowOrder);
    setDesRowOrder(!desRowOrder);
  };

  const onSortingSelect = (
    event?: SyntheticEvent<HTMLDivElement, Event> | undefined
  ) => {
    const eventTarget = event?.target as HTMLElement;
    const sortBy = eventTarget.innerText;
    setCurrentCategory(sortBy);
    setIsSortingDropdownOpen(!isSortingDropdownOpen);
    updateTableRows(connectors, desRowOrder, sortBy);
  };

  const onSortingToggle = (isOpen: boolean) => {
    setIsSortingDropdownOpen(isOpen);
  };

  const onCollapse = (event: any, rowKey: number, isOpen: any) => {
    tableRows[rowKey].isOpen = isOpen;
    let updatedExpandedRows = [...expandedRows];
    updatedExpandedRows = isOpen
      ? [...updatedExpandedRows, rowKey]
      : updatedExpandedRows.filter((val) => val !== rowKey);
    setTableRows(tableRows);
    setExpandedRows(updatedExpandedRows);
  };

  const confirmationDialog = () => {
    const shouldShow = currentAction === Action.NONE ? false : true;
    let confirmTitle = '';
    let confirmButtonText = '';
    let confirmMessageText = '';

    switch (currentAction) {
      case Action.DELETE:
        confirmTitle = t('deleteConnector');
        confirmButtonText = t('delete');
        confirmMessageText = t('deleteWarningMsg', {
          connectorName: currentActionConnector,
        });
        break;
      case Action.PAUSE:
        confirmTitle = t('pauseConnector');
        confirmButtonText = t('pause');
        confirmMessageText = t('connectorPauseWarningMsg', {
          connectorName: currentActionConnector,
        });
        break;
      case Action.RESUME:
        confirmTitle = t('resumeConnector');
        confirmButtonText = t('resume');
        confirmMessageText = t('connectorResumeWarningMsg', {
          connectorName: currentActionConnector,
        });
        break;
      case Action.RESTART:
        confirmTitle = t('restartConnector');
        confirmButtonText = t('restart');
        confirmMessageText = t('connectorRestartWarningMsg', {
          connectorName: currentActionConnector,
        });
        break;
      case Action.RESTART_TASK:
        const [connName, connTaskId] = connectorTaskToRestart;
        confirmTitle = t('restartConnectorTask');
        confirmButtonText = t('restart');
        confirmMessageText = t('connectorTaskRestartWarningMsg', {
          connectorName: connName,
          taskId: connTaskId,
        });
        break;
      default:
        break;
    }
    return (
      <ConfirmationDialog
        buttonStyle={ConfirmationButtonStyle.DANGER}
        i18nCancelButtonText={t('cancel')}
        i18nConfirmButtonText={confirmButtonText}
        i18nConfirmationMessage={confirmMessageText}
        i18nTitle={confirmTitle}
        type={ConfirmationType.DANGER}
        showDialog={shouldShow}
        onCancel={resetCurrentAction}
        onConfirm={doPerformCurrentAction}
      />
    );
  };

  const toolbarItems = (
    <React.Fragment>
      <ToolbarContent>
        <ToolbarItem>
          <Dropdown
            onSelect={onSortingSelect}
            position={DropdownPosition.left}
            toggle={
              <DropdownToggle onToggle={onSortingToggle}>
                <FilterIcon size="sm" /> {currentCategory}
              </DropdownToggle>
            }
            isOpen={isSortingDropdownOpen}
            dropdownItems={sortFieldsItem.map((item, index) => (
              <DropdownItem key={index}>{item.title}</DropdownItem>
            ))}
          />
        </ToolbarItem>
        <ToolbarItem>
          {desRowOrder ? (
            <SortAmountDownIcon
              className="connectors-page_toolbarSortIcon"
              size="sm"
              onClick={toggleRowOrder}
            />
          ) : (
            <SortAmountDownAltIcon
              className="connectors-page_toolbarSortIcon"
              size="sm"
              onClick={toggleRowOrder}
            />
          )}
        </ToolbarItem>
      </ToolbarContent>
    </React.Fragment>
  );
  return (
    <WithLoader
      error={apiError}
      loading={loading}
      loaderChildren={<PageLoader />}
      errorChildren={
        <ApiError
          i18nErrorTitle={props.i18nApiErrorTitle}
          i18nErrorMsg={props.i18nApiErrorMsg}
          error={errorMsg}
        />
      }
    >
      {() => (
        <>
          {connectors.length > 0 ? (
            <>
              {confirmationDialog()}
              <ToastAlertComponent
                alerts={alerts}
                removeAlert={removeAlert}
                i18nDetails={t('details')}
              />
              <ConnectorDrawer
                connector={selectedConnector}
                onClose={deselectConnector}
              >
                <Flex className="connectors-page_toolbarFlex flexCol pf-u-box-shadow-sm">
                  <FlexItem>
                    {props.title ? (
                      <Title headingLevel={'h1'}>{t('connectors')}</Title>
                    ) : (
                      ''
                    )}
                    <p>{t('connectorPageHeadingText')}</p>
                  </FlexItem>
                </Flex>
                <Flex className="connectors-page_toolbarFlex">
                  <FlexItem>
                    <Toolbar>{toolbarItems}</Toolbar>
                  </FlexItem>
                  <FlexItem>
                    <Button
                      variant="primary"
                      onClick={createConnector}
                      className="connectors-page_toolbarCreateButton"
                    >
                      {t('createAConnector')}
                    </Button>
                  </FlexItem>
                </Flex>
                <Table
                  aria-label="Connector Table"
                  className="connectors-page_dataTable"
                  cells={columns}
                  rows={tableRows}
                  actionResolver={tableActionResolver}
                  onCollapse={onCollapse}
                >
                  <TableHeader />
                  <TableBody />
                </Table>
              </ConnectorDrawer>
            </>
          ) : (
            <EmptyState variant={EmptyStateVariant.large}>
              <EmptyStateIcon icon={CubesIcon} />
              <Title headingLevel="h4" size="lg">
                {t('noConnectors')}
              </Title>
              <EmptyStateBody>{t('connectorEmptyStateMsg')}</EmptyStateBody>
              <Button
                onClick={createConnector}
                variant="primary"
                className="connectors-page_createButton"
              >
                {t('createAConnector')}
              </Button>
            </EmptyState>
          )}
        </>
      )}
    </WithLoader>
  );
};

export default ConnectorsTableComponent;
