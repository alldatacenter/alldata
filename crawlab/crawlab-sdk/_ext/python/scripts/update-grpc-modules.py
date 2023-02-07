import os
import re
from os.path import abspath, join, dirname, curdir, basename

cur_dir_path = abspath(__file__).replace(basename(__file__), '')
grpc_dir_path = abspath(join(cur_dir_path, '..', 'crawlab', 'grpc'))

regexp_pattern = re.compile(r'(\n|^)from (entity) import')


def main():
    for item in os.walk(grpc_dir_path):
        dir_path, dir_names, file_names = item
        for file_name in file_names:
            if not file_name.endswith('.py'):
                continue
            file_path = join(dir_path, file_name)
            with open(file_path, 'rb') as f:
                file_data = f.read()
                file_content = file_data.decode('utf-8')
            replaced_file_content = regexp_pattern.sub(r'\1from crawlab.grpc.\2 import', file_content)
            if replaced_file_content == file_content:
                continue
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(replaced_file_content)


if __name__ == '__main__':
    main()
