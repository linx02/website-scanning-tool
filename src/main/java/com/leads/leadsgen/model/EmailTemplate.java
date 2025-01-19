package com.leads.leadsgen.model;

public class EmailTemplate {
    private String subject;
    private String html;

    public EmailTemplate(String subject, String html) {
        this.subject = subject;
        this.html = html;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }
}
