from __future__ import annotations

import json

import requests
from soda.execution.partition import Partition
from soda.sampler.sample_context import SampleContext
from soda.sampler.sample_ref import SampleRef
from soda.sampler.sampler import Sampler


class HTTPSampler(Sampler):
    def __init__(self, url: str, format: str = "json", link: str | None = None, message: str = "", link_text: str = ""):
        self.url = url
        self.format = format
        self.link = link
        self.message = message
        self.link_text = link_text

    def store_sample(self, sample_context: SampleContext) -> SampleRef | None:
        self.logs.info(f"Sending failed row samples to {self.url}")
        sample_rows = sample_context.sample.get_rows()
        row_count = len(sample_rows)
        sample_schema = sample_context.sample.get_schema()

        result_dict = {
            "schema": sample_schema.get_dict(),
            "count": row_count,
            "rows": sample_rows,
            "datasource": sample_context.sample.data_source.data_source_name,
            "dataset": Partition.get_table_name(sample_context.partition),
            "scan_definition": sample_context.scan._scan_definition_name,
            "check_name": sample_context.check_name,
        }

        response = requests.post(self.url, json=json.dumps(result_dict, sort_keys=True, default=str))

        if response.status_code != 200:
            self.logs.error(f"Unable to upload failed rows to {self.url} -  {response.text}")
        else:
            self.logs.info(f"Uploaded {row_count} failed rows to {self.url}")

        if sample_context.samples_limit is not None:
            stored_row_count = row_count if row_count < sample_context.samples_limit else sample_context.samples_limit
        else:
            stored_row_count = row_count

        return SampleRef(
            name=sample_context.sample_name,
            schema=sample_schema,
            total_row_count=row_count,
            stored_row_count=stored_row_count,
            type=SampleRef.TYPE_NOT_PERSISTED,
            link=self.link,
            message=f"{self.message} {response.text}",
            link_text=self.link_text,
        )
