package com.linkedin.feathr.common.featurizeddataset;

import com.linkedin.feathr.common.tensor.TensorIterator;

/**
 * The base for custom iterators over dense tensors of ranks 1+.
 * Encapsulates the logic for traversing nested collections, so that it can be shared between e.g. Avro and Spark representations.
 *
 * Examples:
 * <ul>
 * <li>
 * A regular tensor of the shape [2, 3].
 * Numbering of positions:
 * [
 *  [0, 1, 2],
 *  [3, 4, 5]
 * ].
 * The sequence of indices:
 * [(0, 0), (0, 1), (0, 2), (1, 0), (1, 1), (1, 2)].
 * </li><li>
 * A ragged tensor of rank 2.
 * Numbering of positions:
 * [
 *  [0, 1],
 *  [],
 *  [2, 3, 4]
 * ]
 * The sequence of indices:
 * [(0, 0), (0, 1), (2, 0), (2, 1), (2, 2)].
 * </li>
 * </ul>
 *
 * Subclasses are expected to cache their current path through the tree of nested collections.
 * There are as many collections in the cache as the rank of the tensor, starting from the root and ending with the level containing leaves/values:
 * cache[0] = root
 * cache[1] = cache[0][_indices[0]]
 * ...
 * cache[rank] = cache[rank-1][_indices[rank-1]] = current value
 */
public abstract class BaseDenseTensorIterator implements TensorIterator {
    private final int _rank;

    // The SoT for iteration: an index for every dimension.
    // Those are incremented in lexicographic order (rightmost more often), corresponding to the structure of nested arrays.
    private final int[] _indices;

    // If true, all of the cached fields are valid and the iterator addresses a valid tuple within the tensor.
    private boolean _valid;

    // The sequential index of the current position in iteration order.
    private int _flatIndex;

    protected BaseDenseTensorIterator(int rank) {
        if (rank < 1) {
            throw new IllegalArgumentException("Only supports rank >= 1, was: " + rank);
        }
        _rank = rank;
        _indices = new int[_rank];
    }

    protected BaseDenseTensorIterator(BaseDenseTensorIterator original) {
        _rank = original._rank;
        _indices = original._indices.clone();
        _valid = original._valid;
        _flatIndex = original._flatIndex;
    }

    protected final int getRank() {
        return _rank;
    }

    public final int getFlatIndex() {
        return _flatIndex;
    }

    protected final int getIndex(int dimension) {
        return _indices[dimension];
    }

    /**
     * Fill the level of cache from the childIndex child of the level-1.
     *
     * @return false if j is out of range.
     */
    protected abstract boolean cache(int level, int childIndex);

    /**
     * Fill the 0th level of cache with the root.
     */
    protected abstract void cacheRoot();

    @Override
    public void start() {
        for (int i = 0; i < getRank(); i++) {
            _indices[i] = 0;
        }
        _valid = true;

        // Find the first path to a value at a leaf of the tree formed by nested arrays.
        // If no such path exist (all arrays are empty), become invalid.
        for (int i = 0; i <= getRank(); i++) {
            // Initialize this level with the first element from the first non-empty array on the previous level.
            // Use increment() to scan the ith level of the tree for the first non-empty array.
            while (!access(i)) {
                increment(i - 1);
                if (!_valid) {
                    return;
                }
            }
        }
    }

    /**
     * Fill the level of cache.
     * @param level 0-rank inclusive, 0 being the root (the feature value itself).
     * @return if the cache was successfully filled - can fail if indices are out of range.
     */
    private boolean access(int level) {
        if (level == 0) {
            cacheRoot();
            return true;
        }
        return cache(level, _indices[level - 1]);
    }

    @Override
    public boolean isValid() {
        return _valid;
    }

    @Override
    public void next() {
        _flatIndex++;
        increment(getRank() - 1);
    }

    // Increments the i-th index by 1, doing the carry-over if needed.
    // Maintains cache so it always corresponds to _indices.
    // Does not touch _indices above i, so is safe to call in a partially initialized state (where only indices and cache up to level i are initialized).
    private void increment(int i) {
        if (i < 0) {
            _valid = false;
            return;
        }
        _indices[i]++;
        while (!access(i + 1)) {
            _indices[i] = 0;
            increment(i - 1);
        }
    }
}
