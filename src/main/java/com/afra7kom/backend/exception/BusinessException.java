package com.afra7kom.backend.exception;

/**
 * Exception métier pour les erreurs de logique métier
 */
public class BusinessException extends RuntimeException {
    
    public BusinessException(String message) {
        super(message);
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}


