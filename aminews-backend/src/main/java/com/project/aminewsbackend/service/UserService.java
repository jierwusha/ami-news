package com.project.aminewsbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.aminewsbackend.dto.UserDTO;
import com.project.aminewsbackend.entity.User;
import com.project.aminewsbackend.mapper.UserMapper;
import com.project.aminewsbackend.utils.JwtUtil;
import com.project.aminewsbackend.utils.Result;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
* @author ARounder
*/
@Service
public class UserService extends ServiceImpl<UserMapper, User>
    implements IService<User> {

    private final JwtUtil jwtUtil;
    @Autowired
    private FolderService folderService;
    @Autowired
    private HotWordService hotWordService;
    @Autowired
    private MailService mailService;
    @Autowired
    private RedisService redisService;

    public UserService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Autowired
    private RssService rssService;


    public Result login(UserDTO userDTO) {
        System.out.println("[UserService] login 入参: " + userDTO);
        if (userDTO == null || userDTO.getUsernameOrEmail() == null || userDTO.getPassword() == null) {
            System.out.println("[UserService] login 参数不全: " + userDTO);
            return Result.error(401,"输入的用户信息不全");
        }

        // 通过usernameOrEmail字符串得到用户名或邮箱
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        // md5加密密码
        user.setPassword(DigestUtils.md5Hex(user.getPassword()));
        System.out.println("[UserService] login 尝试登录: " + userDTO.getUsernameOrEmail());
        if (userDTO.getUsernameOrEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            user.setEmail(userDTO.getUsernameOrEmail());
            // 将邮箱转为小写
            user.setEmail(user.getEmail().toLowerCase());
            // 通过邮箱密码查询用户，判断是否存在
            User existingUser = this.lambdaQuery()
                    .eq(User::getEmail, user.getEmail())
                    .eq(User::getPassword, user.getPassword())
                    .one();
            if (existingUser == null) {
                System.out.println("[UserService] login 邮箱或密码错误: " + user.getEmail());
                return Result.error(401, "邮箱或密码错误");
            }
            // 设置用户名以便后续使用
            BeanUtils.copyProperties(existingUser, user);
            System.out.println("[UserService] login 邮箱登录成功: " + user.getEmail());
        }else {
            user.setUsername(userDTO.getUsernameOrEmail());
            // 通过用户名密码查询用户，判断是否存在
            User existingUser = this.lambdaQuery()
                    .eq(User::getUsername, user.getUsername())
                    .eq(User::getPassword, user.getPassword())
                    .one();
            if (existingUser == null) {
                System.out.println("[UserService] login 用户名或密码错误: " + user.getUsername());
                return Result.error(401, "用户名或密码错误");
            }
            // 设置邮箱以便后续使用
            BeanUtils.copyProperties(existingUser, user);
            System.out.println("[UserService] login 用户名登录成功: " + user.getUsername());
        }
        // 生成token,使用userid和随机数作为token的唯一标识
        String token = jwtUtil.genAccessToken((int) (user.getId() + System.currentTimeMillis()));
        System.out.println("[UserService] login 生成token: " + token);
        // 将token和用户信息存入redis，设置过期时间为1天
        redisService.set("user:token:" + token, user, 1, TimeUnit.DAYS);
        System.out.println("[UserService] login 用户信息存入redis: " + user.getId());
        hotWordService.refreshHotWordsForUser(user.getId());
        System.out.println("[UserService] login 热词刷新完成: " + user.getId());
        return Result.success(token, "登录成功");
    }

    public Result register(UserDTO user) {
        System.out.println("[UserService] register 入参: " + user);
        if (user == null || user.getUsername() == null || user.getPassword() == null) {
            System.out.println("[UserService] register 用户名或密码为空: " + user);
            return Result.error(401, "用户名或密码不能为空");
        }
        // 判断密码是否符合规范，不得含有特殊字符，长度在6-20之间，必须包含字母和数字
        if (user.getPassword().length() < 6 || user.getPassword().length() > 20 || !user.getPassword().matches("^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d]+$")) {
            System.out.println("[UserService] 注册失败：密码不符合规范 -> " + user.getPassword());
            return Result.error(402, "密码不符合规范");
        }
        // md5加密密码
        user.setPassword(DigestUtils.md5Hex(user.getPassword()));
        // 检查用户名是否已存在
        User existingUser = this.lambdaQuery()
                .eq(User::getUsername, user.getUsername())
                .one();
        if (existingUser != null) {
            System.out.println("[UserService] 注册失败：用户名已存在 -> " + user.getUsername());
            return Result.error(403, "用户名已存在");
        }
        // 检查用户名是否符合规范，不得含有特殊字符，长度在3-20之间
        if (user.getUsername().length() < 3 || user.getUsername().length() > 20 || !user.getUsername().matches("^[a-zA-Z0-9_]+$")) {
            System.out.println("[UserService] 注册失败：用户名不符合规范 -> " + user.getUsername());
            return Result.error(404, "用户名不符合规范");
        }
        // 将邮箱转为小写
        if (user.getEmail() != null) {
            user.setEmail(user.getEmail().toLowerCase());
        }
        // 检查邮箱是否已被注册
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            User existingEmailUser = this.lambdaQuery()
                    .eq(User::getEmail, user.getEmail())
                    .one();
            if (existingEmailUser != null) {
                System.out.println("[UserService] 注册失败：邮箱已被注册 -> " + user.getEmail());
                return Result.error(404, "邮箱已被注册");
            }
        }
        // 通过redis检查验证码
        String code = (String) redisService.get("identify:code:" + user.getEmail());
        if (code == null || !code.equals(user.getCode())) {
            System.out.println("[UserService] 注册失败：验证码错误或已过期 -> " + user.getEmail());
            return Result.error(405, "验证码错误或已过期");
        } else {
            // 验证码正确，删除redis中的验证码
            redisService.delete("identify:code:" + user.getEmail());
            System.out.println("[UserService] 注册成功：验证码验证通过 -> " + user.getEmail());
        }
        // 创建保存新用户对象
        User saveUser = new User();
        BeanUtils.copyProperties(user, saveUser);
        // 保存新用户
        boolean saveResult = this.save(saveUser);
        System.out.println("[UserService] 新用户注册保存结果: " + saveResult + "，用户名: " + saveUser.getUsername());
        rssService.addDefaultSubscribes(saveUser);
        System.out.println("[UserService] 已为新用户添加默认订阅: " + saveUser.getUsername());
        folderService.createRootFolder(saveUser.getId());
        System.out.println("[UserService] 已为新用户创建根文件夹: " + saveUser.getUsername());
        System.out.println("[UserService] 注册流程完成: " + saveUser.getUsername());
        return Result.success("注册成功");
    }

    public Result getCode(String email) {
        System.out.println("[UserService] getCode 入参: " + email);
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            System.out.println("[UserService] getCode 邮箱格式不正确: " + email);
            return Result.error(400, "邮箱格式不正确");
        }
        // 将邮箱转为小写
        email = email.toLowerCase();
