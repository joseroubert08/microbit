package com.samsung.microbit.presentation;

/**
 * Provides common methods for presenter classes.
 */
public interface Presenter {
    /**
     * Starts some progress.
     */
    void start();

    /**
     * Stops some progress.
     */
    void stop();

    /**
     * Releases resources to prevent memory leaks.
     */
    void destroy();
}
