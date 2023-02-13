interface IHeadlessBrowserIdentifierProps {
  renderSign: boolean;
  width: number;
  height: number;
}

export function HeadlessBrowserIdentifier({
  renderSign,
  width,
  height,
}: IHeadlessBrowserIdentifierProps) {
  if (!renderSign) {
    return <span />;
  } else {
    return (
      <>
        <input id="headlessBrowserRenderSign" type="hidden" />
        <input id="width" type="hidden" value={width} />
        <input id="height" type="hidden" value={height} />
      </>
    );
  }
}
