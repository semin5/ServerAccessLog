package sarangit.semin5.serveraccesslog.service;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sarangit.semin5.serveraccesslog.domain.ItemLog;
import sarangit.semin5.serveraccesslog.repository.ItemLogRepository;
import sarangit.semin5.serveraccesslog.web.ItemLogForm;

@Service
public class ItemLogService {
    private final ItemLogRepository repository;
    private final AccessLogService accessLogService;
    private final JdbcTemplate jdbcTemplate;

    public ItemLogService(ItemLogRepository repository, AccessLogService accessLogService, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.accessLogService = accessLogService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ItemLog create(ItemLogForm form) {
        SignatureImage signature = extractRequiredSignature(form);
        return repository.save(new ItemLog(form.isRental(), form.getReceivedDate(), clean(form.getRecipientName()),
                clean(form.getRecipientDepartment()), clean(form.getItemName()), resolvedQuantity(form),
                nullable(form.getAssetNumber()), clean(form.getReason()), signature.fileName(),
                signature.contentType(), signature.bytes()));
    }

    @Transactional
    public ItemLog update(Long id, ItemLogForm form) {
        ItemLog log = get(id);
        Optional<SignatureImage> signature = extractOptionalSignature(form);
        log.update(form.isRental(), form.getReceivedDate(), clean(form.getRecipientName()),
                clean(form.getRecipientDepartment()), clean(form.getItemName()), resolvedQuantity(form),
                nullable(form.getAssetNumber()), clean(form.getReason()),
                signature.map(SignatureImage::fileName).orElse(log.getSignatureFileName()),
                signature.map(SignatureImage::contentType).orElse(log.getSignatureContentType()),
                signature.map(SignatureImage::bytes).orElse(log.getSignatureImage()));
        return log;
    }

    @Transactional
    public void confirm(Long id, String managerName) {
        if (!managerNames().contains(managerName)) {
            throw new IllegalArgumentException("담당자를 선택해주세요.");
        }
        ItemLog log = get(id);
        if (log.getConfirmedAt() != null) {
            throw new IllegalArgumentException("이미 담당자 확인이 완료된 기록입니다.");
        }
        log.confirm(managerName);
    }

    @Transactional
    public void returnItem(Long id) {
        ItemLog log = get(id);
        if (!log.isRental()) throw new IllegalArgumentException("대여 물품만 반납 처리할 수 있습니다.");
        if (log.getConfirmedAt() == null) throw new IllegalArgumentException("담당자 확인을 먼저 완료해주세요.");
        if (log.getReturnedAt() != null) throw new IllegalArgumentException("이미 반납 처리된 기록입니다.");
        log.returnItem();
    }

    @Transactional
    public void updateRemarks(Long id, String remarks) {
        if (remarks != null && remarks.length() > 2000) {
            throw new IllegalArgumentException("비고는 2000자 이하로 입력해주세요.");
        }
        get(id).updateRemarks(remarks);
    }

    @Transactional(readOnly = true)
    public List<ItemLog> find(LocalDate startDate, LocalDate endDate, String itemName, String managerName, Boolean rental) {
        return repository.findForAdmin(startDate, endDate, nullable(itemName), nullable(managerName), rental);
    }

    @Transactional(readOnly = true)
    public List<String> itemNames() { return repository.findDistinctItemNames(); }

    @Transactional(readOnly = true)
    public List<String> departmentNames() {
        return departmentOptions().stream().map(DepartmentOption::name).toList();
    }

    @Transactional(readOnly = true)
    public List<DepartmentOption> departmentOptions() {
        return jdbcTemplate.query("SELECT name, middle_category FROM worklog.department WHERE is_active = 1 ORDER BY name",
                (rs, rowNum) -> new DepartmentOption(rs.getString("name"), rs.getString("middle_category")));
    }

    public record DepartmentOption(String name, String middleCategory) { }

    public List<String> managerNames() { return accessLogService.managerNames(); }

    @Transactional(readOnly = true)
    public ItemLog get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("물품 기록을 찾을 수 없습니다. id=" + id));
    }

    private SignatureImage extractRequiredSignature(ItemLogForm form) {
        return extractOptionalSignature(form).orElseThrow(() -> new IllegalArgumentException("서명이 필요합니다."));
    }

    private Optional<SignatureImage> extractOptionalSignature(ItemLogForm form) {
        if (form.getSignatureData() == null || form.getSignatureData().isBlank()) return Optional.empty();
        String prefix = "data:image/png;base64,";
        if (!form.getSignatureData().startsWith(prefix)) throw new IllegalArgumentException("서명 이미지 형식이 올바르지 않습니다.");
        try {
            return Optional.of(new SignatureImage(Base64.getDecoder().decode(form.getSignatureData().substring(prefix.length())),
                    "signature.png", "image/png"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("서명 이미지 형식이 올바르지 않습니다.", e);
        }
    }

    private String clean(String value) { return value == null ? null : value.trim(); }
    private String nullable(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private int resolvedQuantity(ItemLogForm form) { return form.getQuantity() == null ? 1 : form.getQuantity(); }
    private record SignatureImage(byte[] bytes, String fileName, String contentType) { }
}
