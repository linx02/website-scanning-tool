package com.leads.leadsgen.service;

import com.mailersend.sdk.MailerSend;
import com.mailersend.sdk.emails.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mailersend.sdk.MailerSendResponse;
import com.mailersend.sdk.exceptions.MailerSendException;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Value("${apikeys.mailersend}")
    private String API_TOKEN;

    private static final String FROM_EMAIL = "info@trial-3z0vklomn1eg7qrx.mlsender.net";


    public void sendEmails(List<Map<String, String>> emails) {
        for (Map<String, String> email : emails) {
            sendEmail(email);
        }
    }

    private void sendEmail(Map<String, String> inputEmail) {

        Email email = new Email();

        email.setFrom("name", FROM_EMAIL);

        email.addRecipient(inputEmail.get("recipient"), inputEmail.get("recipient"));

        email.setSubject(inputEmail.get("subject"));

        email.setHtml(inputEmail.get("html"));

        MailerSend ms = new MailerSend();

        ms.setToken(API_TOKEN);

        try {

            MailerSendResponse response = ms.emails().send(email);
            System.out.println(response.messageId);
        } catch (MailerSendException e) {
            e.printStackTrace();
        }
    }
}