interface MetricSnapshotProps {
  snapshot?: MetricSnapshot;
  format?: MetricSnapshotFormat;
}

type MetricSnapshotFormat = (percentage: number) => string
