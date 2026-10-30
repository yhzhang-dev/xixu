package wiki.yuhang.xixu.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdmin implements ApplicationRunner {
    private final AppUserRepository users;
    private final PasswordEncoder passwords;

    @Value("${xixu.bootstrap.username:}")
    private String username;

    @Value("${xixu.bootstrap.password:}")
    private String password;

    public BootstrapAdmin(AppUserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() && password.isBlank()) {
            return;
        }
        if (!username.matches("[A-Za-z0-9_]{3,50}") || password.length() < 12) {
            throw new IllegalArgumentException("初始管理员用户名需为 3-50 位字母/数字/下划线，密码至少 12 位");
        }
        if (users.countUsers() != 0) {
            throw new IllegalStateException("数据库已有用户，初始管理员创建已停止");
        }
        users.createAdmin(username, passwords.encode(password));
        System.out.println("初始管理员账号已创建：" + username);
    }
}
