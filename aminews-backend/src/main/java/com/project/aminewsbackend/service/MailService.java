package com.project.aminewsbackend.service;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MailService {
    @Resource
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendMail(String to, String subject, String text) {
        try {
            // 创建邮件消息
            var message = mailSender.createMimeMessage();
            var helper = new org.springframework.mail.javamail.MimeMessageHelper(message, true);
            // 从配置中读取发件人地址
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true); // 设置为 HTML 格式

            // 发送邮件
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Async
    public void sendCodeMail(String to, String code) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new org.springframework.mail.javamail.MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("验证码");


            // 构建现代风格的 HTML 邮件内容
            String htmlContent = "<!DOCTYPE html>"
                    + "<html><head><meta charset='UTF-8'><title>验证码</title>"
                    + "<style>"
                    + "body { background: #f4f8fb; font-family: 'Segoe UI', Arial, sans-serif; }"
                    + ".container { max-width: 420px; margin: 60px auto; background: #fff; border-radius: 12px; box-shadow: 0 4px 24px #e0e7ef; padding: 36px 32px; text-align: center; }"
                    + ".title { font-size: 24px; color: #222; font-weight: 600; margin-bottom: 18px; }"
                    + ".desc { font-size: 16px; color: #666; margin-bottom: 32px; }"
                    + ".code { display: inline-block; font-size: 32px; letter-spacing: 8px; color: #2563eb; background: #f0f4fa; border-radius: 8px; padding: 16px 32px; font-weight: bold; margin-bottom: 24px; }"
                    + ".tip { font-size: 14px; color: #999; margin-top: 18px; }"
                    + "</style></head><body>"
                    + "<div class='container'>"
                    + "<div class='title'>欢迎使用 AMINEWS 新闻平台</div>"
                    + "<div class='desc'>您的验证码如下，请在输入以完成验证。</div>"
                    + "<div class='code'>" + code + "</div>"
                    + "<div class='tip'>验证码有效期为 5 分钟，请勿泄露给他人。</div>"
                    + "</div></body></html>";

            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
