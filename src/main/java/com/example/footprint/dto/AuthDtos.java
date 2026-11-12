package com.example.footprint.dto;

import com.example.footprint.dto.PolicyDtos.ConsentInput;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public final class AuthDtos {
    private AuthDtos() {}
    public record Signup(@NotBlank @Email @Size(max=100) String email, @NotBlank @Size(max=128) String password,
                         @NotBlank @Size(max=50) String nickname, @NotNull @Size(max=20) List<@Valid ConsentInput> consents) {}
    public record Login(@NotBlank @Email @Size(max=100) String email, @NotBlank @Size(max=128) String password) {}
    public record Refresh(@NotBlank @Size(max=256) String refreshToken) {}
    public record Tokens(String accessToken, String refreshToken, String tokenType, long expiresIn) {}
}
