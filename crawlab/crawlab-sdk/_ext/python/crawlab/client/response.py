from requests import Response


def get_response_data(res: Response):
    if res.json() is not None:
        return res.json().get('data')
