package fr.rakambda.filbleuattestations;

import fr.rakambda.filbleuattestations.config.Mail;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

@Log4j2
public class Mailer{
	private final Mail configurationMail;
	
	public Mailer(Mail configurationMail){
		this.configurationMail = configurationMail;
	}
	
	public boolean sendMail(String recipient, String subject, String body, File attachment){
		try{
			var properties = getProperties();
			var session = getSession(properties);
			
			var message = buildMessage(session, recipient, subject, body, attachment);
			sendMessage(session, message);
			
			log.info("Mail sent");
		}
		catch(Exception e){
			log.error("Failed to send mail", e);
			return false;
		}
		return true;
	}
	
	private void sendMessage(@NonNull Session session, @NonNull Message message) throws MessagingException{
		var transport = session.getTransport("smtps");
		transport.connect(configurationMail.getHost(), configurationMail.getFromEmail(), configurationMail.getPassword());
		transport.sendMessage(message, message.getAllRecipients());
	}
	
	@NonNull
	private Message buildMessage(@NonNull Session session, @NonNull String recipient, @NonNull String subject, @NonNull String body, @Nullable File attachment) throws MessagingException, IOException{
		var message = new MimeMessage(session);
		message.setFrom(new InternetAddress(configurationMail.getFromName()));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
		message.setSubject(subject);
		
		var multipart = new MimeMultipart();
		
		var mimeBodyPart = new MimeBodyPart();
		mimeBodyPart.setContent(body, "text/plain");
		multipart.addBodyPart(mimeBodyPart);
		
		if(attachment != null && attachment.exists()){
			var attachmentBodyPart = new MimeBodyPart();
			attachmentBodyPart.attachFile(attachment);
			multipart.addBodyPart(attachmentBodyPart);
		}
		
		message.setContent(multipart);
		return message;
	}
	
	@NonNull
	private Session getSession(@NonNull Properties properties){
		return Session.getInstance(properties, new Authenticator(){
			@Override
			protected PasswordAuthentication getPasswordAuthentication(){
				return new PasswordAuthentication(configurationMail.getUsername(), configurationMail.getPassword());
			}
		});
	}
	
	@NonNull
	private Properties getProperties(){
		var properties = new Properties();
		properties.put("mail.smtp.auth", true);
		properties.put("mail.smtp.starttls.enable", "true");
		properties.put("mail.smtp.host", configurationMail.getHost());
		properties.put("mail.smtp.port", configurationMail.getPort());
		properties.put("mail.smtp.ssl.trust", configurationMail.getHost());
		return properties;
	}
}
