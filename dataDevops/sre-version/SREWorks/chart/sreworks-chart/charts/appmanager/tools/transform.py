# coding: utf-8

import json


def transform(item):
    if not item:
        return
    elif isinstance(item, dict):
        if 'description' in item:
            del item['description']
        for key in item:
            transform(item[key])
    elif isinstance(item, list):
        for i in item:
            transform(i)


def run():
    with open('microservice.json') as f:
        content = json.loads(f.read())
    transform(content)
    with open('microservice-simple.json', 'w') as f:
        f.write(json.dumps(content, ensure_ascii=False, indent=4))


if __name__ == '__main__':
    run()
