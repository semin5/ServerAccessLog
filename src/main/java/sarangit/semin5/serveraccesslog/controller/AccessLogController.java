package sarangit.semin5.serveraccesslog.controller;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sarangit.semin5.serveraccesslog.domain.AccessLog;
import sarangit.semin5.serveraccesslog.service.AccessLogExcelService;
import sarangit.semin5.serveraccesslog.service.AccessLogService;
import sarangit.semin5.serveraccesslog.web.AccessLogForm;

@Controller
public class AccessLogController {

    private final AccessLogService accessLogService;
    private final AccessLogExcelService accessLogExcelService;

    public AccessLogController(AccessLogService accessLogService, AccessLogExcelService accessLogExcelService) {
        this.accessLogService = accessLogService;
        this.accessLogExcelService = accessLogExcelService;
    }

    @GetMapping("/")
    public String form(Model model) {
        if (!model.containsAttribute("accessLogForm")) {
            AccessLogForm form = new AccessLogForm();
            form.setVisitedAt(LocalDateTime.now());
            model.addAttribute("accessLogForm", form);
        }
        return "access-log/form";
    }

    @PostMapping("/access-logs")
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
        return "redirect:/complete/{id}";
    }

    @GetMapping("/complete/{id}")
    public String complete(@PathVariable Long id, Model model) {
        model.addAttribute("accessLog", accessLogService.get(id));
        return "access-log/complete";
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("accessLogs", accessLogService.findAll());
        model.addAttribute("exitGuideNames", accessLogService.exitGuideNames());
        return "admin/list";
    }

    @GetMapping("/admin/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        AccessLog accessLog = accessLogService.get(id);
        if (!model.containsAttribute("accessLogForm")) {
            model.addAttribute("accessLogForm", AccessLogForm.from(accessLog));
        }
        model.addAttribute("accessLog", accessLog);
        return "admin/edit";
    }

    @PostMapping("/admin/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute AccessLogForm accessLogForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
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
        redirectAttributes.addFlashAttribute("message", "출입 기록이 수정되었습니다.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/{id}/checkout")
    public String checkout(
            @PathVariable Long id,
            @RequestParam String exitGuideName,
            RedirectAttributes redirectAttributes
    ) {
        try {
            accessLogService.checkout(id, exitGuideName);
            redirectAttributes.addFlashAttribute("message", "퇴실 처리되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:/admin";
    }

    @GetMapping("/admin/excel")
    public ResponseEntity<byte[]> excel() {
        byte[] bytes = accessLogExcelService.createWorkbook(accessLogService.findAll());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("server-access-logs.xlsx")
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/admin/{id}/signature")
    public ResponseEntity<byte[]> signature(@PathVariable Long id) {
        AccessLog accessLog = accessLogService.get(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(accessLog.getSignatureContentType()))
                .body(accessLog.getSignatureImage());
    }
}
