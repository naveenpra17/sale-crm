package com.example.acres.entity;
import jakarta.persistence.*; import lombok.*; import java.time.*;
@Entity @Table(name="audit_logs",indexes=@Index(name="idx_audit_created",columnList="created_at")) @Getter @Setter @NoArgsConstructor
public class AuditLog { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") private User user; @Column(nullable=false) private String action; @Column(nullable=false) private String entityType; private String entityId; @Column(columnDefinition="text") private String oldValue; @Column(columnDefinition="text") private String newValue; @Column(nullable=false) private Instant createdAt; private String ipAddress; }
