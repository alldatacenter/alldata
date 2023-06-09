import sys
import re

_LOG_PREFIX = [
    "LOG_TRACE", "LOG_DEBUG", "LOG_INFO", "LOG_WARNING", "LOG_ERROR",
    "LOG_FATAL"
]

def check_already_right(line):
  line = line.strip()
  assert line[-2:] == ");"

  i, n = 0, len(line)
  ans = 0
  while i < n:
    if i + 1 < n and line[i: i + 2] == "{}":
      ans += 1
      i += 2
    else:
      i += 1
  # Not process format string has "{}" inside.
  return ans > 0

def line_starts_with_prefix(line):
  line = line.strip()

  for s in _LOG_PREFIX:
    n = len(s)
    if len(line) > n and line[0:n] == s:
      return True
  return False


_PART_TYPE_NONE = False
_PART_TYPE_STRR = 1
_PART_TYPE_ARGS = 2


class StringStream:
  def __init__(self, str):
    self.i_ = 0
    self.str_ = str

  def get_next(self):
    ss = self.str_
    i = self.i_
    n = len(ss)

    # print(i, n, ss[i:])
    # Skip white space first
    while i < n and (ss[i] == ' ' or ss[i] == '\n'):
      i += 1

    if i == n:
      # Done process
      return (_PART_TYPE_NONE, None)

    if ss[i] == '"':
      i += 1
      j = i
      while j < n and ss[j] != '"':
        j += 1
      assert j < n and ss[j] == '"'

      self.i_ = j + 1
      return (_PART_TYPE_STRR, ss[i:j])

    elif ss[i] == '+':
      i += 1
      assert i < n, "Wrong input string {}".format(ss)

      while i < n and (ss[i] == ' ' or ss[i] == '\n'):
        i += 1
      assert i < n, "Wrong input string {}".format(ss)

      if ss[i] == '"':
        self.i_ = i
        return self.get_next()
      else:
        stk = []
        j = i

        while j < n:
          if ss[j] == '(':
            stk.append(j)
          elif ss[j] == ')':
            assert len(stk) > 0, "{}".format(ss)
            stk.pop()
          elif ss[j] == '+':
            if len(stk) == 0:
              break
          elif ss[j] == '<':
            break
          j += 1

        assert j - i + 1 > 0, "Wring input string {}".format(ss)

        self.i_ = j
        return (_PART_TYPE_ARGS, ss[i:j])

    elif ss[i] == '<':
      assert i < n and ss[i + 1] == '<'
      self.i_ = i + 2
      return self.get_next()

    else:
      j = i
      while j < n and ss[j] not in '+<':
        j += 1
      assert j - i + 1 > 0, "Wring input string {}".format(ss)

      self.i_ = j
      return (_PART_TYPE_ARGS, ss[i:j])


def fmt_style(line):
  line = r'{}'.format(line)
  i = 0
  while i < len(line):
    if line[i] == ',':
      break
    i += 1

  ff = line[i + 1:].strip()
  # print(ff)
  assert ff[-2:] == ");", "Wring input strnig: {}".format(line)
  ss = StringStream(ff[0:-2].strip())

  fin = line[0:i + 1]
  fin += ' "'
  args = []
  while 1:
    (t, c) = ss.get_next()
    # print(t, c)
    if t == _PART_TYPE_NONE:
      break
    elif t == _PART_TYPE_ARGS:
      fin += "{}"
      args.append(c.strip())
    else:
      fin += c
  fin += '"'

  for i, x in enumerate(args):
    fin += ", "
    fin += x

  fin += ");"
  return fin


def read_raw_from_file(file):
  rows = []
  with open(file, "r") as f:
    rows = f.read().split('\n')
  return rows[0:-1]


def write_text_to_file(file, rows):
  with open(file, "w") as f:
    for r in rows:
      f.write(r + "\n")


