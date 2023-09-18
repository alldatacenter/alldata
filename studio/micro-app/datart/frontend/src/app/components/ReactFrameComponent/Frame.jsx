import PropTypes from 'prop-types';
import React, { Component } from 'react';
import ReactDOM from 'react-dom';
import Content from './Content';
import { FrameContextProvider } from './Context';

export class Frame extends Component {
  // React warns when you render directly into the body since browser extensions
  // also inject into the body and can mess up React. For this reason
  // initialContent is expected to have a div inside of the body
  // element that we render react into.
  static propTypes = {
    style: PropTypes.object,
    head: PropTypes.node,
    mountTarget: PropTypes.string,
    contentDidMount: PropTypes.func,
    contentDidUpdate: PropTypes.func,
    children: PropTypes.oneOfType([
      PropTypes.element,
      PropTypes.arrayOf(PropTypes.element),
    ]),
  };

  static defaultProps = {
    style: {},
    head: null,
    children: undefined,
    mountTarget: undefined,
    contentDidMount: () => {},
    contentDidUpdate: () => {},
  };

  constructor(props, context) {
    super(props, context);
    this._isMounted = false;
    this.nodeRef = React.createRef();
    this.state = { iframeLoaded: false };
  }

  componentDidMount() {
    this._isMounted = true;
    const doc = this.getDoc();
    if (doc && doc.readyState === 'complete') {
      this.forceUpdate();
    } else {
      this.nodeRef.current.addEventListener('load', this.handleLoad);
    }
  }

  componentWillUnmount() {
    this._isMounted = false;
    this.nodeRef.current.removeEventListener('load', this.handleLoad);
  }

  getDoc() {
    return this.nodeRef.current ? this.nodeRef.current.contentDocument : null; // eslint-disable-line
  }

  getMountTarget() {
    const doc = this.getDoc();
    if (this.props.mountTarget) {
      return doc.querySelector(this.props.mountTarget);
    }
    return doc.body;
  }

  setRef = node => {
    this.nodeRef.current = node;
    if (!this.nodeRef.current?.contentWindow) {
      return;
    }
  };

  handleLoad = () => {
    this.setState({ iframeLoaded: true });
  };

  renderFrameContents() {
    if (!this._isMounted) {
      return null;
    }
    const doc = this.getDoc();
    const contentDidMount = this.props.contentDidMount;
    const contentDidUpdate = this.props.contentDidUpdate;

    const win = doc.defaultView || doc.parentView;
    const contents = (
      <Content
        contentDidMount={contentDidMount}
        contentDidUpdate={contentDidUpdate}
      >
        <FrameContextProvider value={{ document: doc, window: win }}>
          <div className="frame-content">{this.props.children}</div>
        </FrameContextProvider>
      </Content>
    );

    const mountTarget = this.getMountTarget();
    const res = [
      ReactDOM.createPortal(this.props.head, this.getDoc().head),
      ReactDOM.createPortal(contents, mountTarget),
    ];

    return res;
  }

  render() {
    const props = {
      ...this.props,
      children: undefined, // The iframe isn't ready so we drop children from props here. #12, #17
    };

    delete props.head;
    /**
     * Because ios wechat browser issue which not support srcDoc props
     * we remove this props and also to avoid performance issue with
     * document.write function, but PR is still welcome!
     */
    delete props.srcDoc;
    delete props.mountTarget;
    delete props.contentDidMount;
    delete props.contentDidUpdate;
    return (
      <iframe
        title={props?.id}
        {...props}
        ref={this.setRef}
        onLoad={this.handleLoad}
      >
        {this.renderFrameContents()}
      </iframe>
    );
  }
}
