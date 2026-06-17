package sarangit.semin5.serveraccesslog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;

@Entity
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String companyName;

    @Column(nullable = false, length = 50)
    private String visitorName;

    @Column(length = 50)
    private String contact;

    @Column(length = 100)
    private String hostName;

    @Column(length = 100)
    private String serverName;

    @Column(nullable = false)
    private LocalDateTime visitedAt;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(length = 255)
    private String signatureFileName;

    @Column(length = 100)
    private String signatureContentType;

    @Lob
    @Column(nullable = false)
    private byte[] signatureImage;

    @Lob
    @Column(nullable = false)
    private byte[] pdfFile;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AccessLog() {
    }

    public AccessLog(
            String companyName,
            String visitorName,
            String contact,
            String hostName,
            String serverName,
            LocalDateTime visitedAt,
            String content,
            String signatureFileName,
            String signatureContentType,
            byte[] signatureImage,
            byte[] pdfFile
    ) {
        this.companyName = companyName;
        this.visitorName = visitorName;
        this.contact = contact;
        this.hostName = hostName;
        this.serverName = serverName;
        this.visitedAt = visitedAt;
        this.content = content;
        this.signatureFileName = signatureFileName;
        this.signatureContentType = signatureContentType;
        this.signatureImage = signatureImage;
        this.pdfFile = pdfFile;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getVisitorName() {
        return visitorName;
    }

    public String getContact() {
        return contact;
    }

    public String getHostName() {
        return hostName;
    }

    public String getServerName() {
        return serverName;
    }

    public LocalDateTime getVisitedAt() {
        return visitedAt;
    }

    public String getContent() {
        return content;
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

    public byte[] getPdfFile() {
        return pdfFile;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
