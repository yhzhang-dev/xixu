package wiki.yuhang.xixu.auth;

public record AppUser(long id, String username, String passwordHash, String displayName,
                      String roleCode, int status) {
}
