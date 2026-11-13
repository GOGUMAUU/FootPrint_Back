package com.example.footprint.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReportRequest {
    private String reason;       // SPAM | INAPPROPRIATE | HARASSMENT | OTHER
    private String description;  // 선택, 500자 이내
}