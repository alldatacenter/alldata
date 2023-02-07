/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#ifndef _DRILL_COLLECTIONS_H
#define _DRILL_COLLECTIONS_H

#include <iterator>

#include <boost/noncopyable.hpp>
#include <boost/shared_ptr.hpp>

namespace Drill {
namespace impl {

/**
 * Interface for internal iterators
 */
template<typename T>
class DrillIteratorImpl: private boost::noncopyable {
public:
	typedef DrillIteratorImpl<T> iterator;
	typedef boost::shared_ptr<iterator> iterator_ptr;

	typedef T value_type;
	typedef value_type& reference;
	typedef value_type* pointer;

	virtual ~DrillIteratorImpl() {};

	// To allow conversion from non-const to const types
	virtual operator typename DrillIteratorImpl<const T>::iterator_ptr() const = 0;

	virtual reference operator*() const = 0;
	virtual pointer   operator->() const = 0;

	virtual iterator& operator++() = 0;

	virtual bool operator==(const iterator& x) const = 0;
	virtual bool operator!=(const iterator& x) const = 0;
};

/**
 * Interface for internal collections
 */
template<typename T>
class DrillCollectionImpl: private boost::noncopyable {
public:
	// STL-like iterator typedef
	typedef DrillIteratorImpl<T> iterator;
	typedef boost::shared_ptr<iterator> iterator_ptr;
	typedef DrillIteratorImpl<const T> const_iterator;
	typedef boost::shared_ptr<const_iterator> const_iterator_ptr;

	typedef T value_type;
	typedef value_type& reference;
	typedef const value_type& const_reference;
	typedef value_type* pointer;
	typedef const value_type* const_pointer;
	typedef int size_type;

	virtual ~DrillCollectionImpl() {}

	virtual iterator_ptr begin() = 0;
	virtual const_iterator_ptr begin() const = 0;
	virtual iterator_ptr end() = 0;
	virtual const_iterator_ptr end() const = 0;
};
} // namespace internal

template<typename T>
class DrillCollection;

template<typename T>
class DrillIterator: public std::iterator<std::input_iterator_tag, T> {
public:
	typedef impl::DrillIteratorImpl<T> Impl;
	typedef boost::shared_ptr<Impl> ImplPtr;

	typedef DrillIterator<T> iterator;
	typedef std::iterator<std::input_iterator_tag, T> superclass;
	typedef typename superclass::reference reference;
	typedef typename superclass::pointer pointer;

	// Default constructor
	DrillIterator(): m_pImpl() {};
	~DrillIterator() {}

	// Iterators are CopyConstructible and CopyAssignable
	DrillIterator(const iterator& it): m_pImpl(it.m_pImpl) {}
	iterator& operator=(const iterator& it) {
		m_pImpl = it.m_pImpl;
		return *this;
	}

	template<typename U>
	DrillIterator(const DrillIterator<U>& it): m_pImpl(*it.m_pImpl) {}

	reference operator*() const { return m_pImpl->operator*(); }
	pointer   operator->() const { return m_pImpl->operator->(); }

	iterator& operator++() { m_pImpl->operator++(); return *this; }

	bool operator==(const iterator& x) const { 
		if (m_pImpl == x.m_pImpl) {
			return true;
		}
		return m_pImpl && m_pImpl->operator==(*x.m_pImpl);
	}

	bool operator!=(const iterator& x) const { 
		if (m_pImpl == x.m_pImpl) {
			return false;
		}
		return !m_pImpl ||  m_pImpl->operator!=(*x.m_pImpl);
	}

private:
	template<typename U>
	friend class DrillCollection;
	template<typename U>
	friend class DrillIterator;

	ImplPtr m_pImpl;

	template<typename U>
	DrillIterator(const boost::shared_ptr<impl::DrillIteratorImpl<U> >& pImpl): m_pImpl(pImpl) {}
};

template<typename T>
class DrillCollection {
public:
	typedef impl::DrillCollectionImpl<T> Impl;
	typedef boost::shared_ptr<Impl> ImplPtr;

	// STL-like iterator typedef
	typedef DrillIterator<T> iterator;
	typedef DrillIterator<const T> const_iterator;
	typedef T value_type;
	typedef value_type& reference;
	typedef const value_type& const_reference;
	typedef value_type* pointer;
	typedef const value_type* const_pointer;
	typedef int size_type;

	iterator       begin()       { return iterator(m_pImpl->begin()); }
	const_iterator begin() const { return const_iterator(boost::const_pointer_cast<const Impl>(m_pImpl)->begin()); }
	iterator       end()         { return iterator(m_pImpl->end()); }
	const_iterator end() const   { return const_iterator(boost::const_pointer_cast<const Impl>(m_pImpl)->end()); }

protected:
	DrillCollection(const ImplPtr& impl): m_pImpl(impl) {}

	Impl& operator*() { return *m_pImpl; }
	const Impl& operator*() const { return *m_pImpl; }
	Impl* operator->() { return m_pImpl.get(); }
	const Impl* operator->() const { return m_pImpl.get(); }

private:
	ImplPtr m_pImpl;
};


} /* namespace Drill */
#endif /* _DRILL_COLLECTIONS_H */
