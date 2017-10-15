package com.netflix.archaius.exceptions;

import java.util.NoSuchElementException;

public class StackSupressedNoSuchElementException extends NoSuchElementException {

    /**
     * Constructs a <code>StackSupressedNoSuchElementException</code> with <tt>null</tt>
     * as its error message string.
     */
    public StackSupressedNoSuchElementException() {
        super();
    }

    /**
     * Constructs a <code>StackSupressedNoSuchElementException</code>, saving a reference
     * to the error message string <tt>s</tt> for later retrieval by the
     * <tt>getMessage</tt> method.
     *
     * @param   s   the detail message.
     */
    public StackSupressedNoSuchElementException(String s) {
        super(s);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
