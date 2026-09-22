package sarangit.semin5.serveraccesslog;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import sarangit.semin5.serveraccesslog.domain.CarryLog;
import sarangit.semin5.serveraccesslog.domain.ItemLog;
import sarangit.semin5.serveraccesslog.service.CarryLogPdfService;
import sarangit.semin5.serveraccesslog.service.ItemLogPdfService;
import sarangit.semin5.serveraccesslog.service.PdfService;
import sarangit.semin5.serveraccesslog.web.AccessLogForm;

class PdfTemplateAlignmentTests {
    @Test
    void rendersBothApplicationTemplates() throws Exception {
        byte[] signature = sampleSignature();
        LocalDate date = LocalDate.of(2026, 9, 21);
        LocalDateTime visitedAt = date.atTime(9, 30);
        AccessLogForm access = new AccessLogForm();
        access.setCompanyName("사랑병원 IT팀");
        access.setVisitorName("홍길동");
        access.setBirthDate(LocalDate.of(1990, 1, 1));
        access.setVisitedAt(visitedAt);
        access.setContent("서버 점검 및 네트워크 작업");

        CarryLog carry = new CarryLog("홍길동", date, "사랑병원 IT팀", "사원", visitedAt,
                "010-1234-5678", false, true, "서버 장비", false, false, null,
                "장비 교체 작업", "signature.png", "image/png", signature);

        Path output = Path.of("target", "pdf-review");
        Files.createDirectories(output);
        byte[] accessPdf = new PdfService().createAccessLogPdf(access, signature);
        byte[] carryPdf = new CarryLogPdfService().createPdf(carry);
        assertThat(accessPdf).startsWith("%PDF".getBytes());
        assertThat(carryPdf).startsWith("%PDF".getBytes());
        Files.write(output.resolve("access-application.pdf"), accessPdf);
        Files.write(output.resolve("carry-application.pdf"), carryPdf);
    }

    @Test
    void rendersRentalItemInFullWidthItemCell() throws Exception {
        ItemLog item = new ItemLog(true, LocalDate.of(2026, 9, 22), "홍길동", "IT팀",
                "노트북", 1, "A-123", "업무용 대여", "signature.png", "image/png", sampleSignature());
        byte[] pdf = new ItemLogPdfService().createPdf(item);
        assertThat(pdf).startsWith("%PDF".getBytes());
        Path output = Path.of("target", "pdf-review");
        Files.createDirectories(output);
        Files.write(output.resolve("item-rental-application.pdf"), pdf);
    }

    private byte[] sampleSignature() throws Exception {
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
        return out.toByteArray();
    }
}
