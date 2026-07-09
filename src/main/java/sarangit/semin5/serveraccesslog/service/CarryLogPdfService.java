package sarangit.semin5.serveraccesslog.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import sarangit.semin5.serveraccesslog.domain.CarryLog;

@Service
public class CarryLogPdfService {

    private static final String TEMPLATE_PDF_PATH = "pdf/carry-confirmation-template.pdf";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public byte[] createPdf(CarryLog log) {
        try (PDDocument document = loadTemplate(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            applyDocumentTitle(document);
            PDFont font = loadKoreanFont(document);
            PDPage page = document.getPage(0);

            try (PDPageContentStream content = new PDPageContentStream(document, page, AppendMode.APPEND, true, true)) {
                drawDocument(document, content, font, log);
            }

            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("반출입 신청서 PDF 생성 중 오류가 발생했습니다.", e);
        }
    }

    private void applyDocumentTitle(PDDocument document) {
        PDDocumentInformation information = document.getDocumentInformation();
        information.setTitle("IT팀 출입통제");
        document.setDocumentInformation(information);
    }

    private PDDocument loadTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PDF_PATH);
        if (!resource.exists()) {
            throw new IOException("PDF 양식 파일을 찾을 수 없습니다: " + TEMPLATE_PDF_PATH);
        }
        return Loader.loadPDF(resource.getInputStream().readAllBytes());
    }

    private void drawDocument(PDDocument document, PDPageContentStream content, PDFont font, CarryLog log) throws IOException {
        drawCentered(content, font, 10, 128, 695, 169, log.getVisitorName());
        drawCentered(content, font, 10, 394, 695, 170, log.getCompanyName());
        drawCentered(content, font, 10, 128, 668, 169, log.getJobTitle());
        drawCentered(content, font, 10, 394, 668, 170, log.getContact());

        drawCentered(content, font, 10, 128, 611, 169, log.getWorkDate().format(DATE_FORMATTER));
        drawCentered(content, font, 10, 394, 611, 170, log.getVisitedAt().format(TIME_FORMATTER));
        drawCentered(content, font, 10, 394, 584, 170, log.getExitedAt() == null ? "" : log.getExitedAt().format(TIME_FORMATTER));
        drawCentered(content, font, 10, 394, 557, 170, guideName(log.getExitGuideName()));

        drawCheck(content, log.isEquipmentInbound(), 109.4f, 585);
        drawCheck(content, log.isEquipmentOutbound(), 109.4f, 585);
        drawText(content, font, 10, 200, 584, value(log.getEquipmentModelName()));
        drawCheck(content, log.isStorageInbound(), 109.4f, 558);
        drawCheck(content, log.isStorageOutbound(), 109.4f, 558);
        drawText(content, font, 10, 200, 557, value(log.getStorageModelName()));

        drawWrapped(content, font, 10, 48, 510, 500, log.getWorkContent(), 5, 18);

        LocalDate pledgeDate = log.getWorkDate() == null ? LocalDate.now() : log.getWorkDate();
        drawText(content, font, 10, 424, 193, String.valueOf(pledgeDate.getYear()));
        drawText(content, font, 10, 489, 193, String.format("%02d", pledgeDate.getMonthValue()));
        drawText(content, font, 10, 536, 193, String.format("%02d", pledgeDate.getDayOfMonth()));
        drawCentered(content, font, 10, 452, 153, 92, log.getVisitorName());
        drawSignature(document, content, log, 536, 136, 58, 38);
    }

    private PDFont loadKoreanFont(PDDocument document) throws IOException {
        ClassPathResource embeddedFont = new ClassPathResource("fonts/malgun.ttf");
        if (embeddedFont.exists()) {
            try (var inputStream = embeddedFont.getInputStream()) {
                return PDType0Font.load(document, inputStream);
            }
        }
        List<String> candidates = List.of(
                "C:/Windows/Fonts/malgun.ttf",
                "C:/Windows/Fonts/gulim.ttc",
                "/System/Library/Fonts/AppleSDGothicNeo.ttc",
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"
        );
        for (String path : candidates) {
            File file = new File(path);
            if (file.exists()) {
                return PDType0Font.load(document, file);
            }
        }
        throw new IOException("PDF 생성을 위한 한글 폰트를 찾을 수 없습니다.");
    }

    private void drawCheck(PDPageContentStream content, boolean checked, float x, float y) throws IOException {
        if (!checked) {
            return;
        }
        content.setLineWidth(1.2f);
        content.moveTo(x + 2, y + 4);
        content.lineTo(x + 5, y + 1);
        content.lineTo(x + 11, y + 10);
        content.stroke();
    }

    private void drawSignature(PDDocument document, PDPageContentStream content, CarryLog log, float x, float y, float maxWidth, float maxHeight)
            throws IOException {
        byte[] signatureImage = log.getSignatureImage();
        if (signatureImage == null || signatureImage.length == 0) {
            return;
        }
        PDImageXObject image = PDImageXObject.createFromByteArray(document, signatureImage, "signature-" + log.getId());
        float scale = Math.min(maxWidth / image.getWidth(), maxHeight / image.getHeight());
        float width = image.getWidth() * scale;
        float height = image.getHeight() * scale;
        content.drawImage(image, x + ((maxWidth - width) / 2), y + ((maxHeight - height) / 2), width, height);
    }

    private void drawText(PDPageContentStream content, PDFont font, float size, float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(value(text));
        content.endText();
    }

    private void drawCentered(PDPageContentStream content, PDFont font, float size, float x, float y, float width, String text)
            throws IOException {
        String fitted = trimToWidth(value(text), font, size, width - 4);
        float textWidth = font.getStringWidth(fitted) / 1000 * size;
        drawText(content, font, size, x + Math.max(2, (width - textWidth) / 2), y, fitted);
    }

    private void drawWrapped(PDPageContentStream content, PDFont font, float size, float x, float y, float width, String text, int maxLines, float lineHeight)
            throws IOException {
        List<String> lines = wrapText(value(text), font, size, width);
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            drawText(content, font, size, x, y - (i * lineHeight), lines.get(i));
        }
    }

    private List<String> wrapText(String text, PDFont font, float size, float width) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String candidate = line.toString() + text.charAt(i);
            if (font.getStringWidth(candidate) / 1000 * size <= width) {
                line.append(text.charAt(i));
            } else {
                lines.add(line.toString());
                line = new StringBuilder(String.valueOf(text.charAt(i)));
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines.isEmpty() ? List.of("") : lines;
    }

    private String trimToWidth(String text, PDFont font, float size, float width) throws IOException {
        String result = value(text);
        while (!result.isEmpty() && font.getStringWidth(result) / 1000 * size > width) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String guideName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().split("\\s+")[0];
    }

    private String value(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("\\p{Cntrl}+", " ").trim();
    }
}
