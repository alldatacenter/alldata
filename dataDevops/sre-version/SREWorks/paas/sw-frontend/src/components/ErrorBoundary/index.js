/**
 * Created by caoshuaibiao on 2020/10/10.
 * 通用错误边界
 */
import React, { Component } from 'react';
import { Layout, Alert, Button, Spin } from 'antd';

export default class ErrorBoundary extends React.Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false, errorInfo: '', error: '' };
    }

    static getDerivedStateFromError(error) {
        return { hasError: true };
    }

    componentDidCatch(error, errorInfo) {
        this.setState({ error, errorInfo })
    }

    render() {
        let { hasError, errorInfo, error } = this.state;
        if (hasError) {
            return (
                <Alert
                    message="Warning"
                    description={error + ""}
                    type="warning"
                    showIcon
                />)

        }
        return this.props.children;
    }
}