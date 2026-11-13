package com.example.footprint.dto;

import com.example.footprint.entity.PolicyDocument;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public final class PolicyDtos {
    private PolicyDtos() {}
    public record ConsentInput(@NotBlank @Size(max=30) String type, @NotBlank @Size(max=40) String version, @NotNull Boolean accepted) {}
    public record ConsentRequest(@NotNull @Size(min=1,max=20) List<@Valid ConsentInput> consents) {}
    public record Document(String type, String version, String content, boolean required, boolean consentRequired, String requiredAt, boolean withdrawable) {
        public static Document of(PolicyDocument p) {
            return new Document(p.getType(),p.getVersion(),p.getContent(),p.isConsentRequired() && !"OPTIONAL".equals(p.getRequiredAt()),
                    p.isConsentRequired(),p.getRequiredAt(),p.isWithdrawable());
        }
    }
    public record Documents(List<Document> policies) {}
}
