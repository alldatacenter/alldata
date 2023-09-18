import React, {useState, useMemo} from 'react';
import styled from 'styled-components';
import {Alert, Button, Divider, Empty, message, Modal, Pagination, Typography} from 'antd';
import {DeleteOutlined, InfoCircleOutlined, PlusOutlined} from '@ant-design/icons';
import {red} from '@ant-design/colors';

import {FacetFilterInput} from '../../types.generated';
import {useListAccessTokensQuery, useRevokeAccessTokenMutation} from '../../graphql/auth.generated';
import {Message} from '../shared/Message';
import TabToolbar from '../entity/shared/components/styled/TabToolbar';
import {StyledTable} from '../entity/shared/components/styled/StyledTable';
import CreateTokenModal from './CreateTokenModal';
import {getLocaleTimezone} from '../shared/time/timeUtils';
import {scrollToTop} from '../shared/searchUtils';
import analytics, {EventType} from '../analytics';
import {useUserContext} from '../context/useUserContext';
import {useAppConfig} from '../useAppConfig';

const SourceContainer = styled.div`
    width: 100%;
    padding-top: 20px;
    padding-right: 40px;
    padding-left: 40px;
`;

const TokensContainer = styled.div`
    padding-top: 0px;
`;

const TokensHeaderContainer = styled.div`
    && {
        padding-left: 0px;
    }
`;

const TokensTitle = styled(Typography.Title)`
    && {
        margin-bottom: 8px;
    }
`;

const StyledAlert = styled(Alert)`
    padding-top: 12px;
    padding-bottom: 12px;
    margin-bottom: 20px;
`;

const StyledInfoCircleOutlined = styled(InfoCircleOutlined)`
    margin-right: 8px;
`;

const PersonTokenDescriptionText = styled(Typography.Paragraph)`
    && {
        max-width: 700px;
        margin-top: 12px;
        margin-bottom: 16px;
    }
`;

const ActionButtonContainer = styled.div`
    display: flex;
    justify-content: right;
`;

const PaginationContainer = styled.div`
    display: flex;
    justify-content: center;
`;

const NeverExpireText = styled.span`
    color: ${red[5]};
`;

const DEFAULT_PAGE_SIZE = 10;

