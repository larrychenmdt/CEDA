/*
 *
 *  * Copyright 2003-2022 Beijing XinRong Meridian Limited.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * https://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.adaptiveMQ.exceptions;

/**
 * Base Ceda exception for catching all Ceda specific errors.
 */
public class CedaException extends RuntimeException
{
    /**
     * Category of {@link Exception}.
     */
    public enum Category
    {
        /**
         * Exception indicates a fatal condition. Recommendation is to terminate process immediately to avoid
         * state corruption.
         */
        FATAL,

        /**
         * Exception is an error. Corrective action is recommended if understood, otherwise treat as fatal.
         */
        ERROR,

        /**
         * Exception is a warning. Action has been, or will be, taken to handle the condition.
         * Additional corrective action by the application may be needed.
         */
        WARN
    }

    /**
     * {@link CedaException.Category} of the exception to help the client decide how they should proceed.
     */
    private final Category category;

    /**
     * Default Ceda exception as {@link CedaException.Category#ERROR}.
     */
    public CedaException()
    {
        this.category = Category.ERROR;
    }

    /**
     * Default Ceda exception with provided {@link CedaException.Category}.
     *
     * @param category of this exception.
     */
    public CedaException(final Category category)
    {
        this.category = category;
    }

    /**
     * Ceda exception with provided message and {@link CedaException.Category#ERROR}.
     *
     * @param message to detail the exception.
     */
    public CedaException(final String message)
    {
        super(Category.ERROR.name() + " - " + message);
        this.category = Category.ERROR;
    }

    /**
     * Ceda exception with provided cause and {@link CedaException.Category#ERROR}.
     *
     * @param cause of the error.
     */
    public CedaException(final Throwable cause)
    {
        super(cause);
        this.category = Category.ERROR;
    }

    /**
     * Ceda exception with a detailed message and provided {@link CedaException.Category}.
     *
     * @param message  providing detail on the error.
     * @param category of the exception.
     */
    public CedaException(final String message, final Category category)
    {
        super(category.name() + " - " + message);
        this.category = category;
    }

    /**
     * Ceda exception with a detailed message, cause, and {@link CedaException.Category#ERROR}.
     *
     * @param message providing detail on the error.
     * @param cause   of the error.
     */
    public CedaException(final String message, final Throwable cause)
    {
        super(Category.ERROR.name() + " - " + message, cause);
        this.category = Category.ERROR;
    }

    /**
     * Ceda exception with cause and provided {@link CedaException.Category}.
     *
     * @param cause    of the error.
     * @param category of the exception.
     */
    public CedaException(final Throwable cause, final Category category)
    {
        super(cause);
        this.category = category;
    }

    /**
     * Ceda exception with a detailed message, cause, and {@link CedaException.Category}.
     *
     * @param message  providing detail on the error.
     * @param cause    of the error.
     * @param category of the exception.
     */
    public CedaException(final String message, final Throwable cause, final Category category)
    {
        super(category.name() + " - " + message, cause);
        this.category = category;
    }

    /**
     * Constructs a new Ceda exception with a detail message, cause, suppression enabled or disabled,
     * and writable stack trace enabled or disabled, in the category {@link CedaException.Category#ERROR}.
     *
     * @param message            providing detail on the error.
     * @param cause              of the error.
     * @param enableSuppression  is suppression enabled or not.
     * @param writableStackTrace is the stack trace writable or not.
     */
    public CedaException(
        final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace)
    {
        super(Category.ERROR.name() + " - " + message, cause, enableSuppression, writableStackTrace);
        this.category = Category.ERROR;
    }

    /**
     * Constructs a new Ceda exception with a detail message, cause, suppression enabled or disabled,
     * writable stack trace enabled or disabled, an {@link CedaException.Category}.
     *
     * @param message            providing detail on the error.
     * @param cause              of the error.
     * @param enableSuppression  is suppression enabled or not.
     * @param writableStackTrace is the stack trace writable or not.
     * @param category           of the exception.
     */
    public CedaException(
        final String message,
        final Throwable cause,
        final boolean enableSuppression,
        final boolean writableStackTrace,
        final Category category)
    {
        super(category.name() + " - " + message, cause, enableSuppression, writableStackTrace);
        this.category = category;
    }

    /**
     * {@link CedaException.Category} of exception for determining what follow-up action can be taken.
     *
     * @return {@link CedaException.Category} of exception for determining what follow-up action can be taken.
     */
    public Category category()
    {
        return category;
    }
}
