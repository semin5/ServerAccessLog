package sarangit.semin5.serveraccesslog.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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
import sarangit.semin5.serveraccesslog.domain.ItemLog;
import sarangit.semin5.serveraccesslog.service.ItemLogPdfService;
import sarangit.semin5.serveraccesslog.service.ItemLogService;
import sarangit.semin5.serveraccesslog.web.ItemLogForm;

@Controller
public class ItemLogController {
    private static final String ADMIN_PASSWORD = "love!@0924";
    private static final String ADMIN_AUTH_SESSION_KEY = "itemAdminAuthenticated";

    private final ItemLogService service;
    private final ItemLogPdfService pdfService;

    public ItemLogController(ItemLogService service, ItemLogPdfService pdfService) {
        this.service = service;
        this.pdfService = pdfService;
    }

    @GetMapping("/item-register")
    public String form(Model model, HttpSession session) {
        session.removeAttribute(ADMIN_AUTH_SESSION_KEY);
        if (!model.containsAttribute("itemLogForm")) {
            ItemLogForm form = new ItemLogForm();
            form.setReceivedDate(LocalDate.now());
            model.addAttribute("itemLogForm", form);
        }
        addFormOptions(model, null);
        return "item-log/form";
    }

    @PostMapping("/item-register/logs")
    public String create(@Valid @ModelAttribute ItemLogForm itemLogForm, BindingResult bindingResult,
                         Model model, RedirectAttributes redirectAttributes) {
        validateCreate(itemLogForm, bindingResult);
        validateDepartment(itemLogForm, bindingResult, null);
        if (bindingResult.hasErrors()) {
            addFormOptions(model, null);
            return "item-log/form";
        }
        try {
            ItemLog saved = service.create(itemLogForm);
            redirectAttributes.addAttribute("id", saved.getId());
            return "redirect:/item-register/complete/{id}";
        } catch (IllegalArgumentException e) {
            bindingResult.reject("itemLog.invalid", e.getMessage());
            addFormOptions(model, null);
            return "item-log/form";
        }
    }

    @GetMapping("/item-register/complete/{id}")
    public String complete(@PathVariable Long id, Model model) {
        model.addAttribute("itemLog", service.get(id));
        return "item-log/complete";
    }

    @GetMapping("/item-register/admin/login")
    public String login() { return "item-admin/login"; }

    @PostMapping("/item-register/admin/login")
    public String login(@RequestParam String password, HttpSession session, Model model) {
        if (ADMIN_PASSWORD.equals(password)) {
            session.setAttribute(ADMIN_AUTH_SESSION_KEY, true);
            return "redirect:/item-register/admin";
        }
        model.addAttribute("error", "비밀번호가 올바르지 않습니다.");
        return "item-admin/login";
    }

    @GetMapping("/item-register/admin")
    public String admin(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                        @RequestParam(required = false) String itemName,
                        @RequestParam(required = false) String managerName,
                        @RequestParam(required = false) Boolean rental,
                        Model model, HttpSession session) {
        if (!authenticated(session)) return "redirect:/item-register/admin/login";
        LocalDate today = LocalDate.now();
        LocalDate resolvedStart = startDate == null ? today.minusMonths(1) : startDate;
        LocalDate resolvedEnd = endDate == null ? today : endDate;
        model.addAttribute("itemLogs", service.find(resolvedStart, resolvedEnd, itemName, managerName, rental));
        model.addAttribute("itemNames", service.itemNames());
        model.addAttribute("managerNames", service.managerNames());
        model.addAttribute("startDate", resolvedStart);
        model.addAttribute("endDate", resolvedEnd);
        model.addAttribute("itemName", itemName == null ? "" : itemName);
        model.addAttribute("managerName", managerName == null ? "" : managerName);
        model.addAttribute("rental", rental);
        return "item-admin/list";
    }

    @GetMapping("/item-register/admin/{id}/edit")
    public String edit(@PathVariable Long id, Model model, HttpSession session) {
        if (!authenticated(session)) return "redirect:/item-register/admin/login";
        ItemLog log = service.get(id);
        if (!model.containsAttribute("itemLogForm")) model.addAttribute("itemLogForm", ItemLogForm.from(log));
        model.addAttribute("itemLog", log);
        addFormOptions(model, log);
        return "item-admin/edit";
    }

