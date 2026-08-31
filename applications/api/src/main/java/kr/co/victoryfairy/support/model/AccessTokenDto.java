package kr.co.victoryfairy.support.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Schema
public class AccessTokenDto {

    @Schema(accessMode = READ_ONLY, description = "accessToken")
    String accessToken;

    @Schema(accessMode = READ_ONLY, description = "refreshToken")
    String refreshToken;

}
