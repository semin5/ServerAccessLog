package sarangit.semin5.serveraccesslog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import sarangit.semin5.serveraccesslog.domain.ItemLog;
import sarangit.semin5.serveraccesslog.repository.ItemLogRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ItemLogFlowTests {
    @Autowired MockMvc mockMvc;
    @Autowired ItemLogRepository repository;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepareDepartments() {
        repository.deleteAll();
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS worklog");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS worklog.department (id INT PRIMARY KEY, middle_category VARCHAR(255), name VARCHAR(255) NOT NULL UNIQUE, is_active TINYINT NOT NULL DEFAULT 1)");
        jdbcTemplate.execute("MERGE INTO worklog.department (id, middle_category, name, is_active) KEY(id) VALUES (1, NULL, '전산', 1)");
        jdbcTemplate.execute("MERGE INTO worklog.department (id, middle_category, name, is_active) KEY(id) VALUES (2, '진료부', '영상의학', 0)");
        jdbcTemplate.execute("MERGE INTO worklog.department (id, middle_category, name, is_active) KEY(id) VALUES (3, '진료부', '검사실', 1)");
    }

    @Test
    void unreturnedRentalsRemainVisibleOutsideDateRange() throws Exception {
        LocalDate today = LocalDate.now();
        String itemName = "기간외 미반납 테스트 물품";
        byte[] signature = Base64.getDecoder().decode(sampleSignatureData().split(",", 2)[1]);
        ItemLog open = repository.save(new ItemLog(true, today.minusYears(1), "대여자", "전산",
                itemName, 1, null, "대여", "sign.png", "image/png", signature));
        ItemLog returned = new ItemLog(true, today.minusYears(1), "반납자", "전산",
                itemName, 1, null, "대여", "sign.png", "image/png", signature);
        returned.returnItem();
        returned = repository.save(returned);
        ItemLog current = repository.save(new ItemLog(false, today, "수령자", "전산",
                itemName, 1, null, "수령", "sign.png", "image/png", signature));

        var visible = repository.findForAdmin(today.minusMonths(1), today, itemName, null, null);
        assertThat(visible).extracting(ItemLog::getId).contains(open.getId(), current.getId())
                .doesNotContain(returned.getId());
        assertThat(visible).extracting(ItemLog::getId).containsExactly(open.getId(), current.getId());
        assertThat(repository.findForAdmin(today.minusMonths(1), today, itemName, null, false))
                .extracting(ItemLog::getId).containsExactly(current.getId());
    }

    @Test
    void itemRegisterWorkflow() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<p class=\"eyebrow\">Register</p>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("IT팀 관리대장")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("물품 관리대장")));

        mockMvc.perform(get("/item-register"))
                .andExpect(status().isOk())
                .andExpect(view().name("item-log/form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<select required id=\"recipientDepartment\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"전산\"")));

        mockMvc.perform(get("/item-register"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-middle-category=\"진료부\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"검사실\"")));

        mockMvc.perform(get("/item-register"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("value=\"영상의학\""))));

        mockMvc.perform(post("/item-register/logs")
                        .param("receivedDate", LocalDate.now().toString())
                        .param("recipientName", "테스트 수령인")
                        .param("recipientDepartment", "없는부서")
                        .param("itemName", "테스트 노트북")
                        .param("reason", "업무용 대여")
                        .param("signatureData", sampleSignatureData())
                        .param("privacyAgreed", "true"))
                .andExpect(view().name("item-log/form"))
                .andExpect(model().attributeHasFieldErrors("itemLogForm", "recipientDepartment"));

        mockMvc.perform(post("/item-register/logs")
                        .param("rental", "true")
                        .param("receivedDate", LocalDate.now().toString())
                        .param("recipientName", "테스트 수령인")
                        .param("recipientDepartment", "전산")
                        .param("itemName", "테스트 노트북")
                        .param("assetNumber", "ASSET-1")
                        .param("reason", "업무용 대여")
                        .param("signatureData", sampleSignatureData())
                        .param("privacyAgreed", "true"))
                .andExpect(status().is3xxRedirection());

        ItemLog log = repository.findAll().getFirst();
        assertThat(log.getQuantity()).isEqualTo(1);
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/item-register/admin/login").session(session).param("password", "love!@0924"))
                .andExpect(redirectedUrl("/item-register/admin"));
        mockMvc.perform(get("/item-register/admin/{id}/edit", log.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<select required id=\"recipientDepartment\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"전산\"")));
        mockMvc.perform(get("/item-register/admin").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("테스트 노트북")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("대여 여부")));
        mockMvc.perform(post("/item-register/admin/{id}/confirm", log.getId()).session(session)
                        .param("managerName", "오세민 사원"))
                .andExpect(redirectedUrl("/item-register/admin"));
        mockMvc.perform(get("/item-register/admin").session(session)
                        .param("managerName", "오세민 사원"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("테스트 노트북")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">반납</button>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"rental-badge\">대여</span>")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("확인 20"))));
        mockMvc.perform(post("/item-register/admin/{id}/return", log.getId()).session(session))
                .andExpect(redirectedUrl("/item-register/admin"));
        byte[] pdf = mockMvc.perform(get("/item-register/admin/{id}/pdf", log.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(pdf).startsWith("%PDF".getBytes());
        Path preview = Path.of("target", "pdf-review", "item-application.pdf");
        Files.createDirectories(preview.getParent());
        Files.write(preview, pdf);
        assertThat(pdfText(pdf)).contains("반납 일자", "테스트 노트북 · 1 EA");

        ItemLog completed = repository.findById(log.getId()).orElseThrow();
        assertThat(completed.getConfirmedAt()).isNotNull();
        assertThat(completed.getReturnedAt()).isNotNull();
        LocalDateTime returnedAt = completed.getReturnedAt();
        mockMvc.perform(get("/item-register/admin").session(session).param("rental", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"rental-badge rental-badge--returned\">반납</span>")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("반납완료"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("반납시간")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("rental-badge")));
        mockMvc.perform(get("/item-register/admin").session(session).param("rental", "false"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("itemLogs", org.hamcrest.Matchers.empty()));

        editRental(log.getId(), session, false);
        ItemLog receipt = repository.findById(log.getId()).orElseThrow();
        assertThat(receipt.isRental()).isFalse();
        assertThat(receipt.getReturnedAt()).isEqualTo(returnedAt);
        byte[] receiptPdf = mockMvc.perform(get("/item-register/admin/{id}/pdf", log.getId()).session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(pdfText(receiptPdf)).doesNotContain("반납 일자").contains("테스트 노트북 · 1 EA");
        Files.write(Path.of("target", "pdf-review", "item-application-receipt.pdf"), receiptPdf);

        editRental(log.getId(), session, true);
        ItemLog rentedAgain = repository.findById(log.getId()).orElseThrow();
        assertThat(rentedAgain.getReturnedAt()).isEqualTo(returnedAt);
        byte[] rentalPdf = mockMvc.perform(get("/item-register/admin/{id}/pdf", log.getId()).session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(pdfText(rentalPdf)).contains("반납 일자", "테스트 노트북 · 1 EA");
        Files.write(Path.of("target", "pdf-review", "item-application-rental.pdf"), rentalPdf);
    }

    private void editRental(Long id, MockHttpSession session, boolean rental) throws Exception {
        mockMvc.perform(post("/item-register/admin/{id}/edit", id).session(session)
                        .param("rental", Boolean.toString(rental))
                        .param("receivedDate", LocalDate.now().toString())
                        .param("recipientName", "테스트 수령인")
                        .param("recipientDepartment", "전산")
                        .param("itemName", "테스트 노트북")
                        .param("quantity", "1")
                        .param("assetNumber", "ASSET-1")
                        .param("reason", "업무용 대여"))
                .andExpect(redirectedUrl("/item-register/admin"));
    }

    private String pdfText(byte[] pdf) throws Exception {
        try (var document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String sampleSignatureData() throws Exception {
        BufferedImage image = new BufferedImage(400, 100, BufferedImage.TYPE_INT_RGB);
        var pen = image.createGraphics();
        pen.setColor(Color.WHITE);
        pen.fillRect(0, 0, 400, 100);
        pen.setColor(Color.BLACK);
        pen.setStroke(new BasicStroke(4));
        pen.drawLine(55, 65, 145, 30);
        pen.drawLine(145, 30, 235, 65);
        pen.drawLine(235, 65, 345, 35);
        pen.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }
}
