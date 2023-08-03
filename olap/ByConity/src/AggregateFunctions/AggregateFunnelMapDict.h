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

#include <Common/Allocator.h>
#include <Common/PODArray.h>
#include <Common/ArenaAllocator.h>
#include <common/StringRef.h>
#include <utility>


namespace DB
{

template<typename DictType>
typename DictType::ElemType locateMapDictByIndex(DictType& input, typename DictType::IdxType& val)
{
    // assume val is valid and exist in input's index slot
    for(auto it = input.sorted_buf.begin(); it != input.sorted_buf.end(); it++)
    {
        if(it->second == val)
            return it->first;
    }
    // dummy output
    return {};
}


template<typename ElementType, typename IndexType>
class EmbeddedMapDict
{
public:
    using ElemIndexPair = std::pair<ElementType, IndexType>;
    using Allocator = MixedArenaAllocator<4096>;
    using SortedBuf = PODArray<ElemIndexPair, 4*sizeof(ElemIndexPair), Allocator>;
    using ElemType = ElementType;
    using IdxType = IndexType;
    // Keep this buffer sorted so that it can be accessed in O(log_n) complexity
    SortedBuf sorted_buf;

    /* locate lowerBound of val in the sorted buffer */
    std::pair<bool, typename SortedBuf::const_iterator>
    lowerBoundMapDict(const ElementType& val) const
    {
        // logic copy from std::lower_bound
        size_t count = sorted_buf.size(), step;
        auto first_it  = sorted_buf.begin();
        //empty Dict, return false
        if (count == 0) return {false, first_it};

        decltype(first_it) it;

        while (count>0)
        {
            it = first_it; step = count/2; std::advance(it, step);
            if (it->first < val)
            {
                first_it = ++it;
                count-=step+1;
            }
            else
                count = step;
        }

        // true if val exist inside input
        return {bool(first_it != sorted_buf.end() && first_it->first==val), first_it};
    }

    /* locate lowerBound of val in the sorted buffer */
    std::pair<bool, typename SortedBuf::iterator>
    lowerBoundMapDict(const ElementType& val)
    {
        // logic copy from std::lower_bound
        size_t count = sorted_buf.size(), step;
        auto first_it  = sorted_buf.begin();
        //empty Dict, return false
        if (count == 0) return {false, first_it};

        decltype(first_it) it;

        while(count>0)
        {
            it = first_it; step = count/2; std::advance(it, step);
            if (it->first < val)
            {
                first_it = ++it;
                count -= step+1;
            }
            else
                count = step;
        }

        // true if val exist inside input
        return {bool(first_it != sorted_buf.end() && first_it->first==val), first_it};
    }

    // Return true if add new elemement successfully.
    bool add(const ElementType elem, Arena* arena)
    {
        //find if elem existed in sorted buf
        auto lower_it = lowerBoundMapDict(elem);

        if(lower_it.first) return false; // elem already exist
        sorted_buf.insert_one(lower_it.second, {elem, size()}, arena);

        return true;
    }

    std::pair<bool, const IndexType> get(const ElementType elem) const
    {
        //find if elem existed in sorted buf
        auto lower_it = lowerBoundMapDict(elem);
        return {lower_it.first, lower_it.first ? lower_it.second->second : IndexType{} };
    }

    IndexType operator[](const ElementType elem) const
    {
        //assume elem existed in sorted buf
        auto lower_it = lowerBoundMapDict(elem);
        return lower_it.second->second;
    }

    inline size_t size() const {return sorted_buf.size();}
    inline SortedBuf const& getRawBuf() const {return sorted_buf;}
    inline SortedBuf& getRawBuf() {return sorted_buf;}

    // debug logic
    bool isValid() const
    {
        size_t s = size();
        if (s==0) return true;
        size_t count = 0;
        for (auto& v: sorted_buf)
            count += v.second;

        // valid dictionary means key are 0, 1, ....s-1
        return count * 2 == s*(s-1);
    }

