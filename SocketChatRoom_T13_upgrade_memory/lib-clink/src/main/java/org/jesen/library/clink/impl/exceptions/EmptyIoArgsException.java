package org.jesen.library.clink.impl.exceptions;

import java.io.IOException;

/**
 * IoArgs为null时抛出的异常
 */
public class EmptyIoArgsException extends IOException {
    public EmptyIoArgsException(String s) {
        super(s);
    }
}