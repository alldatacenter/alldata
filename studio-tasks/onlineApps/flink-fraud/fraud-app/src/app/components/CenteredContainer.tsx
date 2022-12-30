import React, { forwardRef, ReactNode, CSSProperties } from "react";
import { Card } from "reactstrap";
import cx from "classnames";

export const CenteredContainer = forwardRef((props: Props, ref) => {
  return (
    <Card
      innerRef={ref}
      className={cx(["w-100 overflow-hidden flex-shrink-0", props.tooManyItems ? "my-3" : "my-auto", props.className])}
      style={props.style}
    >
      {props.children}
    </Card>
  );
});

interface Props {
  tooManyItems: boolean;
  children: ReactNode;
  className?: string;
  style?: CSSProperties;
}
