import React from 'react';
import {render, waitFor, fireEvent} from '@testing-library/react';
import {MockedProvider} from '@apollo/client/testing';
import {HomePage} from '../HomePage';
import {mocks} from '../../../Mocks';
import TestPageContainer from '../../../utils/test-utils/TestPageContainer';

describe('HomePage', () => {
    it('renders', async () => {
        const {getByTestId} = render(
            <MockedProvider
                mocks={mocks}
                addTypename={false}
                defaultOptions={{
                    watchQuery: {fetchPolicy: 'no-cache'},
                    query: {fetchPolicy: 'no-cache'},
                }}
            >
                <TestPageContainer>
                    <HomePage/>
                </TestPageContainer>
            </MockedProvider>,
        );
        await waitFor(() => expect(getByTestId('search-input')).toBeInTheDocument());
    });

    it('查看可浏览的实体', async () => {
        const {getByText} = render(
            <MockedProvider
                mocks={mocks}
                addTypename={false}
                defaultOptions={{
                    watchQuery: {fetchPolicy: 'no-cache'},
                    query: {fetchPolicy: 'no-cache'},
                }}
            >
                <TestPageContainer>
                    <HomePage/>
                </TestPageContainer>
            </MockedProvider>,
        );
        await waitFor(() => expect(getByText('Datasets')).toBeInTheDocument());
    });

    it('查看自动完成结果', async () => {
        const {getByTestId, queryAllByText} = render(
            <MockedProvider
                mocks={mocks}
                addTypename={false}
                defaultOptions={{
                    watchQuery: {fetchPolicy: 'no-cache'},
                    query: {fetchPolicy: 'no-cache'},
                }}
            >
                <TestPageContainer>
                    <HomePage/>
                </TestPageContainer>
            </MockedProvider>,
        );
        const searchInput = getByTestId('search-input');
        await waitFor(() => expect(searchInput).toBeInTheDocument());
        fireEvent.change(searchInput, {target: {value: 't'}});

        await waitFor(() => expect(queryAllByText('大量的测试数据集').length).toBeGreaterThanOrEqual(1));
        expect(queryAllByText('其他数据集').length).toBeGreaterThanOrEqual(1);
    });

    it('renders search suggestions', async () => {
        const {getByText, queryAllByText} = render(
            <MockedProvider
                mocks={mocks}
                addTypename
                defaultOptions={{
                    watchQuery: {fetchPolicy: 'no-cache'},
                    query: {fetchPolicy: 'no-cache'},
                }}
            >
                <TestPageContainer>
                    <HomePage/>
                </TestPageContainer>
            </MockedProvider>,
        );
        await waitFor(() => expect(getByText('尝试搜索')).toBeInTheDocument());
        expect(queryAllByText('另一个数据集').length).toBeGreaterThanOrEqual(1);
        expect(queryAllByText('第四个测试数据集').length).toBeGreaterThanOrEqual(1);
    });
});
