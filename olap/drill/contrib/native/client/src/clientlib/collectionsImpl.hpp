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
#ifndef DRILL_COLLECTIONSIMPL_H
#define DRILL_COLLECTIONSIMPL_H

#include <iterator>
#include <drill/collections.hpp>

namespace Drill {
namespace impl {
template<typename T, typename Iterator>
class DrillContainerIterator: public DrillIteratorImpl<T> {
public:
	typedef DrillContainerIterator<T, Iterator> type;
	typedef DrillIteratorImpl<T> supertype;
	typedef typename supertype::iterator iterator;
	typedef typename iterator::value_type value_type;
	typedef typename iterator::reference reference;
	typedef typename iterator::pointer pointer;

	DrillContainerIterator(Iterator it): supertype(), m_it(it) {};

	operator typename DrillIteratorImpl<const T>::iterator_ptr() const { return typename DrillIteratorImpl<const T>::iterator_ptr(new DrillContainerIterator<const T, Iterator>(m_it)); }

	reference operator*() const { return m_it.operator *();}
	pointer   operator->() const { return m_it.operator->(); }

	iterator& operator++() { m_it++; return *this; }

	bool operator==(const iterator& x) const {
		const type& other(dynamic_cast<const type&>(x));
		return m_it == other.m_it;
	}

	bool operator!=(const iterator& x) const { return !(*this==x); }

private:
	Iterator m_it;
};

template<typename T, typename Container>
class DrillContainerCollection: public DrillCollectionImpl<T> {
public:
	typedef DrillCollectionImpl<T> supertype;
	typedef typename supertype::value_type value_type;
	typedef typename supertype::iterator iterator;
	typedef typename supertype::const_iterator const_iterator;

	typedef typename supertype::iterator_ptr iterator_ptr;
	typedef typename supertype::const_iterator_ptr const_iterator_ptr;

	DrillContainerCollection(): supertype(), m_container() {};

	Container& operator*() { return &m_container; }
	const Container& operator*() const { return &m_container; }
	Container* operator->() { return &m_container; }
	const Container* operator->() const { return &m_container; }

	iterator_ptr begin() { return iterator_ptr(new IteratorImpl(m_container.begin())); }
	const_iterator_ptr begin() const { return const_iterator_ptr(new ConstIteratorImpl(m_container.begin())); }
	iterator_ptr end() { return iterator_ptr(new IteratorImpl(m_container.end())); }
	const_iterator_ptr end() const { return const_iterator_ptr(new ConstIteratorImpl(m_container.end())); }

private:
	typedef DrillContainerIterator<value_type, typename Container::iterator> IteratorImpl;
	typedef DrillContainerIterator<const value_type, typename Container::const_iterator> ConstIteratorImpl;

	Container m_container;
};
} /* namespace impl */


/**
 * Drill collection backed up by a vector
 * Offer a view over a collection of Iface instances,
 * where concrete implementation of Iface is T
 */
template<typename Iface, typename T>
class DrillVector: public DrillCollection<Iface> {
public:
	DrillVector(): DrillCollection<Iface>(ImplPtr(new Impl())) {};

	void clear() {
		Impl& impl = static_cast<Impl&>(**this);
		impl->clear();
	}

	void push_back( const T& value ) {
		Impl& impl = static_cast<Impl&>(**this);
		impl->push_back(value);
	}

	void reserve(std::size_t new_cap) {
		Impl& impl = static_cast<Impl&>(**this);
		impl->reserve(new_cap);
	}


private:
	typedef impl::DrillContainerCollection<Iface, std::vector<T> > Impl;
	typedef boost::shared_ptr<Impl> ImplPtr;
};
}



#endif /* DRILL_COLLECTIONSIMPL_H */
