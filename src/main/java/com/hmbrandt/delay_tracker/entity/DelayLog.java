package com.hmbrandt.delay_tracker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "delay_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE delay_logs SET deleted_at = NOW() WHERE delay_log_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class DelayLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delay_log_id")
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "delay_date", nullable = false)
    private LocalDate delayDate;

    @Column(name = "location")
    private String location;

    @Column(name = "delay_description")
    private String delayDescription;

    @Column(name = "impact_equipment")
    private String impactEquipment;

    @Column(name = "summary")
    private String summary;

    @Column(name = "resolution")
    private String resolution;

    @Column(name = "workers")
    private String workers;

    @Column(name = "cost")
    private String cost;

    @Column(name = "delay_status")
    private String delayStatus;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "delayLog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DelayTime> times = new ArrayList<>();

    @OneToMany(mappedBy = "delayLog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DelayOption> options = new ArrayList<>();

    @OneToMany(mappedBy = "delayLog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DelaySignature> signatures = new ArrayList<>();

    public void addTime(DelayTime time) {
        if (this.times == null) {
            this.times = new ArrayList<>();
        }
        this.times.add(time);
        time.setDelayLog(this); // CRÍTICO: Vincula el hijo con el padre
    }

    public void addOption(DelayOption option) {
        if (this.options == null) {
            this.options = new ArrayList<>();
        }
        this.options.add(option);
        option.setDelayLog(this); // CRÍTICO: Vincula el hijo con el padre
    }

    public void addSignature(DelaySignature signature) {
        if (this.signatures == null) {
            this.signatures = new ArrayList<>();
        }
        this.signatures.add(signature);
        signature.setDelayLog(this); // CRÍTICO: Vincula el hijo con el padre
    }

    public void removeTime(DelayTime time) {
        if (this.times != null) {
            this.times.remove(time);
            time.setDelayLog(null);
        }
    }

    public void removeOption(DelayOption option) {
        if (this.options != null) {
            this.options.remove(option);
            option.setDelayLog(null);
        }
    }

    public void removeSignature(DelaySignature signature) {
        if (this.signatures != null) {
            this.signatures.remove(signature);
            signature.setDelayLog(null);
        }
    }
}
