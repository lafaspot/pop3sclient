/**
 *
 */
package com.lafaspot.pop.exception;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Pop Exception object.
 *
 * @author kraman
 *
 */
public class PopException extends Exception {

    /** Pop exception message. */
    private String message = null;

    /** Error type. */
    private Type type;

    /**
     * Constructor.
     *
     * @param type of failure
     */
    public PopException(@Nonnull final Type type) {
        super(type.toString());
        this.type = type;
    }

    /**
     * Returns the exception type.
     * @return type
     */
    public PopException.Type getType() {
        return type;
    }

    /**
     * Constructor.
     *
     * @param failureType type of failure
     * @param cause error message
     */
    public PopException(@Nonnull final Type failureType, @Nullable final Throwable cause) {
        super(failureType.toString(), cause);
    }

    /**
     * Types of PopException.
     *
     * @author kraman
     *
     */
    public enum Type {
        /** Failed to connect to server. */
        CONNECT_FAILURE,
        /** Inactivity timeout fired. */
        TIMEDOUT,
        /** Parse failure. */
        PARSE_FAILURE,
        /** Session in invalid state to process command. */
        INVALID_STATE,
        /** Failed to process command - internal failure. */
        INTERNAL_FAILURE,
        /** Invalid arguments. */
        INVALID_ARGUMENTS,
        /** Channel disconnected. */
        CHANNEL_DISCONNECTED,
        /** Channel not connected. */
        CHANNEL_NOT_CONNECTED;
    }

}
