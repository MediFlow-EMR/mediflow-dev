package com.mediflow.emr.dto;

import lombok.Builder;

@Builder
public record EmailSignupRequest(
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Email
        @jakarta.validation.constraints.Size(max = 255) String email,
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,72}$",
                message = "비밀번호는 8~72자이며 문자·숫자·허용된 특수문자를 포함해야 합니다.") String password
) {
}
