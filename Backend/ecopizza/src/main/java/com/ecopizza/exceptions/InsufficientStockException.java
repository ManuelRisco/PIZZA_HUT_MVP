package com.ecopizza.exceptions;

public class InsufficientStockException extends IllegalStateException {
    public InsufficientStockException(String message) {
        super(message);
    }
}