def test_all():
  test1 = '''    LOG_DEBUG(logger, "Successfully opened the manifest store, with latest_version=" << latest_version
                << " commit_version=" << commit_version
                << " checkpoint_version=" << checkpoint_version
                << " time_of_last_commit_log=" << time_of_last_commit_log);
  '''
  test3 = '''LOG_TRACE(log, "some thing...");'''

  x = fmt_style(test1)
  assert x == '    LOG_DEBUG(logger, "Successfully opened the manifest store, with latest_version={} commit_version={} checkpoint_version={} time_of_last_commit_log={}", latest_version, commit_version, checkpoint_version, time_of_last_commit_log);'

  x = fmt_style(test3)
  assert x == 'LOG_TRACE(log, "some thing...");'

  test4 = '''LOG_DEBUG(log, "Wrote block with " << current_block.block.rows() << " rows");'''
  x = fmt_style(test4)
  assert x == 'LOG_DEBUG(log, "Wrote block with {} rows", current_block.block.rows());'

  test2 = '''LOG_WARNING(log_, user_name + ": " + msg + formatSkippedMessage(args...) + " AAAA");'''
  x = fmt_style(test2)
  # print(x)
  assert x == 'LOG_WARNING(log_, "{}: {}{} AAAA", user_name, msg, formatSkippedMessage(args...));'

  test4 = '''LOG_WARNING(log_, "aaa" + ": " + msg + formatSkippedMessage(args...) + " bbb");'''
  x = fmt_style(test4)
  assert x == 'LOG_WARNING(log_, "aaa: {}{} bbb", msg, formatSkippedMessage(args...));'

  test5 = '''LOG_WARNING(log_, "aaa+<>" + "+: " + msg + formatSkippedMessage(args...) + " bbb");'''
  x = fmt_style(test5)
  assert x == 'LOG_WARNING(log_, "aaa+<>+: {}{} bbb", msg, formatSkippedMessage(args...));'

  test6 = '''LOG_WARNING(log_, "a is {}, b is {}", a, b);'''
  assert check_already_right(test6) == True
  x = line_starts_with_prefix(test6)
  assert x == True

  test7 = '''LOG_WARNING(storage.log, "Path " + fullPath(disk, path_to_clone + relative_path) + " already exists. Will remove it and clone again.");'''
  x = fmt_style(test7)
  assert x == 'LOG_WARNING(storage.log, "Path {} already exists. Will remove it and clone again.", fullPath(disk, path_to_clone + relative_path));'

  test8 = '''LOG_DEBUG(log, "aaa " + func(b + f(c, e + d)) + " ffff");'''
  x = fmt_style(test8)
  assert x == 'LOG_DEBUG(log, "aaa {} ffff", func(b + f(c, e + d)));'

  test9 = '''LOG_WARNING(storage.log, "Path {} already exists. Will remove it and clone again.", fullPath(disk, path_to_clone + relative_path));'''
  assert check_already_right(test9) == True

  test10 = '''LOG_WARNING(log, "Error while deleting ZooKeeper path `" << path << "`: " + Coordination::errorMessage(rc) << ", ignoring.");'''
  # print(test10)
  x = fmt_style(test10)
  # print(x)
  assert x == 'LOG_WARNING(log, "Error while deleting ZooKeeper path `{}`: {}, ignoring.", path, Coordination::errorMessage(rc));'

def main(file):
  print("Starting foramt file: {}".format(file))
  rows = read_raw_from_file(file)

  after = []
  n = len(rows)
  i = 0
  while i < n:
    r = rows[i].rstrip()
    if line_starts_with_prefix(r):
      j = i
      while j < n and rows[j][-2:] != ");":
        j += 1
      assert j < n, "Not a valid c/c++ file."

      r = "".join(x for x in rows[i:j + 1])
      if not check_already_right(r):
        r = fmt_style(r)
      i = j

    after.append(r)
    i += 1

  write_text_to_file(file, after)

  print("Finish format file.")


if __name__ == "__main__":
  test_all()
  assert len(sys.argv) >= 2, "Please give me a file to format."
  file = sys.argv[1]
  main(file)
