package com.leads.leadsgen.exceptions;

public class ScannerException extends Exception {
    public ScannerException(String message) {
        super(message);
    }

    public ScannerException(String message, Throwable cause) {
        super(message, cause);
    }
}
