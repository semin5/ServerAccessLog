package sarangit.semin5.serveraccesslog.service;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sarangit.semin5.serveraccesslog.domain.CarryLog;
import sarangit.semin5.serveraccesslog.repository.CarryLogRepository;
import sarangit.semin5.serveraccesslog.web.CarryLogForm;

@Service
public class CarryLogService {

    private final CarryLogRepository carryLogRepository;
    private final AccessLogService accessLogService;

    public CarryLogService(CarryLogRepository carryLogRepository, AccessLogService accessLogService) {
        this.carryLogRepository = carryLogRepository;
        this.accessLogService = accessLogService;
    }

    @Transactional
    public CarryLog create(CarryLogForm form) {
        SignatureImage signatureImage = extractRequiredSignature(form);
        CarryLog carryLog = new CarryLog(
                form.getVisitorName(),
                form.getVisitedAt().toLocalDate(),
                form.getCompanyName(),
                form.getJobTitle(),
                form.getVisitedAt(),
                formatPhoneNumber(form.getContact()),
                form.isEquipmentInbound(),
                form.isEquipmentOutbound(),
                form.getEquipmentModelName(),
                form.isStorageInbound(),
                form.isStorageOutbound(),
                form.getStorageModelName(),
                form.getWorkContent(),
                signatureImage.fileName(),
                signatureImage.contentType(),
                signatureImage.bytes()
        );
        return carryLogRepository.save(carryLog);
    }

    @Transactional
    public CarryLog update(Long id, CarryLogForm form) {
        CarryLog carryLog = get(id);
        Optional<SignatureImage> newSignature = extractOptionalSignature(form);
        byte[] signatureBytes = newSignature.map(SignatureImage::bytes).orElse(carryLog.getSignatureImage());
        String signatureFileName = newSignature.map(SignatureImage::fileName).orElse(carryLog.getSignatureFileName());
        String signatureContentType = newSignature.map(SignatureImage::contentType).orElse(carryLog.getSignatureContentType());

        carryLog.update(
                form.getVisitorName(),
                form.getVisitedAt().toLocalDate(),
                form.getCompanyName(),
                form.getJobTitle(),
                form.getVisitedAt(),
                formatPhoneNumber(form.getContact()),
                form.isEquipmentInbound(),
                form.isEquipmentOutbound(),
                form.getEquipmentModelName(),
                form.isStorageInbound(),
                form.isStorageOutbound(),
                form.getStorageModelName(),
                form.getWorkContent(),
                signatureFileName,
                signatureContentType,
                signatureBytes
        );
        return carryLog;
    }

    @Transactional
    public void checkout(Long id, String managerName) {
        if (!managerNames().contains(managerName)) {
            throw new IllegalArgumentException("담당자를 선택해주세요.");
        }
        get(id).checkout(managerName);
    }

    public List<String> managerNames() {
        return accessLogService.managerNames();
    }

    @Transactional(readOnly = true)
    public List<CarryLog> findByWorkDateRange(LocalDate startDate, LocalDate endDate) {
        return carryLogRepository.findByWorkDateBetweenOrderByWorkDateDescIdDesc(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<CarryLog> findByWorkDateRange(LocalDate startDate, LocalDate endDate, String managerName) {
        String resolvedManagerName = managerName == null || managerName.isBlank() ? null : managerName;
        return carryLogRepository.findByWorkDateBetweenAndManager(startDate, endDate, resolvedManagerName);
    }

    @Transactional(readOnly = true)
    public List<CarryLog> findByIds(List<Long> ids) {
        return carryLogRepository.findAllById(ids).stream()
                .sorted((left, right) -> {
                    int dateCompare = right.getWorkDate().compareTo(left.getWorkDate());
                    if (dateCompare != 0) {
                        return dateCompare;
                    }
                    return right.getId().compareTo(left.getId());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public CarryLog get(Long id) {
        return carryLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("반출·입 기록을 찾을 수 없습니다. id=" + id));
    }

    private SignatureImage extractRequiredSignature(CarryLogForm form) {
        return extractOptionalSignature(form)
                .orElseThrow(() -> new IllegalArgumentException("서명이 필요합니다."));
    }

    private Optional<SignatureImage> extractOptionalSignature(CarryLogForm form) {
        if (form.getSignatureData() == null || form.getSignatureData().isBlank()) {
            return Optional.empty();
        }
        String prefix = "data:image/png;base64,";
        if (!form.getSignatureData().startsWith(prefix)) {
            throw new IllegalArgumentException("서명 이미지 형식이 올바르지 않습니다.");
        }
        byte[] bytes = Base64.getDecoder().decode(form.getSignatureData().substring(prefix.length()));
        return Optional.of(new SignatureImage(bytes, "signature.png", "image/png"));
    }

    private String formatPhoneNumber(String contact) {
        if (contact == null || contact.isBlank()) {
            return contact;
        }

        String trimmed = contact.trim();
        String digits = trimmed.replaceAll("\\D", "");
        if (digits.length() < 8) {
            return trimmed;
        }

        if (digits.startsWith("02")) {
            if (digits.length() == 9) {
                return digits.substring(0, 2) + "-" + digits.substring(2, 5) + "-" + digits.substring(5);
            }
            if (digits.length() == 10) {
                return digits.substring(0, 2) + "-" + digits.substring(2, 6) + "-" + digits.substring(6);
            }
        }

        if (digits.startsWith("050") && digits.length() == 12) {
            return digits.substring(0, 4) + "-" + digits.substring(4, 8) + "-" + digits.substring(8);
        }

        if (digits.length() == 10) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
        }
        if (digits.length() == 11) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
        }

        return trimmed;
    }

    private record SignatureImage(byte[] bytes, String fileName, String contentType) {
    }
}
