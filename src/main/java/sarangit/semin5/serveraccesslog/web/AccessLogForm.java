package sarangit.semin5.serveraccesslog.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import sarangit.semin5.serveraccesslog.domain.AccessLog;

public class AccessLogForm {

    @NotBlank(message = "소속을 입력해주세요.")
    @Size(max = 100, message = "소속은 100자 이하로 입력해주세요.")
    private String companyName;

    @NotBlank(message = "성명을 입력해주세요.")
    @Size(max = 50, message = "성명은 50자 이하로 입력해주세요.")
    private String visitorName;

    @NotNull(message = "생년월일을 입력해주세요.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private String contact;

    private String hostName;

    private String serverName;

    @NotNull(message = "출입 시간을 입력해주세요.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime visitedAt;

    @NotBlank(message = "출입사유를 입력해주세요.")
    @Size(max = 2000, message = "출입사유는 2000자 이하로 입력해주세요.")
    private String content;

    private String signatureData;

    private MultipartFile signatureFile;

    private boolean privacyAgreed;

    public boolean hasSignature() {
        return (signatureData != null && !signatureData.isBlank())
                || (signatureFile != null && !signatureFile.isEmpty());
    }

    public static AccessLogForm from(AccessLog accessLog) {
        AccessLogForm form = new AccessLogForm();
        form.setCompanyName(accessLog.getCompanyName());
        form.setVisitorName(accessLog.getVisitorName());
        form.setBirthDate(accessLog.getBirthDate());
        form.setContact(accessLog.getContact());
        form.setHostName(accessLog.getHostName());
        form.setServerName(accessLog.getServerName());
        form.setVisitedAt(accessLog.getVisitedAt());
        form.setContent(accessLog.getContent());
        return form;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getVisitorName() {
        return visitorName;
    }

    public void setVisitorName(String visitorName) {
        this.visitorName = visitorName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public LocalDateTime getVisitedAt() {
        return visitedAt;
    }

    public void setVisitedAt(LocalDateTime visitedAt) {
        this.visitedAt = visitedAt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSignatureData() {
        return signatureData;
    }

    public void setSignatureData(String signatureData) {
        this.signatureData = signatureData;
    }

    public MultipartFile getSignatureFile() {
        return signatureFile;
    }

    public void setSignatureFile(MultipartFile signatureFile) {
        this.signatureFile = signatureFile;
    }

    public boolean isPrivacyAgreed() {
        return privacyAgreed;
    }

    public void setPrivacyAgreed(boolean privacyAgreed) {
        this.privacyAgreed = privacyAgreed;
    }
}
