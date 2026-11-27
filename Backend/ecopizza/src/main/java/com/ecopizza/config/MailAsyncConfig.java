package com.ecopizza.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class MailAsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(MailAsyncConfig.class);

    private final JavaMailSender javaMailSender;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    public MailAsyncConfig(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.javaMailSender = mailSenderProvider != null ? mailSenderProvider.getIfAvailable() : null;
    }

    @PostConstruct
    public void sanitizeGmailAppPassword() {
        if (javaMailSender instanceof JavaMailSenderImpl mailSenderImpl) {
            String pwd = mailSenderImpl.getPassword();
            if (pwd != null && pwd.contains(" ")) {
                mailSenderImpl.setPassword(pwd.replace(" ", "").trim());
                log.info("App Password de correo normalizada correctamente (espacios removidos).");
            }
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void verifySmtpConnectivityOnStartup() {
        if (!mailEnabled || !(javaMailSender instanceof JavaMailSenderImpl mailSenderImpl)) {
            return;
        }
        String user = mailSenderImpl.getUsername();
        if (user == null || user.isBlank()) {
            log.warn("MAIL_USERNAME no est\u00e1 definido en .env; el env\u00edo de correos permanecer\u00e1 inactivo.");
            return;
        }
        Thread probeThread = new Thread(() -> checkSmtpConnection(mailSenderImpl), "smtp-startup-check");
        probeThread.setDaemon(true);
        probeThread.start();
    }

    private void checkSmtpConnection(JavaMailSenderImpl mailSenderImpl) {
        try {
            mailSenderImpl.testConnection();
            log.info("Conexi\u00f3n y autenticaci\u00f3n SMTP Gmail verificadas exitosamente para remitente configurado en .env.");
        } catch (Exception ex) {
            log.error("Error al verificar autenticaci\u00f3n SMTP Gmail con las credenciales de .env: {}", ex.getMessage());
        }
    }

    @Bean(name = "mailTaskExecutor")
    public Executor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("mail-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        executor.initialize();
        return executor;
    }
}

