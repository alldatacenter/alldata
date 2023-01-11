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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.plugin.geo;

public class BinarySearchTree<T extends Comparable<T> & RangeChecker<V>, V> {
	private Node<T> root;
	private int size;

	public BinarySearchTree() {
		root = null;
	}

	public void insert(final T value) {
		Node<T> node = new Node<T>(value);

		if (root == null) {
			root = node;
			return;
		}

		Node<T> focusNode = root;
		Node<T> parent;

		while (true) {
			parent = focusNode;

			int comparison = focusNode.getValue().compareTo(value);
			if (comparison == 0) {
				return;
			}
			if (comparison < 0) {
				focusNode = focusNode.getRight();
				if (focusNode == null) {
					parent.setRight(node);
					return;
				}
			} else {
				focusNode = focusNode.getLeft();
				if (focusNode == null) {
					parent.setLeft(node);
					return;
				}
			}
		}
	}

	public T find(final V value) {
		Node<T> focusNode = root;

		int rangeCheck;

		while (focusNode != null) {
			rangeCheck = focusNode.getValue().compareToRange(value);
			if (rangeCheck == 0) {
				break;
			} else if (rangeCheck < 0) {
				focusNode = focusNode.getRight();
			} else {
				focusNode = focusNode.getLeft();
			}
		}

		return focusNode == null ? null : focusNode.getValue();
	}

	final public void preOrderTraverseTree(final ValueProcessor<T> processor) {
		preOrderTraverseTree(getRoot(), processor);
	}

	final public void inOrderTraverseTree(final ValueProcessor<T> processor) {
		inOrderTraverseTree(getRoot(), processor);
	}

	Node<T> getRoot() {
		return root;
	}

	void setRoot(final Node<T> newRoot) {
		root = newRoot;
	}

	void rebalance() {
		Node<T> dummy = new Node<T>(null);
		dummy.setRight(root);

		setRoot(dummy);

		degenerate();
		reconstruct();

		setRoot(getRoot().getRight());
	}

	final void inOrderTraverseTree(final Node<T> focusNode, final ValueProcessor<T> processor) {
		if (focusNode != null) {
			inOrderTraverseTree(focusNode.getLeft(), processor);
			processor.process(focusNode.getValue());
			inOrderTraverseTree(focusNode.getRight(), processor);
		}
	}

	final void preOrderTraverseTree(final Node<T> focusNode, final ValueProcessor<T> processor) {
		if (focusNode != null) {
			processor.process(focusNode.getValue());

			preOrderTraverseTree(focusNode.getLeft(), processor);
			preOrderTraverseTree(focusNode.getRight(), processor);
		}
	}

	private void degenerate() {

		Node<T> remainder, temp, sentinel;

		sentinel = getRoot();
		remainder = sentinel.getRight();

		size = 0;

		while (remainder != null) {
			if (remainder.getLeft() == null) {
				sentinel = remainder;
				remainder = remainder.getRight();
				size++;
			} else {
				temp = remainder.getLeft();
				remainder.setLeft(temp.getRight());
				temp.setRight(remainder);
				remainder = temp;
				sentinel.setRight(temp);
			}
		}
	}

	private void reconstruct() {

		int sz = size;
		Node<T> node = getRoot();

		int fullCount = fullSize(sz);
		rotateLeft(node, sz - fullCount);

		for (sz = fullCount; sz > 1; sz /= 2) {
			rotateLeft(node, sz / 2);
		}
	}

	private void rotateLeft(Node<T> root, final int count) {
		if (root == null) return;

		Node<T> scanner = root;

		for (int i = 0; i < count; i++) {
			//Leftward rotation
			Node<T> child = scanner.getRight();
			scanner.setRight(child.getRight());
			scanner = child.getRight();
			child.setRight(scanner.getLeft());
			scanner.setLeft(child);
		}
	}

	private int fullSize(final int size) {
		// Full portion of a complete tree
		int ret = 1;
		while (ret <= size) {
			ret = 2*ret + 1;
		}
		return ret / 2;
	}

	static class Node<T> {
		private T value;
		private Node<T> left;
		private Node<T> right;

		public Node(final T value) {
			this.value = value;
		}

		public T getValue() {
			return value;
		}

		public void setValue(final T value) {
			this.value = value;
		}

		public Node<T> getLeft() {
			return left;
		}

		public void setLeft(final Node<T> left) {
			this.left = left;
		}

		public Node<T> getRight() {
			return right;
		}

		public void setRight(final Node<T> right) {
			this.right = right;
		}
	}
}