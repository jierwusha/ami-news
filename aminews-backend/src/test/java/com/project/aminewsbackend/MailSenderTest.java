package com.project.aminewsbackend;

import com.project.aminewsbackend.service.MailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSender;

@SpringBootTest
public class MailSenderTest {

    @Autowired
    private MailService mailService;

    // 测试发送邮件
    @Test
    public void testSendCodeEmail() {
        String code = "123456";
        String email = "18805086528@163.com";
        try {
            mailService.sendCodeMail(email, code);
            System.out.println("邮件发送成功");
        } catch (Exception e) {
            System.err.println("邮件发送失败: " + e.getMessage());
        }
    }

}
