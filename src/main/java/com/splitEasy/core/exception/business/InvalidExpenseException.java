package com.splitEasy.core.exception.business;

import org.springframework.http.HttpStatus;

public class InvalidExpenseException extends BusinessException {
    public InvalidExpenseException(String message) {
        super(message, "ERR_INVALID_EXPENSE", HttpStatus.BAD_REQUEST);
    }
}