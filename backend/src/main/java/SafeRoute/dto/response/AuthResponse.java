package SafeRoute.dto.response;

import SafeRoute.enums.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Role role;
}