    @PostMapping("/item-register/admin/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute ItemLogForm itemLogForm,
                         BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes,
                         HttpSession session) {
        if (!authenticated(session)) return "redirect:/item-register/admin/login";
        ItemLog log = service.get(id);
        validateDepartment(itemLogForm, bindingResult, log);
        if (bindingResult.hasErrors()) {
            model.addAttribute("itemLog", log);
            addFormOptions(model, log);
            return "item-admin/edit";
        }
        try {
            service.update(id, itemLogForm);
            redirectAttributes.addFlashAttribute("message", "물품 기록이 수정되었습니다.");
            return "redirect:/item-register/admin";
        } catch (IllegalArgumentException e) {
            bindingResult.reject("itemLog.invalid", e.getMessage());
            model.addAttribute("itemLog", log);
            addFormOptions(model, log);
            return "item-admin/edit";
        }
    }

    @PostMapping("/item-register/admin/{id}/confirm")
    public String confirm(@PathVariable Long id, @RequestParam String managerName,
                          RedirectAttributes redirectAttributes, HttpSession session) {
        if (!authenticated(session)) return "redirect:/item-register/admin/login";
        try {
            service.confirm(id, managerName);
            redirectAttributes.addFlashAttribute("message", "담당자 확인이 완료되었습니다.");
        } catch (IllegalArgumentException e) { redirectAttributes.addFlashAttribute("message", e.getMessage()); }
        return "redirect:/item-register/admin";
    }

    @PostMapping("/item-register/admin/{id}/return")
    public String returnItem(@PathVariable Long id, RedirectAttributes redirectAttributes, HttpSession session) {
        if (!authenticated(session)) return "redirect:/item-register/admin/login";
        try {
            service.returnItem(id);
            redirectAttributes.addFlashAttribute("message", "반납 처리되었습니다.");
        } catch (IllegalArgumentException e) { redirectAttributes.addFlashAttribute("message", e.getMessage()); }
        return "redirect:/item-register/admin";
    }

    @PostMapping("/item-register/admin/{id}/remarks")
    public String remarks(@PathVariable Long id, @RequestParam(required = false) String remarks,
                          RedirectAttributes redirectAttributes, HttpSession session) {
        if (!authenticated(session)) return "redirect:/item-register/admin/login";
        try {
            service.updateRemarks(id, remarks);
            redirectAttributes.addFlashAttribute("message", "비고가 저장되었습니다.");
        } catch (IllegalArgumentException e) { redirectAttributes.addFlashAttribute("message", e.getMessage()); }
        return "redirect:/item-register/admin";
    }

    @GetMapping("/item-register/admin/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id,
                                      @RequestParam(defaultValue = "false") boolean download,
                                      HttpSession session) {
        if (!authenticated(session)) return ResponseEntity.status(302).header(HttpHeaders.LOCATION, "/item-register/admin/login").build();
        ItemLog log = service.get(id);
        ContentDisposition disposition = (download ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename("item-application-" + id + ".pdf").build();
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF).body(pdfService.createPdf(log));
    }

    @GetMapping("/item-register/admin/{id}/signature")
    public ResponseEntity<byte[]> signature(@PathVariable Long id, HttpSession session) {
        if (!authenticated(session)) return ResponseEntity.status(302).header(HttpHeaders.LOCATION, "/item-register/admin/login").build();
        ItemLog log = service.get(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(log.getSignatureContentType())).body(log.getSignatureImage());
    }

    private void validateCreate(ItemLogForm form, BindingResult errors) {
        if (!form.hasSignature()) errors.rejectValue("signatureData", "signature.required", "서명을 입력해주세요.");
        if (!form.isPrivacyAgreed()) errors.rejectValue("privacyAgreed", "privacy.required", "개인정보 수집 및 이용에 동의해주세요.");
    }

    private void validateDepartment(ItemLogForm form, BindingResult errors, ItemLog existing) {
        String department = form.getRecipientDepartment();
        if (department == null || department.isBlank()) return;
        if (!service.departmentNames().contains(department)
                && (existing == null || !department.equals(existing.getRecipientDepartment()))) {
            errors.rejectValue("recipientDepartment", "department.invalid", "목록에서 부서를 선택해주세요.");
        }
    }

    private void addFormOptions(Model model, ItemLog existing) {
        model.addAttribute("itemNames", service.itemNames());
        List<ItemLogService.DepartmentOption> departments = new ArrayList<>(service.departmentOptions());
        if (existing != null && departments.stream().noneMatch(item -> item.name().equals(existing.getRecipientDepartment()))) {
            departments.add(new ItemLogService.DepartmentOption(existing.getRecipientDepartment(), null));
        }
        model.addAttribute("departmentOptions", departments);
    }

    private boolean authenticated(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(ADMIN_AUTH_SESSION_KEY));
    }
}
