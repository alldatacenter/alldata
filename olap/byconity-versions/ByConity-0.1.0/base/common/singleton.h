/*
 * Copyright (2022) Bytedance Ltd. and/or its affiliates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma once


namespace ext
{

/** Example (1):
  *
  *    class Derived : public ext::singleton<Derived>
  *    {
  *        friend class ext::singleton<Derived>;
  *        ...
  *    protected:
  *        Derived() {};
  *    };
  *
  * Example (2):
  *
  *    class Some
  *    {
  *        ...
  *    };
  *
  *    class SomeSingleton : public Some, public ext::singleton<SomeSingleton> {}
  */
template <typename T> class singleton
{
public:
    static T & instance()
    {
        /// C++11 has thread safe statics. GCC and Clang have thread safe statics by default even before C++11.
        static T instance;
        return instance;
    }

protected:
    singleton() {}

private:
    singleton(const singleton &);
    singleton & operator=(const singleton &);
};

}
