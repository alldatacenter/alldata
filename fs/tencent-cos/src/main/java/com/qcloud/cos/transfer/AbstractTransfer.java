/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 
 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos.transfer;

import static com.qcloud.cos.event.SDKProgressPublisher.publishProgress;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.qcloud.cos.event.ProgressEventType;
import com.qcloud.cos.event.ProgressListener;
import com.qcloud.cos.event.ProgressListenerChain;
import com.qcloud.cos.event.TransferStateChangeListener;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;

/**
 * Abstract transfer implementation.
 */
public abstract class AbstractTransfer implements Transfer {

    /** The current state of this transfer. */
    protected volatile TransferState state = TransferState.Waiting;

    protected TransferMonitor monitor;

    /** The progress of this transfer. */
    private final TransferProgress transferProgress;

    private final String description;

    /** Hook for adding/removing more progress listeners. */
    protected final ProgressListenerChain listenerChain;

    /** Collection of listeners to be notified for changes to the state of this transfer via setState() */
    protected final Collection<TransferStateChangeListener> stateChangeListeners = new LinkedList<TransferStateChangeListener>();

    public AbstractTransfer(String description, TransferProgress transferProgress, ProgressListenerChain progressListenerChain) {
        this(description, transferProgress, progressListenerChain, null);
    }

    public AbstractTransfer(String description, TransferProgress transferProgress,
            ProgressListenerChain progressListenerChain, TransferStateChangeListener stateChangeListener) {
        this.description = description;
        this.listenerChain = progressListenerChain;
        this.transferProgress = transferProgress;
        addStateChangeListener(stateChangeListener);
    }

    /**
     * Returns whether or not the transfer is finished (i.e. completed successfully,
     * failed, or was canceled).  This method should never block.
     *
     * @return Returns <code>true</code> if this transfer is finished (i.e. completed successfully,
     *         failed, or was canceled).  Returns <code>false</code> if otherwise.
     */
    public final synchronized boolean isDone() {
        return (state == TransferState.Failed ||
                state == TransferState.Completed ||
                state == TransferState.Canceled);
    }

    /**
     * Waits for this transfer to complete. This is a blocking call; the current
     * thread is suspended until this transfer completes.
     *
     * @throws CosClientException
     *             If any errors were encountered in the client while making the
     *             request or handling the response.
     * @throws CosServiceException
     *             If any errors occurred in Qcloud COS while processing the
     *             request.
     * @throws InterruptedException
     *             If this thread is interrupted while waiting for the transfer
     *             to complete.
     */
    public void waitForCompletion()
            throws CosClientException, CosServiceException, InterruptedException {
        try {
            Object result = null;
            while (!monitor.isDone() || result == null) {
                Future<?> f = monitor.getFuture();
                result = f.get();
            }
        } catch (ExecutionException e) {
            rethrowExecutionException(e);

        }
    }

    /**
     * Waits for this transfer to finish and returns any error that occurred, or
     * returns <code>null</code> if no errors occurred.
     * This is a blocking call; the current thread
     * will be suspended until this transfer either fails or completes
     * successfully.
     *
     * @return Any error that occurred while processing this transfer.
     *         Otherwise returns <code>null</code> if no errors occurred.
     *
     * @throws InterruptedException
     *             If this thread is interrupted while waiting for the transfer
     *             to complete.
     */
    public CosClientException waitForException() throws InterruptedException {
        try {

            while (!monitor.isDone()) {
                monitor.getFuture().get();
            }
            monitor.getFuture().get();
            return null;
        } catch (ExecutionException e) {
            return unwrapExecutionException(e);
        }
    }

    /**
     * Returns a human-readable description of this transfer.
     *
     * @return A human-readable description of this transfer.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the current state of this transfer.
     *
     * @return The current state of this transfer.
     */
    public synchronized TransferState getState() {
        return state;
    }

    /**
     * Sets the current state of this transfer.
     */
    public void setState(TransferState state) {
        synchronized (this) {
            this.state = state;
        }
        for ( TransferStateChangeListener listener : stateChangeListeners ) {
            listener.transferStateChanged(this, state);
        }
    }

    /**
     * Notifies all the registered state change listeners of the state update.
     */
    public void notifyStateChangeListeners(TransferState state) {
        for ( TransferStateChangeListener listener : stateChangeListeners ) {
            listener.transferStateChanged(this, state);
        }
    }

    /**
     * Adds the specified progress listener to the list of listeners
     * receiving updates about this transfer's progress.
     *
     * @param listener
     *            The progress listener to add.
     */
    public synchronized void addProgressListener(ProgressListener listener) {
        listenerChain.addProgressListener(listener);
    }

    /**
     * Removes the specified progress listener from the list of progress
     * listeners receiving updates about this transfer's progress.
     *
     * @param listener
     *            The progress listener to remove.
     */
    public synchronized void removeProgressListener(ProgressListener listener) {
        listenerChain.removeProgressListener(listener);
    }

    /**
     * Adds the given state change listener to the collection of listeners.
     */
    public synchronized void addStateChangeListener(TransferStateChangeListener listener) {
        if ( listener != null )
            stateChangeListeners.add(listener);
    }

    /**
     * Removes the given state change listener from the collection of listeners.
     */
    public synchronized void removeStateChangeListener(TransferStateChangeListener listener) {
        if ( listener != null )
            stateChangeListeners.remove(listener);
    }

    /**
     * Returns progress information about this transfer.
     *
     * @return The progress information about this transfer.
     */
    public TransferProgress getProgress() {
        return transferProgress;
    }

    /**
     * Sets the monitor used to poll for transfer completion.
     */
    public void setMonitor(TransferMonitor monitor) {
        this.monitor = monitor;
    }

    public TransferMonitor getMonitor() {
        return monitor;
    }

    protected void fireProgressEvent(final ProgressEventType eventType) {
        publishProgress(listenerChain, eventType);
    }

    /**
     * Examines the cause of the specified ExecutionException and either
     * rethrows it directly (if it's a type of CosClientException) or wraps
     * it in an CosClientException and rethrows it.
     *
     * @param e
     *            The execution exception to examine.
     */
    protected void rethrowExecutionException(ExecutionException e) {
        throw unwrapExecutionException(e);
    }

    /**
     * Unwraps the root exception that caused the specified ExecutionException
     * and returns it. If it was not an instance of CosClientException, it is
     * wrapped as an CosClientException.
     *
     * @param e
     *            The ExecutionException to unwrap.
     *
     * @return The root exception that caused the specified ExecutionException.
     */
    protected CosClientException unwrapExecutionException(ExecutionException e) {
        Throwable t = e.getCause();
        if (t instanceof CosClientException) return (CosClientException)t;
        return new CosClientException("Unable to complete transfer: " + t.getMessage(), t);
    }

}
