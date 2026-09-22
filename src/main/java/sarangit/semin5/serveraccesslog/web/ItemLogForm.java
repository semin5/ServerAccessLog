package sarangit.semin5.serveraccesslog.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import sarangit.semin5.serveraccesslog.domain.ItemLog;

public class ItemLogForm {
    private boolean rental;

    @NotNull(message = "날짜를 입력해주세요.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate receivedDate;

    @NotBlank(message = "수령인을 입력해주세요.")
    @Size(max = 50, message = "수령인은 50자 이하로 입력해주세요.")
    private String recipientName;

    @NotBlank(message = "수령부서를 입력해주세요.")
    @Size(max = 100, message = "수령부서는 100자 이하로 입력해주세요.")
    private String recipientDepartment;

    @NotBlank(message = "물품명을 입력하거나 선택해주세요.")
    @Size(max = 200, message = "물품명은 200자 이하로 입력해주세요.")
    private String itemName;

    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    private Integer quantity;

    @Size(max = 100, message = "자산번호는 100자 이하로 입력해주세요.")
    private String assetNumber;

    @NotBlank(message = "사유를 입력해주세요.")
    @Size(max = 2000, message = "사유는 2000자 이하로 입력해주세요.")
    private String reason;

    private String signatureData;
    private boolean privacyAgreed;

    public boolean hasSignature() { return signatureData != null && !signatureData.isBlank(); }

    public static ItemLogForm from(ItemLog log) {
        ItemLogForm form = new ItemLogForm();
        form.setRental(log.isRental());
        form.setReceivedDate(log.getReceivedDate());
        form.setRecipientName(log.getRecipientName());
        form.setRecipientDepartment(log.getRecipientDepartment());
        form.setItemName(log.getItemName());
        form.setQuantity(log.getQuantity());
        form.setAssetNumber(log.getAssetNumber());
        form.setReason(log.getReason());
        form.setPrivacyAgreed(true);
        return form;
    }

    public boolean isRental() { return rental; }
    public void setRental(boolean rental) { this.rental = rental; }
    public LocalDate getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getRecipientDepartment() { return recipientDepartment; }
    public void setRecipientDepartment(String recipientDepartment) { this.recipientDepartment = recipientDepartment; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getAssetNumber() { return assetNumber; }
    public void setAssetNumber(String assetNumber) { this.assetNumber = assetNumber; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getSignatureData() { return signatureData; }
    public void setSignatureData(String signatureData) { this.signatureData = signatureData; }
    public boolean isPrivacyAgreed() { return privacyAgreed; }
    public void setPrivacyAgreed(boolean privacyAgreed) { this.privacyAgreed = privacyAgreed; }
}
