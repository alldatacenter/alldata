import base64
import logging
import requests
import rsa
from typing import Any, Mapping, Optional
from fastapi import HTTPException, Request, status
from fastapi.security import OAuth2AuthorizationCodeBearer
import jwt
from jwt.exceptions import ExpiredSignatureError, PyJWKError

from rbac import config
from rbac.models import User, UserType


log = logging.getLogger()
BEARER_TOKEN = "BEARER "


class InvalidAuthorization(HTTPException):
    def __init__(self, detail: Any = None) -> None:
        super().__init__(status_code=status.HTTP_401_UNAUTHORIZED,
                         detail=detail, headers={"WWW-Authenticate": "Bearer"})


class AzureADAuth(OAuth2AuthorizationCodeBearer):
    # cached AAD jwt keys
    aad_jwt_keys_cache: dict = {}

    def __init__(self, aad_instance: str = config.RBAC_AAD_INSTANCE, aad_tenant: str = config.RBAC_AAD_TENANT_ID):
        self.base_auth_url: str = f"{aad_instance}/{aad_tenant}"
        super(AzureADAuth, self).__init__(
            authorizationUrl=f"{self.base_auth_url}/oauth2/v2.0/authorize",
            tokenUrl=f"{self.base_auth_url}/oauth2/v2.0/token",
            refreshUrl=f"{self.base_auth_url}/oauth2/v2.0/token",
            scheme_name="oauth2",
            scopes={"https://graph.microsoft.com/User.Read": "User.Read"},
        )

    async def __call__(self, request: Request) -> User:
        bearer_token: str = request.headers.get("authorization")
        if bearer_token:
            token = bearer_token[len(BEARER_TOKEN):]
            decoded_token = self._decode_token(token)
            return self._get_user_from_token(decoded_token)
        else:
            raise InvalidAuthorization(
                detail='No authorization token was found')

    @staticmethod
    def _get_user_from_token(decoded_token: Mapping) -> User:
        try:
            user_id = decoded_token['sub']
        except Exception as e:
            raise InvalidAuthorization(
                detail=f'Unable to extract subject as unique id from token, {e}')

        aad_user_key = "preferred_username"
        aad_app_key = "appid"
        common_user_key = "email"
        name = decoded_token.get("name", "")

        if aad_user_key in decoded_token:
            # Id Tokens from browser
            username = decoded_token.get(aad_user_key)
            type = UserType.AAD_USER
        elif aad_app_key in decoded_token:
            appid = decoded_token.get(aad_app_key)
            # Azure CLI User Impersonation token
            if decoded_token.get("scp") == str(UserType.USER_IMPERSONATION.value):
                if "upn" in decoded_token:
                    username = decoded_token.get("upn")
                # live.com account token doesn't have upn
                else:
                    username = decoded_token.get("email")
                type = UserType.USER_IMPERSONATION
            # Other AAD App token
            else:
                username = appid
                name = decoded_token.get("app_displayname", "")
                type = UserType.AAD_APP
        elif common_user_key in decoded_token:
            username = decoded_token.get(common_user_key)
            type = UserType.COMMON_USER
        else:
            log.debug(f"unknown user type {decoded_token}")
            username = user_id
            type = UserType.UNKNOWN

        log.info(
            f"username: {username}, name: {name}, token type: {str(type)} ")
        return User(
            id=user_id,
            name=name,
            username=username,
            type=type,
            roles=decoded_token.get('roles', [])
        )

    @staticmethod
    def _get_key_id(token: str) -> Optional[str]:
        headers = jwt.get_unverified_header(token)
        return headers['kid'] if headers and 'kid' in headers else None

    @staticmethod
    def _ensure_b64padding(key: str) -> str:
        """
        The base64 encoded keys are not always correctly padded, so pad with the right number of =
        """
        key = key.encode('utf-8')
        missing_padding = len(key) % 4
        for _ in range(missing_padding):
            key = key + b'='
        return key

    def _cache_aad_keys(self) -> None:
        """
        Cache all AAD JWT keys - so we don't have to make a web call each auth request
        """
        response = requests.get(
            f"{self.base_auth_url}/v2.0/.well-known/openid-configuration")
        aad_metadata = response.json() if response.ok else None
        jwks_uri = aad_metadata['jwks_uri'] if aad_metadata and 'jwks_uri' in aad_metadata else None
        if jwks_uri:
            response = requests.get(jwks_uri)
            keys = response.json() if response.ok else None
            if keys and 'keys' in keys:
                for key in keys['keys']:
                    n = int.from_bytes(base64.urlsafe_b64decode(
                        self._ensure_b64padding(key['n'])), "big")
                    e = int.from_bytes(base64.urlsafe_b64decode(
                        self._ensure_b64padding(key['e'])), "big")
                    pub_key = rsa.PublicKey(n, e)
                    # Cache the PEM formatted public key.
                    AzureADAuth.aad_jwt_keys_cache[key['kid']] = pub_key.save_pkcs1(
                    )

    def _get_token_key(self, key_id: str) -> str:
        if key_id not in AzureADAuth.aad_jwt_keys_cache:
            self._cache_aad_keys()
        return AzureADAuth.aad_jwt_keys_cache[key_id]

    def _decode_token(self, token: str) -> Mapping:
        key_id = self._get_key_id(token)
        if not key_id:
            raise InvalidAuthorization(
                f'The token does not contain kid: {token}')
        key = self._get_token_key(key_id)
        try:
            decode = jwt.decode(token, key=key, algorithms=[
                                'RS256'], audience=["https://management.azure.com", config.RBAC_API_AUDIENCE])
            return decode
        except ExpiredSignatureError as e:
            raise InvalidAuthorization(f'The token signature has expired: {e}')
        except PyJWKError as e:
            raise InvalidAuthorization(f'The token is invalid: {e}')
        except Exception as e:
            raise InvalidAuthorization(f'Unable to decode token, error: {e}')


authorize = AzureADAuth()
