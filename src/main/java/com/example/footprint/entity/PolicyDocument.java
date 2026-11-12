package com.example.footprint.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity @Table(name="policy_documents", uniqueConstraints=@UniqueConstraint(columnNames={"type","version"}))
@Getter @Setter @NoArgsConstructor
public class PolicyDocument {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long policyId;
    @Column(nullable=false, length=30) private String type;
    @Column(nullable=false, length=40) private String version;
    @Column(nullable=false, columnDefinition="TEXT") private String content;
    @Column(nullable=false) private boolean consentRequired;
    @Column(nullable=false, length=20) private String requiredAt;
    @Column(nullable=false) private boolean withdrawable;
    @Column(nullable=false) private boolean published;
    @Column(nullable=false) private Instant publishedAt;
}
