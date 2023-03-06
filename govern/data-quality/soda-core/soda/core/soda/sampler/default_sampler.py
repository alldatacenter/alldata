from __future__ import annotations

import logging

from soda.sampler.sample_context import SampleContext
from soda.sampler.sample_ref import SampleRef
from soda.sampler.sampler import Sampler

logger = logging.getLogger(__name__)


class DefaultSampler(Sampler):
    def store_sample(self, sample_context: SampleContext) -> SampleRef:
        self.logs.info("Using DefaultSampler")
        sample_rows = sample_context.sample.get_rows()
        row_count = len(sample_rows)

        sample_schema = sample_context.sample.get_schema()

        return SampleRef(
            name=sample_context.sample_name,
            schema=sample_schema,
            total_row_count=row_count,
            stored_row_count=row_count,
            type=SampleRef.TYPE_NOT_PERSISTED,
            message="Samples are not sent to Soda Cloud",
        )
