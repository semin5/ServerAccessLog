package sarangit.semin5.serveraccesslog.service;

import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import sarangit.semin5.serveraccesslog.domain.AccessLog;
import sarangit.semin5.serveraccesslog.repository.AccessLogRepository;
import sarangit.semin5.serveraccesslog.web.AccessLogForm;

@Service
public class AccessLogService {

    private final AccessLogRepository accessLogRepository;
    private final PdfService pdfService;
    private final SignatureImageService signatureImageService;

    public AccessLogService(
            AccessLogRepository accessLogRepository,
            PdfService pdfService,
            SignatureImageService signatureImageService
    ) {
        this.accessLogRepository = accessLogRepository;
        this.pdfService = pdfService;
        this.signatureImageService = signatureImageService;
    }

    @Transactional
    public AccessLog create(AccessLogForm form) {
        SignatureImage signatureImage = signatureImageService.extract(form);
        byte[] pdfBytes = pdfService.createAccessLogPdf(form, signatureImage.bytes());

        AccessLog accessLog = new AccessLog(
                form.getCompanyName(),
                form.getVisitorName(),
                form.getContact(),
                form.getHostName(),
                form.getServerName(),
                form.getVisitedAt(),
                form.getContent(),
                signatureImage.fileName(),
                signatureImage.contentType(),
                signatureImage.bytes(),
                pdfBytes
        );
        return accessLogRepository.save(accessLog);
    }

    @Transactional(readOnly = true)
    public List<AccessLog> findAll() {
        return accessLogRepository.findAllByOrderByVisitedAtDescIdDesc();
    }

    @Transactional(readOnly = true)
    public AccessLog get(Long id) {
        return accessLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("출입 기록을 찾을 수 없습니다. id=" + id));
    }

    record SignatureImage(byte[] bytes, String fileName, String contentType) {
    }

    @Service
    static class SignatureImageService {

        SignatureImage extract(AccessLogForm form) {
            MultipartFile file = form.getSignatureFile();
            if (file != null && !file.isEmpty()) {
                return extractFile(file);
            }
            return extractDataUrl(form.getSignatureData());
        }

        private SignatureImage extractFile(MultipartFile file) {
            String contentType = file.getContentType();
            if (!"image/jpeg".equalsIgnoreCase(contentType) && !"image/jpg".equalsIgnoreCase(contentType)) {
                throw new IllegalArgumentException("JPG 서명 파일만 업로드할 수 있습니다.");
            }
            try {
                return new SignatureImage(file.getBytes(), file.getOriginalFilename(), "image/jpeg");
            } catch (IOException e) {
                throw new IllegalArgumentException("서명 파일을 읽지 못했습니다.", e);
            }
        }

        private SignatureImage extractDataUrl(String signatureData) {
            if (signatureData == null || signatureData.isBlank()) {
                throw new IllegalArgumentException("서명이 필요합니다.");
            }
            String prefix = "data:image/png;base64,";
            if (!signatureData.startsWith(prefix)) {
                throw new IllegalArgumentException("서명 이미지 형식이 올바르지 않습니다.");
            }
            byte[] bytes = java.util.Base64.getDecoder().decode(signatureData.substring(prefix.length()));
            return new SignatureImage(bytes, "signature.png", "image/png");
        }
    }
}
