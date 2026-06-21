package sarangit.semin5.serveraccesslog.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import sarangit.semin5.serveraccesslog.domain.CarryLog;

public class CarryLogForm {

    @NotBlank(message = "성명을 입력해주세요.")
    @Size(max = 50, message = "성명은 50자 이하로 입력해주세요.")
    private String visitorName;

    @NotBlank(message = "소속을 입력해주세요.")
    @Size(max = 100, message = "소속은 100자 이하로 입력해주세요.")
    private String companyName;

    @NotBlank(message = "직급을 입력해주세요.")
    @Size(max = 50, message = "직급은 50자 이하로 입력해주세요.")
    private String jobTitle;

    @NotNull(message = "입실시간을 입력해주세요.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime visitedAt;

    @NotBlank(message = "연락처를 입력해주세요.")
    @Size(max = 50, message = "연락처는 50자 이하로 입력해주세요.")
    private String contact;

    private boolean equipmentInbound;

    private boolean equipmentOutbound;

    @Size(max = 200, message = "장비 모델명은 200자 이하로 입력해주세요.")
    private String equipmentModelName;

    private boolean storageInbound;

    private boolean storageOutbound;

    @Size(max = 200, message = "저장매체 모델명은 200자 이하로 입력해주세요.")
    private String storageModelName;

    @NotBlank(message = "작업 내역을 입력해주세요.")
    @Size(max = 2000, message = "작업 내역은 2000자 이하로 입력해주세요.")
    private String workContent;

    private String signatureData;

    private boolean privacyAgreed;

    public boolean hasSignature() {
        return signatureData != null && !signatureData.isBlank();
    }

    public static CarryLogForm from(CarryLog carryLog) {
        CarryLogForm form = new CarryLogForm();
        form.setVisitorName(carryLog.getVisitorName());
        form.setCompanyName(carryLog.getCompanyName());
        form.setJobTitle(carryLog.getJobTitle());
        form.setVisitedAt(carryLog.getVisitedAt());
        form.setContact(carryLog.getContact());
        form.setEquipmentInbound(carryLog.isEquipmentInbound());
        form.setEquipmentOutbound(carryLog.isEquipmentOutbound());
        form.setEquipmentModelName(carryLog.getEquipmentModelName());
        form.setStorageInbound(carryLog.isStorageInbound());
        form.setStorageOutbound(carryLog.isStorageOutbound());
        form.setStorageModelName(carryLog.getStorageModelName());
        form.setWorkContent(carryLog.getWorkContent());
        return form;
    }

    public String getVisitorName() {
        return visitorName;
    }

    public void setVisitorName(String visitorName) {
        this.visitorName = visitorName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public LocalDateTime getVisitedAt() {
        return visitedAt;
    }

    public void setVisitedAt(LocalDateTime visitedAt) {
        this.visitedAt = visitedAt;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public boolean isEquipmentInbound() {
        return equipmentInbound;
    }

    public void setEquipmentInbound(boolean equipmentInbound) {
        this.equipmentInbound = equipmentInbound;
    }

    public boolean isEquipmentOutbound() {
        return equipmentOutbound;
    }

    public void setEquipmentOutbound(boolean equipmentOutbound) {
        this.equipmentOutbound = equipmentOutbound;
    }

    public String getEquipmentModelName() {
        return equipmentModelName;
    }

    public void setEquipmentModelName(String equipmentModelName) {
        this.equipmentModelName = equipmentModelName;
    }

    public boolean isStorageInbound() {
        return storageInbound;
    }

    public void setStorageInbound(boolean storageInbound) {
        this.storageInbound = storageInbound;
    }

    public boolean isStorageOutbound() {
        return storageOutbound;
    }

    public void setStorageOutbound(boolean storageOutbound) {
        this.storageOutbound = storageOutbound;
    }

    public String getStorageModelName() {
        return storageModelName;
    }

    public void setStorageModelName(String storageModelName) {
        this.storageModelName = storageModelName;
    }

    public String getWorkContent() {
        return workContent;
    }

    public void setWorkContent(String workContent) {
        this.workContent = workContent;
    }

    public String getSignatureData() {
        return signatureData;
    }

    public void setSignatureData(String signatureData) {
        this.signatureData = signatureData;
    }

    public boolean isPrivacyAgreed() {
        return privacyAgreed;
    }

    public void setPrivacyAgreed(boolean privacyAgreed) {
        this.privacyAgreed = privacyAgreed;
    }
}
