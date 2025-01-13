package com.leads.leadsgen.exception;

public class CrawlerException extends Exception {
    public CrawlerException(String message) {
        super(message);
    }

    public CrawlerException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class Timeout extends CrawlerException {
        public Timeout(String message) {
            super(message);
        }
    }

}
