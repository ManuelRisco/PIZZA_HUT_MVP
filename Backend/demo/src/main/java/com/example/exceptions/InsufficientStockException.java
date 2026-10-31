package com.example.exceptions;

public class InsufficientStockException extends IllegalStateException {
    public InsufficientStockException(String message) {
        super(message);
    }
}

