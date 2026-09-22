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
public class ItemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean rental;

    @Column(nullable = false)
    private LocalDate receivedDate;

    @Column(nullable = false, length = 50)
    private String recipientName;

    @Column(nullable = false, length = 100)
    private String recipientDepartment;

    @Column(nullable = false, length = 200)
    private String itemName;

    @Column(nullable = false)
    private int quantity;

    @Column(length = 100)
    private String assetNumber;

    @Column(nullable = false, length = 2000)
    private String reason;

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
    private LocalDateTime confirmedAt;

    @Column(length = 50)
    private String managerName;

    @Column(length = 2000)
    private String remarks;

    @Column
    private LocalDateTime returnedAt;

    protected ItemLog() {
    }

    public ItemLog(boolean rental, LocalDate receivedDate, String recipientName, String recipientDepartment,
                   String itemName, int quantity, String assetNumber, String reason,
                   String signatureFileName, String signatureContentType, byte[] signatureImage) {
        this.rental = rental;
        this.receivedDate = receivedDate;
        this.recipientName = recipientName;
        this.recipientDepartment = recipientDepartment;
        this.itemName = itemName;
        this.quantity = quantity;
        this.assetNumber = assetNumber;
        this.reason = reason;
        this.signatureFileName = signatureFileName;
        this.signatureContentType = signatureContentType;
        this.signatureImage = signatureImage;
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public void update(boolean rental, LocalDate receivedDate, String recipientName, String recipientDepartment,
                       String itemName, int quantity, String assetNumber, String reason,
                       String signatureFileName, String signatureContentType, byte[] signatureImage) {
        this.rental = rental;
        this.receivedDate = receivedDate;
        this.recipientName = recipientName;
        this.recipientDepartment = recipientDepartment;
        this.itemName = itemName;
        this.quantity = quantity;
        this.assetNumber = assetNumber;
        this.reason = reason;
        this.signatureFileName = signatureFileName;
        this.signatureContentType = signatureContentType;
        this.signatureImage = signatureImage;
    }

    public void confirm(String managerName) {
        this.managerName = managerName;
        this.confirmedAt = LocalDateTime.now();
    }

    public void returnItem() {
        this.returnedAt = LocalDateTime.now();
    }

    public void updateRemarks(String remarks) {
        this.remarks = remarks == null || remarks.isBlank() ? null : remarks.trim();
    }

    public Long getId() { return id; }
    public boolean isRental() { return rental; }
    public LocalDate getReceivedDate() { return receivedDate; }
    public String getRecipientName() { return recipientName; }
    public String getRecipientDepartment() { return recipientDepartment; }
    public String getItemName() { return itemName; }
    public int getQuantity() { return quantity; }
    public String getAssetNumber() { return assetNumber; }
    public String getReason() { return reason; }
    public String getSignatureFileName() { return signatureFileName; }
    public String getSignatureContentType() { return signatureContentType; }
    public byte[] getSignatureImage() { return signatureImage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public String getManagerName() { return managerName; }
    public String getRemarks() { return remarks; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
}
