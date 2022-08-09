"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

"""


def get_property_value(dictionary, property_name, default_value=None, trim_string=False,
                       empty_value=""):
  """
  Get a property value from a dictionary, applying applying rules as necessary.

  If dictionary does not contain a value for property_name or the value for property_name is None,
  null_value is used as the value to return.  Then, if trim_string is True and the value is None
  or the value is an empty string, empty_value will be return else the (current) value is returned.

  Note: the property value will most likely be a string or a unicode string, however in the event
  it is not (for example a number), this method will behave properly and return the value as is.

  :param dictionary: a dictionary of values
  :param property_name: the name of a dictionary item to retrieve
  :param default_value: the value to use if the item is not in the dictionary or the value of the item is None
  :param trim_string: a Boolean value indicating whether to strip whitespace from the value (True) or not (False)
  :param empty_value: the value to use if the (current) value is None or an empty string, if trim_string is True
  :return: the requested property value with rules applied
  """
  # If property_name is not in the dictionary, set value to null_value
  if property_name in dictionary:
    value = dictionary[property_name]
    if value is None:
      value = default_value
  else:
    value = default_value

  if trim_string:
    # If the value is none, consider it empty...
    if value is None:
      value = empty_value
    elif (type(value) == str) or (type(value) == unicode):
      value = value.strip()

      if len(value) == 0:
        value = empty_value

  return value


def get_unstructured_data(dictionary, property_name):
  prefix = property_name + '/'
  prefix_len = len(prefix)
  return dict((k[prefix_len:], v) for k, v in dictionary.iteritems() if k.startswith(prefix))


def split_host_and_port(host):
  """
  Splits a string into its host and port components

  :param host: a string matching the following pattern: <host name | ip address>[:port]
  :return: a Dictionary containing 'host' and 'port' entries for the input value
  """

  if host is None:
    host_and_port = None
  else:
    host_and_port = {}
    parts = host.split(":")

    if parts is not None:
      length = len(parts)

      if length > 0:
        host_and_port['host'] = parts[0]

        if length > 1:
          host_and_port['port'] = int(parts[1])

  return host_and_port


def set_port(host, port):
  """
  Sets the port for a host specification, potentially replacing an existing port declaration

  :param host: a string matching the following pattern: <host name | ip address>[:port]
  :param port: a string or integer declaring the (new) port
  :return: a string declaring the new host/port specification
  """
  if port is None:
    return host
  else:
    host_and_port = split_host_and_port(host)

    if (host_and_port is not None) and ('host' in host_and_port):
      return "%s:%s" % (host_and_port['host'], port)
    else:
      return host
