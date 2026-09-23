package com.hercey.mail;

import io.rocketbase.mail.EmailTemplateBuilder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import com.hercey.provider.MailServiceProvider;
import com.hercey.util.Env;

public abstract class Mailable implements Runnable{
    private final MailServiceProvider mailServiceProvider;
    private final EmailTemplateBuilder.EmailTemplateConfigBuilder emailTemplateConfigBuilder;

    public Mailable(){
        this.mailServiceProvider = MailServiceProvider.getInstance();
        this.emailTemplateConfigBuilder = EmailTemplateBuilder.builder();
    }

    @Override
    public void run() {
        try {
            Session mailSession = Session.getInstance(mailServiceProvider.getProperties(), mailServiceProvider.getAuthenticator());
            MimeMessage mimeMessage = new MimeMessage(mailSession);
            mimeMessage.setFrom(new InternetAddress(Env.get("app.mail")));
            build(mimeMessage);
            if (mimeMessage.getRecipients(Message.RecipientType.TO).length > 0){
                Transport.send(mimeMessage);
                System.out.println("Email Sending Successful....");
            }else {
                throw new RuntimeException("Email Recipients cannot be empty...");
            }
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract void build(Message message)throws MessagingException;

    public EmailTemplateBuilder.EmailTemplateConfigBuilder getEmailTemplateBuilder() {
        return emailTemplateConfigBuilder;
    }


}