    void printToConsole(size_t lineno) const
    {
        // DEBUG CODE, will remote before checkin
        std::cout<<"dictRawBuf info: "<<std::endl;
        std::for_each(sorted_buf.begin(),sorted_buf.end(), [](auto& v){std::cout<<v.first<<", "<<v.second<<std::endl;});
        std::cout<<"------------------"<<lineno<<std::endl;
    }
};

/**
   StringMapDict assume materialized strings are allocated from Arena, and
   their lifespan are the same as Arena itself. StringMapDict destructor isn't
   responsible for dealloc those strings for performance
*/
template<typename IndexType>
class StringMapDict
{
public:
    using ElemIndexPair = std::pair<StringRef, IndexType>;
    // Whether the following three buffers waste memory a bit.
    using Allocator = MixedArenaAllocator<4096>;
    using SortedBuf = PODArray<ElemIndexPair, 4*sizeof(ElemIndexPair), Allocator>;
    using ElemType = StringRef;
    using IdxType = IndexType;

    SortedBuf sorted_buf;

    /* locate lowerBound of val in the sorted buffer */
    std::pair<bool, typename SortedBuf::const_iterator>
    lowerBoundMapDict(StringRef const& val) const
    {
        // logic copy from std::lower_bound
        size_t count = sorted_buf.size(), step;
        typename SortedBuf::const_iterator first_it  = sorted_buf.begin();
        //empty Dict, return false
        if (count == 0) return {false, first_it};

        decltype(first_it) it;

        while(count>0)
        {
            it = first_it; step = count/2; std::advance(it, step);
            if (it->first < val)
            {
                first_it = ++it;
                count-=step+1;
            }
            else
                count = step;
        }

        // true if val exist inside input
        return {bool(first_it != sorted_buf.end() && first_it->first==val), first_it};
    }

    /* locate lowerBound of val in the sorted buffer */
    std::pair<bool, typename SortedBuf::iterator>
    lowerBoundMapDict(StringRef const& val)
    {
        // logic copy from std::lower_bound
        size_t count = sorted_buf.size(), step;
        typename SortedBuf::iterator first_it  = sorted_buf.begin();
        //empty Dict, return false
        if (count == 0) return {false, first_it};
        decltype(first_it) it;

        while(count>0)
        {
            it = first_it; step = count/2; std::advance(it, step);
            if (it->first < val)
            {
                first_it = ++it;
                count-=step+1;
            }
            else
                count = step;
        }

        // true if val exist inside input
        return {bool(first_it != sorted_buf.end() && first_it->first==val), first_it};
    }

    // Return true if add new element successfully.
    bool add(StringRef const& elem, Arena* arena)
    {
        auto lower_it = lowerBoundMapDict(elem);
        if (lower_it.first) return false;

        //alloc element from arena, and deep copy StringRef
        char* buf = arena->alloc(elem.size);
        memcpy(buf, elem.data, elem.size);

        sorted_buf.insert_one(lower_it.second, {StringRef{buf, elem.size}, size()}, arena);

        return true;
    }

    bool add(String const& elem, Arena* arena)
    {
        StringRef str(elem);
        return add(str, arena);
    }

    std::pair<bool, const IndexType> get(const StringRef& elem) const
    {
        auto lower_it = lowerBoundMapDict(elem);
        return {lower_it.first, lower_it.first ? lower_it.second->second : IndexType{}};
    }

    std::pair<bool, const IndexType> get(const String& elem) const
    {
        StringRef sref(elem);
        return get(sref);
    }

    IndexType operator[](const StringRef& elem) const
    {
        auto lower_it = lowerBoundMapDict(elem);
        return lower_it.second->second;
    }

    IndexType operator[](const String& elem) const
    {
        StringRef sref(elem);
        auto lower_it = lowerBoundMapDict(sref);
        return lower_it.second->second;
    }

    inline size_t size() const {return sorted_buf.size();}

    inline const SortedBuf& getRawBuf() const { return sorted_buf;}
    inline SortedBuf& getRawBuf() { return sorted_buf;}

    // debug logic
    bool isValid() const
    {
        size_t s = size();
        if(s==0) return true;

        size_t count = 0;
        for (auto& v: sorted_buf)
            count += v.second;

        // valid dictionary means key are 0, 1, ....s-1
        return count * 2 == s*(s-1);
    }

};

}
