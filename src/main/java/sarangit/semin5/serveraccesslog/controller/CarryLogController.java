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
import sarangit.semin5.serveraccesslog.domain.CarryLog;
import sarangit.semin5.serveraccesslog.service.CarryLogLedgerPdfService;
import sarangit.semin5.serveraccesslog.service.CarryLogPdfService;
import sarangit.semin5.serveraccesslog.service.CarryLogService;
import sarangit.semin5.serveraccesslog.web.CarryLogForm;

@Controller
public class CarryLogController {

    private static final String ADMIN_PASSWORD = "love!@0924";
    private static final String ADMIN_AUTH_SESSION_KEY = "carryAdminAuthenticated";
    private static final DateTimeFormatter COMPACT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CarryLogService carryLogService;
    private final CarryLogPdfService carryLogPdfService;
    private final CarryLogLedgerPdfService carryLogLedgerPdfService;

    public CarryLogController(
            CarryLogService carryLogService,
            CarryLogPdfService carryLogPdfService,
            CarryLogLedgerPdfService carryLogLedgerPdfService
    ) {
        this.carryLogService = carryLogService;
        this.carryLogPdfService = carryLogPdfService;
        this.carryLogLedgerPdfService = carryLogLedgerPdfService;
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

    @GetMapping("/server-carry")
    public String form(Model model, HttpSession session) {
        session.removeAttribute(ADMIN_AUTH_SESSION_KEY);
        if (!model.containsAttribute("carryLogForm")) {
            CarryLogForm form = new CarryLogForm();
            form.setVisitedAt(LocalDateTime.now());
            model.addAttribute("carryLogForm", form);
        }
        return "carry-log/form";
    }

    @PostMapping("/server-carry/logs")
    public String create(
            @Valid @ModelAttribute CarryLogForm carryLogForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        validateSignature(carryLogForm, bindingResult);
        if (!carryLogForm.isPrivacyAgreed()) {
            bindingResult.rejectValue("privacyAgreed", "privacy.required", "개인정보 수집 및 이용에 동의해주세요.");
        }
        if (bindingResult.hasErrors()) {
            return "carry-log/form";
        }

        CarryLog saved;
        try {
            saved = carryLogService.create(carryLogForm);
        } catch (IllegalArgumentException e) {
            bindingResult.reject("carryLog.invalid", e.getMessage());
            return "carry-log/form";
        }
        redirectAttributes.addAttribute("id", saved.getId());
        return "redirect:/server-carry/complete/{id}";
    }

    @GetMapping("/server-carry/complete/{id}")
    public String complete(@PathVariable Long id, Model model) {
        model.addAttribute("carryLog", carryLogService.get(id));
        return "carry-log/complete";
    }

    @GetMapping("/server-carry/admin/login")
    public String adminLogin() {
        return "carry-admin/login";
    }

    @PostMapping("/server-carry/admin/login")
    public String adminLogin(@RequestParam String password, HttpSession session, Model model) {
        if (ADMIN_PASSWORD.equals(password)) {
            session.setAttribute(ADMIN_AUTH_SESSION_KEY, true);
            return "redirect:/server-carry/admin";
        }
        model.addAttribute("error", "비밀번호가 올바르지 않습니다.");
        return "carry-admin/login";
    }

    @GetMapping("/server-carry/admin")
    public String admin(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String managerName,
            Model model,
            HttpSession session
    ) {
        if (!isAdminAuthenticated(session)) {
            return "redirect:/server-carry/admin/login";
        }

        LocalDate today = LocalDate.now();
        LocalDate resolvedEndDate = endDate == null ? today : endDate;
        LocalDate resolvedStartDate = startDate == null ? today.minusMonths(1) : startDate;

        model.addAttribute("carryLogs", carryLogService.findByWorkDateRange(resolvedStartDate, resolvedEndDate, managerName));
        model.addAttribute("managerNames", carryLogService.managerNames());
        model.addAttribute("startDate", resolvedStartDate);
        model.addAttribute("endDate", resolvedEndDate);
        model.addAttribute("managerName", managerName == null ? "" : managerName);
        model.addAttribute("currentYear", today.getYear());
        model.addAttribute("today", today);
        return "carry-admin/list";
    }

    @GetMapping("/server-carry/admin/{id}/edit")
    public String edit(@PathVariable Long id, Model model, HttpSession session) {
        if (!isAdminAuthenticated(session)) {
            return "redirect:/server-carry/admin/login";
        }

        CarryLog carryLog = carryLogService.get(id);
        if (!model.containsAttribute("carryLogForm")) {
            model.addAttribute("carryLogForm", CarryLogForm.from(carryLog));
        }
        model.addAttribute("carryLog", carryLog);
        return "carry-admin/edit";
    }

    @PostMapping("/server-carry/admin/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute CarryLogForm carryLogForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpSession session
    ) {
        if (!isAdminAuthenticated(session)) {
            return "redirect:/server-carry/admin/login";
        }

        CarryLog carryLog = carryLogService.get(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("carryLog", carryLog);
            return "carry-admin/edit";
        }

        try {
            carryLogService.update(id, carryLogForm);
        } catch (IllegalArgumentException e) {
            bindingResult.reject("carryLog.invalid", e.getMessage());
            model.addAttribute("carryLog", carryLog);
            return "carry-admin/edit";
        }
        redirectAttributes.addFlashAttribute("message", "반출·입 기록이 수정되었습니다.");
        return "redirect:/server-carry/admin";
    }

    @PostMapping("/server-carry/admin/{id}/checkout")
    public String checkout(
            @PathVariable Long id,
            @RequestParam String managerName,
            RedirectAttributes redirectAttributes,
            HttpSession session
    ) {
        if (!isAdminAuthenticated(session)) {
            return "redirect:/server-carry/admin/login";
        }

        try {
            carryLogService.checkout(id, managerName);
            redirectAttributes.addFlashAttribute("message", "퇴실 처리되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:/server-carry/admin";
    }

    @GetMapping("/server-carry/admin/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id, HttpSession session) {
        if (!isAdminAuthenticated(session)) {
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, "/server-carry/admin/login")
                    .build();
        }

        CarryLog carryLog = carryLogService.get(id);
        byte[] bytes = carryLogPdfService.createPdf(carryLog);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("server-carry-confirmation-" + carryLog.getId() + ".pdf")
                        .build()
                        .toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @PostMapping("/server-carry/admin/pdf")
    public ResponseEntity<byte[]> ledgerPdf(
            @RequestParam(name = "selectedIds", required = false) List<Long> selectedIds,
            @RequestParam(name = "ledgerYear", required = false) Integer ledgerYear,
            @RequestParam(name = "printDateEnabled", required = false, defaultValue = "false") boolean printDateEnabled,
            @RequestParam(name = "printDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate printDate,
            HttpSession session
    ) {
        if (!isAdminAuthenticated(session)) {
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, "/server-carry/admin/login")
                    .build();
        }

        LocalDate today = LocalDate.now();
        int resolvedLedgerYear = ledgerYear == null ? today.getYear() : ledgerYear;
        LocalDate resolvedPrintDate = printDateEnabled
                ? (printDate == null ? today : printDate)
                : null;
        List<CarryLog> carryLogs = selectedIds == null || selectedIds.isEmpty()
                ? List.of()
                : carryLogService.findByIds(selectedIds);
        byte[] bytes = carryLogLedgerPdfService.createLedgerPdf(
                carryLogs,
                resolvedLedgerYear,
                resolvedPrintDate
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("server-carry-logs.pdf")
                        .build()
                        .toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @GetMapping("/server-carry/admin/{id}/signature")
    public ResponseEntity<byte[]> signature(@PathVariable Long id) {
        CarryLog carryLog = carryLogService.get(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(carryLog.getSignatureContentType()))
                .body(carryLog.getSignatureImage());
    }

    private void validateSignature(CarryLogForm carryLogForm, BindingResult bindingResult) {
        if (!carryLogForm.hasSignature()) {
            bindingResult.rejectValue("signatureData", "signature.required", "서명을 입력해주세요.");
        }
    }

    private boolean isAdminAuthenticated(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(ADMIN_AUTH_SESSION_KEY));
    }
}
