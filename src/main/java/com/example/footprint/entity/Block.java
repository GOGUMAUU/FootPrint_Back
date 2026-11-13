package com.example.footprint.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="blocks", uniqueConstraints=@UniqueConstraint(columnNames={"blocker_id","blocked_id"}))
@Getter @Setter @NoArgsConstructor
public class Block {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long blockId;
    @Column(nullable=false) private Long blockerId;
    @Column(nullable=false) private Long blockedId;
    @Column(nullable=false) private Instant createdAt;
}
