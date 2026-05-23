// package com.project.aminewsbackend.service;

// import com.project.aminewsbackend.entity.Item;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import java.util.List;

// @SpringBootTest
// public class NewsQaServiceDebugTest {
    
//     @Autowired
//     private NewsQaService newsQaService;
    
//     @Test
//     public void testAnswerQuestion() {
//         String question = "武汉大学有哪些新闻？";
//         String sessionId = "test-session-1";
        
//         System.out.println("=== 开始测试问答服务 ===");
//         System.out.println("测试问题: " + question);
//         System.out.println("会话ID: " + sessionId);
        
//         try {
//             String answer = newsQaService.answerQuestion(question, sessionId);
            
//             System.out.println("\n=== 最终生成的答案 ===");
//             System.out.println(answer);
            
//             // 验证结果
//             assert answer != null : "答案不能为空";
//             assert !answer.isEmpty() : "答案不能为空字符串";
            
//             System.out.println("\n=== 测试完成 ===");
            
//         } catch (Exception e) {
//             System.err.println("测试失败，异常信息：");
//             e.printStackTrace();
//             throw e; // 重新抛出异常以便JUnit捕获
//         }
//     }
    
//     @Test
//     public void testMultipleQuestions() {
//         String[] questions = {
//             "最新的国际新闻有哪些？",
//             "今天有什么重要新闻？",
//             "科技方面有什么新动态？"
//         };
        
//         for (int i = 0; i < questions.length; i++) {
//             System.out.println("\n" + "=".repeat(50));
//             System.out.println("测试问题 " + (i + 1) + ": " + questions[i]);
//             System.out.println("=".repeat(50));
            
//             try {
//                 String answer = newsQaService.answerQuestion(questions[i], "test-session-" + (i + 1));
//                 System.out.println("答案: " + answer);
//             } catch (Exception e) {
//                 System.err.println("问题 " + (i + 1) + " 测试失败: " + e.getMessage());
//             }
//         }
//     }
// }