//        // 检查邮箱是否已被注册
//        User existingUser = this.lambdaQuery()
//                .eq(User::getEmail, email)
//                .one();
//        if (existingUser != null) {
//            System.out.println("[UserService] getCode 邮箱已被注册: " + email);
//            return Result.error(409, "邮箱已被注册");
//        }
        String redisKey = "identify:code:" + email;
        String code = (String) redisService.get(redisKey);
        Long expire = redisService.getExpire(redisKey, TimeUnit.SECONDS);
        if (code != null && expire != null && expire > 4 * 60) {
            // 已存在验证码且已超过1分钟，重新生成并发送
            code = String.valueOf((int)((Math.random() * 9 + 1) * 100000));
            mailService.sendCodeMail(email, code);
            redisService.set(redisKey, code, 5 * 60, TimeUnit.SECONDS);
            System.out.println("[UserService] getCode 验证码已重新发送: " + code + " -> " + email);
            return Result.success("验证码已重新发送到您的邮箱，请注意查收。");
        } else if (code == null) {
            // 不存在验证码，生成并发送
            code = String.valueOf((int)((Math.random() * 9 + 1) * 100000));
            mailService.sendCodeMail(email, code);
            redisService.set(redisKey, code, 5 * 60, TimeUnit.SECONDS);
            System.out.println("[UserService] getCode 验证码已发送: " + code + " -> " + email);
            return Result.success("验证码已发送到您的邮箱，请注意查收。");
        } else {
            // 验证码未超过1分钟，提示稍后再试
            System.out.println("[UserService] getCode 验证码请求过于频繁: " + email);
            return Result.error(429, "验证码请求过于频繁，请稍后再试。");
        }
    }

    public Result logout(String token) {
        token = token.replace("Bearer ", "").trim();
        System.out.println("[UserService] logout 入参: " + token);
        if (token == null || token.isEmpty()) {
            System.out.println("[UserService] logout token为空");
            return Result.error(401, "未登录或登录已过期");
        }
        // 删除redis中的token
        redisService.delete("user:token:" + token);
        System.out.println("[UserService] logout 成功删除token: " + token);
        return Result.success("退出登录成功");
    }


    public Result resetPassword(UserDTO userDTO) {
        System.out.println("[UserService] resetPassword 入参: " + userDTO);
        if (userDTO == null || userDTO.getEmail() == null || userDTO.getPassword() == null || userDTO.getCode() == null) {
            System.out.println("[UserService] resetPassword 参数不全: " + userDTO);
            return Result.error(400, "输入的用户信息不全");
        }
        // 将邮箱转为小写
        String email = userDTO.getEmail().toLowerCase();
        // 检查邮箱是否已被注册
        User existingUser = this.lambdaQuery()
                .eq(User::getEmail, email)
                .one();
        if (existingUser == null) {
            System.out.println("[UserService] resetPassword 邮箱未注册: " + email);
            return Result.error(404, "邮箱未注册");
        }
        // 验证码检查
        String redisKey = "identify:code:" + email;
        String code = (String) redisService.get(redisKey);
        if (code == null || !code.equals(userDTO.getCode())) {
            System.out.println("[UserService] resetPassword 验证码错误或已过期: " + email);
            return Result.error(400, "验证码错误或已过期");
        }
        // md5加密新密码
        String newPassword = DigestUtils.md5Hex(userDTO.getPassword());
        // 更新密码
        existingUser.setPassword(newPassword);
        boolean updateResult = this.updateById(existingUser);
        if (!updateResult) {
            System.out.println("[UserService] resetPassword 密码更新失败: " + email);
            return Result.error(500, "密码更新失败，请稍后再试");
        }
        // 删除redis中的验证码
        redisService.delete(redisKey);
        System.out.println("[UserService] resetPassword 密码更新成功: " + email);
        return Result.success("密码重置成功");
    }
}
