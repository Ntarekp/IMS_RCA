package npk.rca.ims.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String email;
    private String role;
    private boolean enabled;
    private String name;
    private String phone;
    private String department;
    private String location;
    private String avatarUrl;
    private String coverUrl;
    private LocalDateTime createdAt;
}
