import React from 'react';
import PropTypes from 'prop-types';
import UtilFn from 'js/util.js';
import { Button } from 'antd';

class DataShow extends React.Component {
    constructor(props) {
        super(props);
    }

    syntaxHighlight(json) {
        if (typeof json == 'string' && this.props.type == 'string') { // 只是字符串
            json = json.replace(/\n/g, '</br>');
            return '<span class="string">' + json + '</span>';
        }

        if (typeof json == 'string') {
            json = JSON.parse(json);
        }
        if (typeof json != 'string') {
            json = JSON.stringify(json, undefined, 2);
        }
        json = json.replace(/&/g, '&').replace(/</g, '<').replace(/>/g, '>').replace(/\\n/g, '</br>');
        return json.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function(match) {
            var cls = 'number';
            if (/^"/.test(match)) {
                if (/:$/.test(match)) {
                    cls = 'key';
                } else {
                    cls = 'string';
                }
            } else if (/true|false/.test(match)) {
                cls = 'boolean';
            } else if (/null/.test(match)) {
                cls = 'null';
            }
            return '<span class="' + cls + '">' + match + '</span>';
        });
    }

    render() {
        const data = this.props.data;
        return (
            <div style={this.props.style || {}}>
                <pre className="pre_css" dangerouslySetInnerHTML={{ __html: this.syntaxHighlight(data) }} />
            </div>
        );
    }
}

export default DataShow;