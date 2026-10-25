package backend.commentary.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Kết quả đăng nhập thành công chứa JWT Token")
public class AuthResponse {

    @Schema(description = "JWT Access Token")
    private String token;

    @Schema(description = "Loại token", example = "Bearer")
    private String type = "Bearer";

    @Schema(description = "ID người dùng")
    private Long id;

    @Schema(description = "Email quản trị viên")
    private String email;

    @Schema(description = "Vai trò", example = "ROLE_ADMIN")
    private String role;

    public AuthResponse() {
    }

    public AuthResponse(String token, String type, Long id, String email, String role) {
        this.token = token;
        this.type = type != null ? type : "Bearer";
        this.id = id;
        this.email = email;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String token;
        private String type = "Bearer";
        private Long id;
        private String email;
        private String role;

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public AuthResponse build() {
            return new AuthResponse(token, type, id, email, role);
        }
    }
}
