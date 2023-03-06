from typing import Optional

import boto3


class AwsCredentials:
    def __init__(
        self,
        access_key_id: Optional[str] = None,
        secret_access_key: Optional[str] = None,
        role_arn: Optional[str] = None,
        session_token: Optional[str] = None,
        profile_name: Optional[str] = None,
        region_name: Optional[str] = "eu-west-1",
    ):
        self.access_key_id = access_key_id
        self.secret_access_key = secret_access_key
        self.role_arn = role_arn
        self.session_token = session_token
        self.profile_name = profile_name
        self.region_name = region_name

    @classmethod
    def from_configuration(cls, configuration: dict):
        """
        An AwsCredentials if there is an access_key_id specified, None otherwise
        """
        access_key_id = configuration.get("access_key_id")
        if not isinstance(access_key_id, str):
            return None
        return AwsCredentials(
            access_key_id=access_key_id,
            secret_access_key=configuration.get("secret_access_key"),
            role_arn=configuration.get("role_arn"),
            session_token=configuration.get("session_token"),
            profile_name=configuration.get("profile_name"),
            region_name=configuration.get("region", "eu-west-1"),
        )

    def resolve_role(self, role_session_name: str):
        if self.has_role():
            return self.assume_role(role_session_name)
        return self

    def has_role(self):
        return isinstance(self.role_arn, str)

    def assume_role(self, role_session_name: str):
        aws = boto3.session.Session(profile_name=self.profile_name) if self.profile_name else boto3
        self.sts_client = aws.client(
            "sts",
            region_name=self.region_name,
            aws_access_key_id=self.access_key_id,
            aws_secret_access_key=self.secret_access_key,
            aws_session_token=self.session_token,
        )

        assumed_role_object = self.sts_client.assume_role(RoleArn=self.role_arn, RoleSessionName=role_session_name)
        credentials_dict = assumed_role_object["Credentials"]
        return AwsCredentials(
            region_name=self.region_name,
            access_key_id=credentials_dict["AccessKeyId"],
            secret_access_key=credentials_dict["SecretAccessKey"],
            session_token=credentials_dict["SessionToken"],
        )
