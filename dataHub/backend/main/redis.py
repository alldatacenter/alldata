import json
import redis
import logging
from django.conf import settings

logger = logging.getLogger("app.main.redis")

pool = redis.ConnectionPool(
    host=settings.REDIS_HOST,
    port=settings.REDIS_PORT,
    db=settings.REDIS_DB,
    decode_responses=True,
)
client = redis.Redis(connection_pool=pool)


def push_to_mails(subject, message, to):
    """
    将邮件内容推到 redis 邮件队列
    """
    content = json.dumps(
        {
            "subject": subject,
            "message": message,
            "to": to,
        }
    )

    logger.info(f"send mail: {content}")

    try:
        if not to:
            raise ValueError("email is invalid")
        client.rpush("mails", content)
        logger.info("send mail success")
    except Exception as e:
        logger.error(f"send mail error: {e}")
