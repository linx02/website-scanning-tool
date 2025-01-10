package com.leads.leadsgen.exceptions;

public class CrawlerException extends Exception {
    public CrawlerException(String message) {
        super(message);
    }

    public CrawlerException(String message, Throwable cause) {
        super(message, cause);
    }

}
