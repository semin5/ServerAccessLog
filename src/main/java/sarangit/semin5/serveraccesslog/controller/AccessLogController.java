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
            bindingResult.rejectValue("signatureData", "signature.required", "서명을 그리거나 JPG 파일을 올려주세요.");
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
        return "admin/list";
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

    @GetMapping("/admin/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        AccessLog accessLog = accessLogService.get(id);
        String filename = "server-access-log-" + accessLog.getId() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename)
                        .build()
                        .toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(accessLog.getPdfFile());
    }

}
