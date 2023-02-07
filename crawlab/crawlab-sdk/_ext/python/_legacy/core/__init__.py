import os

home = os.curdir

if 'HOME' in os.environ:
    home = os.environ['HOME']
elif os.name == 'posix':
    home = os.path.expanduser('~/')
elif os.name == 'nt':
    if 'HOMEPATH' in os.environ and 'HOMEDRIVE' in os.environ:
        home = os.environ['HOMEDRIVE'] + os.environ['HOMEPATH']
elif 'HOMEPATH' in os.environ:
    home = os.environ['HOMEPATH']

CRAWLAB_ROOT = os.path.join(home, '.crawlab')
CRAWLAB_TMP = os.path.join(CRAWLAB_ROOT, 'tmp')

if not os.path.exists(CRAWLAB_ROOT):
    os.mkdir(CRAWLAB_ROOT)

if not os.path.exists(CRAWLAB_TMP):
    os.mkdir(CRAWLAB_TMP)
