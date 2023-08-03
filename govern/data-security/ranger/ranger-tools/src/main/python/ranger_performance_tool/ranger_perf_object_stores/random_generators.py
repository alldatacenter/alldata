#!/usr/bin/env python

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import random
import string


def get_random_generator(random_type):
    """
    Returns the random generator based on random_type defines in secondary config file
    :param random_type: str , type of random generator, supported values are "random", "random_int", "random_string"
    :return: generator: object , random generator object corresponding to random_type
    """
    if random_type == "increasing":
        generator = RandomGeneratorIncreasingSize()
    elif random_type == "random":
        generator = RandomGeneratorRandom()
    elif random_type == "incremental":
        generator = RandomGeneratorIncremental()
    else:
        raise Exception(f"Invalid type of Random Generator: {type}")
    return generator


class RandomGeneratorIncreasingSize:
    """
    One of the supported generators with size of object increasing with each call to generator functions

    Attributes
    ----------
    int_counter : int
        counter for generating int values
    string_length : int
        length of the string to be generated
    array_length : int
        length of the array to be generated

    Methods
    -------
    generate_int()
        generates int value
    generate_string()
        generates string value of length string_length
    generate_array()
        generates array of length array_length
    generate_string_list_2d(num_rows)
        generates 2d array of strings of size num_rows x array_length
    generate_string_list_2d(num_rows, num_cols)
        generates 2d array of strings of size num_rows x num_cols
    """

    LARGE_CONST = 100

    def __init__(self):
        self.int_counter = 0
        self.string_length = 1
        self.array_length = 1

    def generate_int(self):
        self.int_counter += 1
        return self.int_counter

    def generate_string(self):
        self.string_length += RandomGeneratorIncreasingSize.LARGE_CONST
        return ''.join(random.choices(string.ascii_letters, k=self.string_length))

    def generate_array(self):
        self.array_length += RandomGeneratorIncreasingSize.LARGE_CONST
        return [self.generate_string() for _ in range(self.array_length)]

    def generate_string_list_2d(self, num_rows):
        self.array_length += RandomGeneratorIncreasingSize.LARGE_CONST
        return [[self.generate_string() for _ in range(self.array_length)] for _ in range(num_rows)]

    def generate_string_list_2d(self, num_rows, num_cols):
        self.array_length += RandomGeneratorIncreasingSize.LARGE_CONST
        return [[self.generate_string() for _ in range(num_cols)] for _ in range(num_rows)]


class RandomGeneratorIncremental:
    """
    One of the supported generators with size of object increasing by 1 with each call to generator functions

    Attributes
    ----------
    int_counter : int
        counter for generating int values
    string_length : int
        length of the string to be generated
    array_length : int
        length of the array to be generated

    Methods
    -------
    generate_int()
        generates int value
    generate_string()
        generates string value of length string_length
    generate_array()
        generates array of length array_length
    generate_string_list_2d(num_rows)
        generates 2d array of strings of size num_rows x array_length
    generate_string_list_2d(num_rows, num_cols)
        generates 2d array of strings of size num_rows x num_cols
    """
    def __init__(self):
        self.int_counter = 0
        self.string_length = 1
        self.array_length = 1

    def generate_int(self):
        self.int_counter += 1
        return self.int_counter

    def generate_string(self):
        self.string_length += 1
        return ''.join(random.choices(string.ascii_letters, k=self.string_length))

    def generate_string_array(self):
        self.array_length += 1
        return [self.generate_string() for _ in range(self.array_length)]

    def generate_string_list_2d(self, num_rows):
        return [[self.generate_string() for _ in range(self.array_length)] for _ in range(num_rows)]

    def generate_string_list_2d(self, num_rows, num_cols):
        return [[self.generate_string() for _ in range(num_cols)] for _ in range(num_rows)]


class RandomGeneratorRandom:
    """
    One of the supported generators with size of object random in each call to generator functions

    Attributes
    ----------
    int_counter : int
        counter for generating int values
    string_length : int
        length of the string to be generated
    array_length : int
        length of the array to be generated

    Methods
    -------
    generate_int()
        generates int value
    generate_string()
        generates string value of random length
    generate_array()
        generates array of random length
    generate_string_list_2d(num_rows)
        generates 2d array of strings of maximum size num_rows x random length
    generate_string_list_2d(num_rows, num_cols)
        generates 2d array of strings of maximum size num_rows x num_cols
    """
    def generate_int(self, max_int=100000000):
        return int(random.randint(1, max_int))

    def generate_string(self):
        return ''.join(random.choices(string.ascii_letters, k=self.generate_int(50)))

    def generate_string_array(self):
        return [self.generate_string() for _ in range(self.generate_int(50))]

    def generate_string_list_2d(self, num_rows):
        num_rows = random.randint(1, num_rows)
        array_length = self.generate_int(50)
        return [[self.generate_string() for _ in range(array_length)] for _ in range(num_rows)]

    def generate_string_list_2d(self, num_rows, num_cols):
        num_rows = random.randint(1, num_rows)
        num_cols = random.randint(1, num_cols)
        return [[self.generate_string() for _ in range(num_cols)] for _ in range(num_rows)]
