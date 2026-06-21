package sarangit.semin5.serveraccesslog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class CarryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String visitorName;

    @Column(nullable = false)
    private LocalDate workDate;

    @Column(nullable = false, length = 100)
    private String companyName;

    @Column(nullable = false, length = 50)
    private String jobTitle;

    @Column(nullable = false)
    private LocalDateTime visitedAt;

    @Column(nullable = false, length = 50)
    private String contact;

    @Column(nullable = false)
    private boolean equipmentInbound;

    @Column(nullable = false)
    private boolean equipmentOutbound;

    @Column(length = 200)
    private String equipmentModelName;

    @Column(nullable = false)
    private boolean storageInbound;

    @Column(nullable = false)
    private boolean storageOutbound;

    @Column(length = 200)
    private String storageModelName;

    @Column(nullable = false, length = 2000)
    private String workContent;

    @Column(length = 255)
    private String signatureFileName;

    @Column(length = 100)
    private String signatureContentType;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] signatureImage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime exitedAt;

    @Column(length = 50)
    private String exitGuideName;

    protected CarryLog() {
    }

    public CarryLog(
            String visitorName,
            LocalDate workDate,
            String companyName,
            String jobTitle,
            LocalDateTime visitedAt,
            String contact,
            boolean equipmentInbound,
            boolean equipmentOutbound,
            String equipmentModelName,
            boolean storageInbound,
            boolean storageOutbound,
            String storageModelName,
            String workContent,
            String signatureFileName,
            String signatureContentType,
            byte[] signatureImage
    ) {
        this.visitorName = visitorName;
        this.workDate = workDate;
        this.companyName = companyName;
        this.jobTitle = jobTitle;
        this.visitedAt = visitedAt;
        this.contact = contact;
        this.equipmentInbound = equipmentInbound;
        this.equipmentOutbound = equipmentOutbound;
        this.equipmentModelName = equipmentModelName;
        this.storageInbound = storageInbound;
        this.storageOutbound = storageOutbound;
        this.storageModelName = storageModelName;
        this.workContent = workContent;
        this.signatureFileName = signatureFileName;
        this.signatureContentType = signatureContentType;
        this.signatureImage = signatureImage;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getVisitorName() {
        return visitorName;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public LocalDateTime getVisitedAt() {
        return visitedAt;
    }

    public String getContact() {
        return contact;
    }

    public boolean isEquipmentInbound() {
        return equipmentInbound;
    }

    public boolean isEquipmentOutbound() {
        return equipmentOutbound;
    }

    public String getEquipmentModelName() {
        return equipmentModelName;
    }

    public boolean isStorageInbound() {
        return storageInbound;
    }

    public boolean isStorageOutbound() {
        return storageOutbound;
    }

    public String getStorageModelName() {
        return storageModelName;
    }

    public String getWorkContent() {
        return workContent;
    }

    public String getSignatureFileName() {
        return signatureFileName;
    }

    public String getSignatureContentType() {
        return signatureContentType;
    }

    public byte[] getSignatureImage() {
        return signatureImage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExitedAt() {
        return exitedAt;
    }

    public String getExitGuideName() {
        return exitGuideName;
    }

    public void update(
            String visitorName,
            LocalDate workDate,
            String companyName,
            String jobTitle,
            LocalDateTime visitedAt,
            String contact,
            boolean equipmentInbound,
            boolean equipmentOutbound,
            String equipmentModelName,
            boolean storageInbound,
            boolean storageOutbound,
            String storageModelName,
            String workContent,
            String signatureFileName,
            String signatureContentType,
            byte[] signatureImage
    ) {
        this.visitorName = visitorName;
        this.workDate = workDate;
        this.companyName = companyName;
        this.jobTitle = jobTitle;
        this.visitedAt = visitedAt;
        this.contact = contact;
        this.equipmentInbound = equipmentInbound;
        this.equipmentOutbound = equipmentOutbound;
        this.equipmentModelName = equipmentModelName;
        this.storageInbound = storageInbound;
        this.storageOutbound = storageOutbound;
        this.storageModelName = storageModelName;
        this.workContent = workContent;
        this.signatureFileName = signatureFileName;
        this.signatureContentType = signatureContentType;
        this.signatureImage = signatureImage;
    }

    public void checkout(String exitGuideName) {
        this.exitedAt = LocalDateTime.now();
        this.exitGuideName = exitGuideName;
    }
}
