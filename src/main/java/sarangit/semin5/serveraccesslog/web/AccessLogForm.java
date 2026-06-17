package sarangit.semin5.serveraccesslog.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

public class AccessLogForm {

    @NotBlank(message = "상호명을 입력해주세요.")
    @Size(max = 100, message = "상호명은 100자 이하로 입력해주세요.")
    private String companyName;

    @NotBlank(message = "이름을 입력해주세요.")
    @Size(max = 50, message = "이름은 50자 이하로 입력해주세요.")
    private String visitorName;

    @Size(max = 50, message = "연락처는 50자 이하로 입력해주세요.")
    private String contact;

    @Size(max = 100, message = "담당자는 100자 이하로 입력해주세요.")
    private String hostName;

    @Size(max = 100, message = "서버명은 100자 이하로 입력해주세요.")
    private String serverName;

    @NotNull(message = "출입 시간을 입력해주세요.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime visitedAt;

    @NotBlank(message = "내용을 입력해주세요.")
    @Size(max = 2000, message = "내용은 2000자 이하로 입력해주세요.")
    private String content;

    private String signatureData;

    private MultipartFile signatureFile;

    public boolean hasSignature() {
        return (signatureData != null && !signatureData.isBlank())
                || (signatureFile != null && !signatureFile.isEmpty());
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
}
