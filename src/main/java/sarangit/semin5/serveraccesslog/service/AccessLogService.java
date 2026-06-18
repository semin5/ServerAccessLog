package sarangit.semin5.serveraccesslog.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import sarangit.semin5.serveraccesslog.domain.AccessLog;
import sarangit.semin5.serveraccesslog.repository.AccessLogRepository;
import sarangit.semin5.serveraccesslog.web.AccessLogForm;

@Service
public class AccessLogService {

    private static final List<String> DEFAULT_MANAGER_NAMES = List.of("김재현 팀장", "강성구 사원", "오세민 사원", "최인호 사원");

    private final AccessLogRepository accessLogRepository;
    private final PdfService pdfService;
    private final SignatureImageService signatureImageService;
    private final List<String> managerNames;

    public AccessLogService(
            AccessLogRepository accessLogRepository,
            PdfService pdfService,
            SignatureImageService signatureImageService,
            @Value("${ACCESS_LOG_MANAGERS:}") String managerNames
    ) {
        this.accessLogRepository = accessLogRepository;
        this.pdfService = pdfService;
        this.signatureImageService = signatureImageService;
        this.managerNames = resolveManagerNames(managerNames);
    }

    @Transactional
    public AccessLog create(AccessLogForm form) {
        SignatureImage signatureImage = signatureImageService.extractRequired(form);
        byte[] pdfBytes = pdfService.createAccessLogPdf(form, signatureImage.bytes());

        AccessLog accessLog = new AccessLog(
                form.getCompanyName(),
                form.getVisitorName(),
                form.getBirthDate(),
                null,
                null,
                null,
                form.getVisitedAt(),
                form.getContent(),
                signatureImage.fileName(),
                signatureImage.contentType(),
                signatureImage.bytes(),
                pdfBytes
        );
        return accessLogRepository.save(accessLog);
    }

    @Transactional
    public AccessLog update(Long id, AccessLogForm form) {
        AccessLog accessLog = get(id);
        Optional<SignatureImage> newSignature = signatureImageService.extractOptional(form);
        byte[] signatureBytes = newSignature.map(SignatureImage::bytes).orElse(accessLog.getSignatureImage());
        String signatureFileName = newSignature.map(SignatureImage::fileName).orElse(accessLog.getSignatureFileName());
        String signatureContentType = newSignature.map(SignatureImage::contentType).orElse(accessLog.getSignatureContentType());
        byte[] pdfBytes = pdfService.createAccessLogPdf(form, signatureBytes);

        accessLog.update(
                form.getCompanyName(),
                form.getVisitorName(),
                form.getBirthDate(),
                null,
                null,
                null,
                form.getVisitedAt(),
                form.getContent(),
                signatureFileName,
                signatureContentType,
                signatureBytes,
                pdfBytes
        );
        return accessLog;
    }

    @Transactional
    public void checkout(Long id, String managerName) {
        if (!managerNames.contains(managerName)) {
            throw new IllegalArgumentException("담당자를 선택해 주세요.");
        }
        get(id).checkout(managerName);
    }

    public List<String> managerNames() {
        return managerNames;
    }

    @Transactional(readOnly = true)
    public List<AccessLog> findAll() {
        return accessLogRepository.findAllByOrderByVisitedAtDescIdDesc();
    }

    @Transactional(readOnly = true)
    public List<AccessLog> findByVisitedDateRange(LocalDate startDate, LocalDate endDate, String managerName) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay().minusNanos(1);
        String resolvedManagerName = managerName == null || managerName.isBlank() ? null : managerName;
        return accessLogRepository.findByVisitedAtBetweenAndManager(start, end, resolvedManagerName);
    }

    @Transactional(readOnly = true)
    public List<AccessLog> findByIds(List<Long> ids) {
        return accessLogRepository.findAllById(ids).stream()
                .sorted((left, right) -> {
                    int visitedCompare = right.getVisitedAt().compareTo(left.getVisitedAt());
                    if (visitedCompare != 0) {
                        return visitedCompare;
                    }
                    return right.getId().compareTo(left.getId());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public AccessLog get(Long id) {
        return accessLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("출입 기록을 찾을 수 없습니다. id=" + id));
    }

    private List<String> parseManagerNames(String configuredManagerNames) {
        List<String> parsedManagerNames = Arrays.stream(configuredManagerNames.split(","))
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .toList();
        if (parsedManagerNames.isEmpty()) {
            throw new IllegalStateException("담당자 목록을 1명 이상 설정해 주세요.");
        }
        return parsedManagerNames;
    }

    private List<String> resolveManagerNames(String configuredManagerNames) {
        return readManagerNamesFromEnvFile()
                .or(() -> readManagerNamesFromSystemEnv())
                .or(() -> parseConfiguredManagerNames(configuredManagerNames))
                .orElse(DEFAULT_MANAGER_NAMES);
    }

    private Optional<List<String>> readManagerNamesFromEnvFile() {
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            return Optional.empty();
        }
        try {
            return Files.readAllLines(envPath, StandardCharsets.UTF_8).stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("ACCESS_LOG_MANAGERS="))
                    .map(line -> line.substring("ACCESS_LOG_MANAGERS=".length()))
                    .map(this::parseManagerNames)
                    .findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private Optional<List<String>> readManagerNamesFromSystemEnv() {
        return parseConfiguredManagerNames(System.getenv("ACCESS_LOG_MANAGERS"));
    }

    private Optional<List<String>> parseConfiguredManagerNames(String configuredManagerNames) {
        if (configuredManagerNames == null || configuredManagerNames.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(parseManagerNames(configuredManagerNames));
    }

    record SignatureImage(byte[] bytes, String fileName, String contentType) {
    }

    @Service
    static class SignatureImageService {

        SignatureImage extractRequired(AccessLogForm form) {
            return extractOptional(form)
                    .orElseThrow(() -> new IllegalArgumentException("서명이 필요합니다."));
        }

        Optional<SignatureImage> extractOptional(AccessLogForm form) {
            MultipartFile file = form.getSignatureFile();
            if (file != null && !file.isEmpty()) {
                return Optional.of(extractFile(file));
            }
            if (form.getSignatureData() != null && !form.getSignatureData().isBlank()) {
                return Optional.of(extractDataUrl(form.getSignatureData()));
            }
            return Optional.empty();
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
            String prefix = "data:image/png;base64,";
            if (!signatureData.startsWith(prefix)) {
                throw new IllegalArgumentException("서명 이미지 형식이 올바르지 않습니다.");
            }
            byte[] bytes = java.util.Base64.getDecoder().decode(signatureData.substring(prefix.length()));
            return new SignatureImage(bytes, "signature.png", "image/png");
        }
    }
}
