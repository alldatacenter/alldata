import * as React from 'react';

export interface IWithLoaderProps {
  /**
   * If true, the [errorChildren]{@link IWithLoaderProps#errorChildren} will be
   * rendered.
   */
  error: boolean;
  /**
   * If true, the [loaderChildren]{@link IWithLoaderProps#loaderChildren}
   * will be rendered.
   */
  loading: boolean;
  /**
   * The minimum amount of time to wait before invoking the [children]{@link IWithLoaderProps#children}
   * render prop. If high enough, it will avoid a flash of loading component.
   * Defaults to 500ms.
   */
  minWait?: number;
  /**
   * The content to render when data is loading.
   */
  loaderChildren: JSX.Element;
  /**
   * The content to render when error occurred while loading data.
   */
  errorChildren: JSX.Element;

  /**
   * A render prop that will be invoked if both [loading]{@link IWithLoaderProps#loading}
   * and [error]{@link IWithLoaderProps#error} are `false`.
   */
  children(): any;
}

export interface IWithLoaderState {
  loaded: boolean;
}

/**
 * A component to handle asynchronous data-loading. It will show the provided
 * loader component for a minimum amount of time - to avoid flash of loading
 * component - before invoking the children render prop.
 * It will also handle the error state rendering the provided error component
 * in case of errors loading the data.
 *
 * @see [error]{@link IWithLoaderProps#error}
 * @see [loading]{@link IWithLoaderProps#loading}
 * @see [minWait]{@link IWithLoaderProps#minWait}
 * @see [loaderChildren]{@link IWithLoaderProps#loaderChildren}
 * @see [errorChildren]{@link IWithLoaderProps#errorChildren}
 * @see [children]{@link IWithLoaderProps#children}
 */
export class WithLoader extends React.PureComponent<
  IWithLoaderProps,
  IWithLoaderState
> {
  public static defaultProps = {
    minWait: 500,
  };

  public isCompMounted = false;

  protected waitTimeout?: number;

  constructor(props: IWithLoaderProps) {
    super(props);
    this.state = {
      loaded: !this.props.loading,
    };
  }

  // tslint:disable-next-line
  static getDerivedStateFromProps(props: IWithLoaderProps) {
    return {
      loaded: !props.loading,
    };
  }

  public componentDidUpdate(prevProps: IWithLoaderProps) {
    this.isCompMounted = true;
    if (!this.props.loading && !this.waitTimeout) {
      this.setTimeout();
    }
  }

  public componentWillUnmount() {
    this.isCompMounted = false;
  }

  public render() {
    if (this.props.error) {
      return this.props.errorChildren;
    }
    if (this.props.loading) {
      return this.props.loaderChildren;
    }
    return this.props.children();
  }

  protected setTimeout() {
    this.clearTimeout();
    this.waitTimeout = window.setTimeout(() => {
      if (this.isCompMounted) {
        this.setState({
          loaded: true,
        });
      }
    }, this.props.minWait!);
  }

  protected clearTimeout() {
    if (this.waitTimeout) {
      clearTimeout(this.waitTimeout);
      this.waitTimeout = undefined;
    }
  }
}
