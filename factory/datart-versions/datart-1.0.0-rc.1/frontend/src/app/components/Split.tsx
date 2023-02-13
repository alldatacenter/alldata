import React, { ReactElement } from 'react';
import Split from 'split.js';

interface SplitWrapperProps {
  sizes?: number[];
  minSize?: number | number[];
  maxSize?: number | number[];
  expandToMin?: boolean;
  gutterSize?: number;
  gutterAlign?: string;
  snapOffset?: number;
  dragInterval?: number;
  direction?: 'horizontal' | 'vertical';
  cursor?: string;
  gutter?: (index, direction, pairElement?) => HTMLElement;
  elementStyle?: (dimension, elementSize, gutterSize, index) => object;
  gutterStyle?: (dimension, gutterSize, index) => object;
  onDrag?: (sizes) => void;
  onDragStart?: (sizes) => void;
  onDragEnd?: (sizes) => void;
  collapsed?: number;
  children?: ReactElement[];
  className?: string;
}

class SplitWrapper extends React.Component<SplitWrapperProps> {
  private split?: Split.Instance;
  private parent: { children: HTMLElement[] } | null = null;

  componentDidMount() {
    const { children, gutter, ...options } = this.props;

    const updatedGutter = (index, direction) => {
      let gutterElement;

      if (gutter) {
        gutterElement = gutter(index, direction);
      } else {
        gutterElement = document.createElement('div');
        gutterElement.className = `gutter gutter-${direction}`;
      }

      // eslint-disable-next-line no-underscore-dangle
      gutterElement.__isSplitGutter = true;
      return gutterElement;
    };

    this.split = Split(this.parent!.children, {
      ...options,
      gutter: updatedGutter,
    });
  }

  componentDidUpdate(prevProps) {
    const { children, minSize, sizes, collapsed, ...options } = this.props;
    const {
      minSize: prevMinSize,
      sizes: prevSizes,
      collapsed: prevCollapsed,
    } = prevProps;

    const otherProps = [
      'maxSize',
      'expandToMin',
      'gutterSize',
      'gutterAlign',
      'snapOffset',
      'dragInterval',
      'direction',
      'cursor',
      'onDrag',
      'onDragStart',
      'onDragEnd',
    ];

    let needsRecreate = otherProps
      // eslint-disable-next-line react/destructuring-assignment
      .map(prop => this.props[prop] !== prevProps[prop])
      .reduce((accum, same) => accum || same, false);

    // Compare minSize when both are arrays, when one is an array and when neither is an array
    if (Array.isArray(minSize) && Array.isArray(prevMinSize)) {
      let minSizeChanged = false;

      minSize.forEach((minSizeI, i) => {
        minSizeChanged = minSizeChanged || minSizeI !== prevMinSize[i];
      });

      needsRecreate = needsRecreate || minSizeChanged;
    } else if (Array.isArray(minSize) || Array.isArray(prevMinSize)) {
      needsRecreate = true;
    } else {
      needsRecreate = needsRecreate || minSize !== prevMinSize;
    }

    // Destroy and re-create split if options changed
    if (needsRecreate) {
      this.split?.destroy(true, true);
      options.gutter = (index, direction, pairB) => pairB.previousSibling;
      this.split = Split(
        Array.from(this.parent!.children).filter(
          // eslint-disable-next-line no-underscore-dangle
          element => !(element as any).__isSplitGutter,
        ),
        { ...options, minSize, sizes: sizes || this.split?.getSizes() },
      );
    } else if (sizes) {
      // If only the size has changed, set the size. No need to do this if re-created.
      let sizeChanged = false;

      sizes.forEach((sizeI, i) => {
        sizeChanged = sizeChanged || sizeI !== prevSizes[i];
      });

      if (sizeChanged) {
        // eslint-disable-next-line react/destructuring-assignment
        this.split?.setSizes(this.props.sizes!);
      }
    }

    // Collapse after re-created or when collapsed changed.
    if (
      Number.isInteger(collapsed) &&
      (collapsed !== prevCollapsed || needsRecreate)
    ) {
      this.split?.collapse(collapsed!);
    }
  }

  componentWillUnmount() {
    if (this.split) {
      this.split.destroy();
      delete this.split;
    }
  }

  render() {
    const {
      sizes,
      minSize,
      maxSize,
      expandToMin,
      gutterSize,
      gutterAlign,
      snapOffset,
      dragInterval,
      direction,
      cursor,
      gutter,
      elementStyle,
      gutterStyle,
      onDrag,
      onDragStart,
      onDragEnd,
      collapsed,
      children,
      ...rest
    } = this.props;

    return (
      <div
        ref={parent => {
          this.parent = parent as any;
        }}
        {...rest}
      >
        {children}
      </div>
    );
  }
}

export default SplitWrapper;
