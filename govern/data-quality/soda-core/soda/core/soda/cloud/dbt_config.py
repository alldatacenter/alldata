from __future__ import annotations


class DbtCloudConfig:
    def __init__(
        self,
        api_token: str | None,
        account_id: str | None,
        api_url: str | None = "https://cloud.getdbt.com/api/v2/accounts/",
    ):
        self.api_token = api_token
        self.account_id = account_id
        self.api_url = api_url
