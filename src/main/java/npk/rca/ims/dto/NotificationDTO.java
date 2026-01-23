package npk.rca.ims.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private String id;
    private String title;
    private String message;
    private String type; // INFO, WARNING, ALERT, SUCCESS
    private String timestamp;
    private boolean read;
}
