package com.mediflow.emr.dto;

import lombok.Builder;

@Builder
public record EmailLoginRequest(
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Email
        @jakarta.validation.constraints.Size(max = 255) String email,
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 72) String password
) {
}

