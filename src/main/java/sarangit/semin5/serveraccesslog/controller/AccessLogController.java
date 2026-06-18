package sarangit.semin5.serveraccesslog.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sarangit.semin5.serveraccesslog.domain.AccessLog;
import sarangit.semin5.serveraccesslog.service.AccessLogBatchPdfService;
import sarangit.semin5.serveraccesslog.service.AccessLogService;
import sarangit.semin5.serveraccesslog.service.PdfService;
import sarangit.semin5.serveraccesslog.web.AccessLogForm;

@Controller
public class AccessLogController {

    private static final String ADMIN_PASSWORD = "love!@0924";
    private static final String ADMIN_AUTH_SESSION_KEY = "adminAuthenticated";
    private static final DateTimeFormatter COMPACT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AccessLogService accessLogService;
    private final AccessLogBatchPdfService accessLogBatchPdfService;
    private final PdfService pdfService;

    public AccessLogController(
            AccessLogService accessLogService,
            AccessLogBatchPdfService accessLogBatchPdfService,
            PdfService pdfService
    ) {
        this.accessLogService = accessLogService;
        this.accessLogBatchPdfService = accessLogBatchPdfService;
        this.pdfService = pdfService;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(LocalDate.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.isBlank()) {
                    setValue(null);
                    return;
                }
                String value = text.trim();
                try {
                    if (value.matches("\\d{8}")) {
                        setValue(LocalDate.parse(value, COMPACT_DATE_FORMATTER));
                        return;
                    }
                    setValue(LocalDate.parse(value));
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("날짜는 19980201 또는 1998-02-01 형식으로 입력해주세요.", e);
                }
            }
        });
    }

    @GetMapping("/")
    public String index(HttpSession session) {
        session.removeAttribute(ADMIN_AUTH_SESSION_KEY);
        return "index";
    }

    @GetMapping("/server-access")
    public String form(Model model, HttpSession session) {
        session.removeAttribute(ADMIN_AUTH_SESSION_KEY);
        if (!model.containsAttribute("accessLogForm")) {
            AccessLogForm form = new AccessLogForm();
            form.setVisitedAt(LocalDateTime.now());
            model.addAttribute("accessLogForm", form);
        }
        return "access-log/form";
    }

    @PostMapping("/server-access/access-logs")
    public String create(
            @Valid @ModelAttribute AccessLogForm accessLogForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (!accessLogForm.hasSignature()) {
            bindingResult.rejectValue("signatureData", "signature.required", "서명을 입력해주세요.");
        }
        if (!accessLogForm.isPrivacyAgreed()) {
            bindingResult.rejectValue("privacyAgreed", "privacy.required", "개인정보 수집 및 이용에 동의해주세요.");
        }
        if (bindingResult.hasErrors()) {
            return "access-log/form";
        }

        AccessLog saved;
        try {
            saved = accessLogService.create(accessLogForm);
        } catch (IllegalArgumentException e) {
            bindingResult.reject("accessLog.invalid", e.getMessage());
            return "access-log/form";
        }
        redirectAttributes.addAttribute("id", saved.getId());
        return "redirect:/server-access/complete/{id}";
    }

    @GetMapping("/server-access/complete/{id}")
    public String complete(@PathVariable Long id, Model model) {
        model.addAttribute("accessLog", accessLogService.get(id));
        return "access-log/complete";
    }

    @GetMapping("/server-access/admin/login")
    public String adminLogin() {
        return "admin/login";
    }

    @PostMapping("/server-access/admin/login")
    public String adminLogin(
            @RequestParam String password,
            HttpSession session,
            Model model
    ) {
        if (ADMIN_PASSWORD.equals(password)) {
            session.setAttribute(ADMIN_AUTH_SESSION_KEY, true);
            return "redirect:/server-access/admin";
        }
        model.addAttribute("error", "비밀번호가 올바르지 않습니다.");
        return "admin/login";
    }

    @GetMapping("/server-access/admin")
    public String admin(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String managerName,
            Model model,
            HttpSession session
    ) {
        if (!isAdminAuthenticated(session)) {
            return "redirect:/server-access/admin/login";
        }

        LocalDate today = LocalDate.now();
        LocalDate resolvedEndDate = endDate == null ? today : endDate;
        LocalDate resolvedStartDate = startDate == null ? today.minusMonths(1) : startDate;

        model.addAttribute("accessLogs", accessLogService.findByVisitedDateRange(resolvedStartDate, resolvedEndDate, managerName));
        model.addAttribute("managerNames", accessLogService.managerNames());
        model.addAttribute("startDate", resolvedStartDate);
        model.addAttribute("endDate", resolvedEndDate);
        model.addAttribute("managerName", managerName == null ? "" : managerName);
        model.addAttribute("currentYear", today.getYear());
        model.addAttribute("today", today);
        return "admin/list";
    }

    @GetMapping("/server-access/admin/{id}/edit")
    public String edit(@PathVariable Long id, Model model, HttpSession session) {
        if (!isAdminAuthenticated(session)) {
            return "redirect:/server-access/admin/login";
        }

        AccessLog accessLog = accessLogService.get(id);
        if (!model.containsAttribute("accessLogForm")) {
            model.addAttribute("accessLogForm", AccessLogForm.from(accessLog));
        }
        model.addAttribute("accessLog", accessLog);
        return "admin/edit";
    }

    @PostMapping("/server-access/admin/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute AccessLogForm accessLogForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpSession session
    ) {
        if (!isAdminAuthenticated(session)) {
            return "redirect:/server-access/admin/login";
        }

        AccessLog accessLog = accessLogService.get(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("accessLog", accessLog);
            return "admin/edit";
        }

        try {
            accessLogService.update(id, accessLogForm);
        } catch (IllegalArgumentException e) {
            bindingResult.reject("accessLog.invalid", e.getMessage());
            model.addAttribute("accessLog", accessLog);
            return "admin/edit";
        }
        redirectAttributes.addFlashAttribute("message", "출입 기록을 수정했습니다.");
        return "redirect:/server-access/admin";
    }

    @PostMapping("/server-access/admin/{id}/checkout")
    public String checkout(
            @PathVariable Long id,
            @RequestParam String managerName,
            RedirectAttributes redirectAttributes,
            HttpSession session
    ) {
        if (!isAdminAuthenticated(session)) {
            return "redirect:/server-access/admin/login";
        }

        try {
            accessLogService.checkout(id, managerName);
            redirectAttributes.addFlashAttribute("message", "퇴실 처리했습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:/server-access/admin";
    }

    @PostMapping("/server-access/admin/pdf")
    public ResponseEntity<byte[]> selectedPdf(
            @RequestParam(name = "selectedIds", required = false) List<Long> selectedIds,
            @RequestParam(name = "ledgerYear", required = false) Integer ledgerYear,
            @RequestParam(name = "printDateEnabled", required = false, defaultValue = "false") boolean printDateEnabled,
            @RequestParam(name = "printDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate printDate,
            HttpSession session
    ) {
        if (!isAdminAuthenticated(session)) {
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, "/server-access/admin/login")
                    .build();
        }

        List<AccessLog> accessLogs = selectedIds == null || selectedIds.isEmpty()
                ? List.of()
                : accessLogService.findByIds(selectedIds);
        int resolvedLedgerYear = ledgerYear == null ? LocalDate.now().getYear() : ledgerYear;
        LocalDate resolvedPrintDate = printDateEnabled
                ? (printDate == null ? LocalDate.now() : printDate)
                : null;
        byte[] bytes = accessLogBatchPdfService.createLedgerPdf(accessLogs, resolvedLedgerYear, resolvedPrintDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("server-access-logs.pdf")
                        .build()
                        .toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @GetMapping("/server-access/admin/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id, HttpSession session) {
        if (!isAdminAuthenticated(session)) {
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, "/server-access/admin/login")
                    .build();
        }

        AccessLog accessLog = accessLogService.get(id);
        byte[] bytes = pdfService.createAccessLogPdf(accessLog);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("server-access-confirmation-" + accessLog.getId() + ".pdf")
                        .build()
                        .toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @GetMapping("/server-access/admin/{id}/signature")
    public ResponseEntity<byte[]> signature(@PathVariable Long id) {
        AccessLog accessLog = accessLogService.get(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(accessLog.getSignatureContentType()))
                .body(accessLog.getSignatureImage());
    }

    private boolean isAdminAuthenticated(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(ADMIN_AUTH_SESSION_KEY));
    }
}