export const AccessTokens = () => {
    const [isCreatingToken, setIsCreatingToken] = useState(false);
    const [removedTokens, setRemovedTokens] = useState<string[]>([]);

    // Current User Urn
    const authenticatedUser = useUserContext();
    const currentUserUrn = authenticatedUser?.user?.urn || '';

    const isTokenAuthEnabled = useAppConfig().config?.authConfig?.tokenAuthEnabled;
    const canGeneratePersonalAccessTokens =
        isTokenAuthEnabled && authenticatedUser?.platformPrivileges?.generatePersonalAccessTokens;

    // Access Tokens list paging.
    const [page, setPage] = useState(1);
    const pageSize = DEFAULT_PAGE_SIZE;
    const start = (page - 1) * pageSize;

    // Filters for Access Tokens list
    const filters: Array<FacetFilterInput> = [
        {
            field: 'ownerUrn',
            values: [currentUserUrn],
        },
    ];

    // Call list Access Token Mutation
    const {
        loading: tokensLoading,
        error: tokensError,
        data: tokensData,
        refetch: tokensRefetch,
    } = useListAccessTokensQuery({
        skip: !canGeneratePersonalAccessTokens,
        variables: {
            input: {
                start,
                count: pageSize,
                filters,
            },
        },
    });

    const totalTokens = tokensData?.listAccessTokens.total || 0;
    const tokens = useMemo(() => tokensData?.listAccessTokens.tokens || [], [tokensData]);
    const filteredTokens = tokens.filter((token) => !removedTokens.includes(token.id));

    // Any time a access token  is removed or created, refetch the list.
    const [revokeAccessToken, {error: revokeTokenError}] = useRevokeAccessTokenMutation();

    // Revoke token Handler
    const onRemoveToken = (token: any) => {
        Modal.confirm({
            title: 'Are you sure you want to revoke this token?',
            content: `Anyone using this token will no longer be able to access the DataHub API. You cannot undo this action.`,
            onOk() {
                // Hack to deal with eventual consistency.
                const newTokenIds = [...removedTokens, token.id];
                setRemovedTokens(newTokenIds);

                revokeAccessToken({variables: {tokenId: token.id}})
                    .then(({errors}) => {
                        if (!errors) {
                            analytics.event({type: EventType.RevokeAccessTokenEvent});
                        }
                    })
                    .catch((e) => {
                        message.destroy();
                        message.error({content: `Failed to revoke Token!: \n ${e.message || ''}`, duration: 3});
                    })
                    .finally(() => {
                        setTimeout(() => {
                            tokensRefetch?.();
                        }, 3000);
                    });
            },
            onCancel() {
            },
            okText: 'Yes',
            maskClosable: true,
            closable: true,
        });
    };

    const tableData = filteredTokens?.map((token) => ({
        urn: token.urn,
        type: token.type,
        id: token.id,
        name: token.name,
        description: token.description,
        actorUrn: token.actorUrn,
        ownerUrn: token.ownerUrn,
        createdAt: token.createdAt,
        expiresAt: token.expiresAt,
    }));

    const tableColumns = [
        {
            title: 'Name',
            dataIndex: 'name',
            key: 'name',
            render: (name: string) => <b>{name}</b>,
        },
        {
            title: 'Description',
            dataIndex: 'description',
            key: 'description',
            render: (description: string) => description || '',
        },
        {
            title: 'Expires At',
            dataIndex: 'expiresAt',
            key: 'expiresAt',
            render: (expiresAt: string) => {
                if (expiresAt === null) return <NeverExpireText>Never</NeverExpireText>;
                const localeTimezone = getLocaleTimezone();
                const formattedExpireAt = new Date(expiresAt);
                return (
                    <span>{`${formattedExpireAt.toLocaleDateString()} at ${formattedExpireAt.toLocaleTimeString()} (${localeTimezone})`}</span>
                );
            },
        },
        {
            title: '',
            dataIndex: '',
            key: 'x',
            render: (_, record: any) => (
                <ActionButtonContainer>
                    <Button onClick={() => onRemoveToken(record)} icon={<DeleteOutlined/>} danger>
                        Revoke
                    </Button>
                </ActionButtonContainer>
            ),
        },
    ];

    const onChangePage = (newPage: number) => {
        scrollToTop();
        setPage(newPage);
    };

    return (
        <SourceContainer>
            {tokensLoading && !tokensData && (
                <Message type="loading" content="Loading tokens..." style={{marginTop: '10%'}}/>
            )}
            {tokensError && message.error('Failed to load tokens :(')}
            {revokeTokenError && message.error('Failed to update the Token :(')}
            <TokensContainer>
                <TokensHeaderContainer>
                    <TokensTitle level={2}>管理 Tokens</TokensTitle>
                    <Typography.Paragraph type="secondary">管理DataHubApi的访问令牌。</Typography.Paragraph>
                </TokensHeaderContainer>
            </TokensContainer>
            <Divider/>
            {isTokenAuthEnabled === false && (
                <StyledAlert
                    type="error"
                    message={
                        <span>
                            <StyledInfoCircleOutlined/>
                            基于令牌的身份验证目前被禁用。请与您的DataHub管理员联系以启用此特性。
                        </span>
                    }
                />
            )}
            <Typography.Title level={5}>个人访问令牌</Typography.Title>
            <PersonTokenDescriptionText type="secondary">
                个人访问令牌允许您对datahub的api发出编程请求。他们继承了你的特权。不要共享个人访问令牌。
            </PersonTokenDescriptionText>
            <TabToolbar>
                <div>
                    <Button
                        type="text"
                        onClick={() => setIsCreatingToken(true)}
                        data-testid="add-token-button"
                        disabled={!canGeneratePersonalAccessTokens}
                    >
                        <PlusOutlined/> 创建新的token
                    </Button>
                </div>
            </TabToolbar>
            <StyledTable
                columns={tableColumns}
                dataSource={tableData}
                rowKey="urn"
                locale={{
                    emptyText: <Empty description="No Access Tokens!" image={Empty.PRESENTED_IMAGE_SIMPLE}/>,
                }}
                pagination={false}
            />
            <PaginationContainer>
                <Pagination
                    style={{margin: 40}}
                    current={page}
                    pageSize={pageSize}
                    total={totalTokens}
                    showLessItems
                    onChange={onChangePage}
                    showSizeChanger={false}
                />
            </PaginationContainer>
            <CreateTokenModal
                currentUserUrn={currentUserUrn}
                visible={isCreatingToken}
                onClose={() => setIsCreatingToken(false)}
                onCreateToken={() => {
                    // Hack to deal with eventual consistency.
                    setTimeout(() => {
                        tokensRefetch?.();
                    }, 3000);
                }}
            />
        </SourceContainer>
    );
};
