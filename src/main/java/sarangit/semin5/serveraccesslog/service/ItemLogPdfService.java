package sarangit.semin5.serveraccesslog.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import sarangit.semin5.serveraccesslog.domain.ItemLog;

@Service
public class ItemLogPdfService {
    private static final String RECEIPT_TEMPLATE = "pdf/item-application-template.pdf";
    private static final String RENTAL_TEMPLATE = "pdf/item-application-rental-template.pdf";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] createPdf(ItemLog log) {
        try (PDDocument document = loadTemplate(log.isRental()); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDFont font;
            try (var stream = new ClassPathResource("fonts/malgun.ttf").getInputStream()) {
                font = PDType0Font.load(document, stream);
            }
            try (PDPageContentStream content = new PDPageContentStream(
                    document, document.getPage(0), AppendMode.APPEND, true, true)) {
                drawCentered(content, font, 10, 115, 712, 168, log.getRecipientName());
                drawCentered(content, font, 10, 380, 712, 168, log.getRecipientDepartment());
                if (log.isRental()) {
                    drawCentered(content, font, 10, 115, 656, 168,
                            log.getReceivedDate().format(DATE) + " "
                                    + (log.getCreatedAt() == null ? "" : log.getCreatedAt().format(TIME)));
                    drawCentered(content, font, 10, 380, 656, 168, log.getAssetNumber());
                    drawCentered(content, font, 10, 115, 629, 168,
                            log.getReturnedAt() == null ? "" : log.getReturnedAt().format(DATE_TIME));
                    drawCentered(content, font, 10, 380, 629, 168, log.getManagerName());
                    drawCentered(content, font, 10, 115, 602, 430, itemDescription(log));
                } else {
                    drawCentered(content, font, 10, 115, 656, 168, log.getReceivedDate().format(DATE));
                    drawCentered(content, font, 10, 380, 656, 168, log.getAssetNumber());
                    drawCentered(content, font, 10, 115, 629, 168, itemDescription(log));
                    drawCentered(content, font, 10, 380, 629, 168, log.getManagerName());
                }
                int shift = log.isRental() ? 27 : 0;
                drawWrapped(content, font, 10, 28, 579 - shift, 510, log.getReason(), 7, 15);
                drawText(content, font, 10, 418, 263 - shift, String.valueOf(log.getReceivedDate().getYear()));
                drawText(content, font, 10, 470, 263 - shift, String.format("%02d", log.getReceivedDate().getMonthValue()));
                drawText(content, font, 10, 520, 263 - shift, String.format("%02d", log.getReceivedDate().getDayOfMonth()));
                drawCentered(content, font, 10, 445, 224 - shift, 80, log.getRecipientName());
                drawSignature(document, content, log, 208 - shift);
            }
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("물품 신청서 PDF 생성 중 오류가 발생했습니다.", e);
        }
    }

    private PDDocument loadTemplate(boolean rental) throws IOException {
        String template = rental ? RENTAL_TEMPLATE : RECEIPT_TEMPLATE;
        ClassPathResource resource = new ClassPathResource(template);
        if (!resource.exists()) throw new IOException("PDF 양식 파일을 찾을 수 없습니다: " + template);
        return Loader.loadPDF(resource.getInputStream().readAllBytes());
    }

    private String itemDescription(ItemLog log) {
        return value(log.getItemName()) + " · " + log.getQuantity() + " EA";
    }

    private void drawCentered(PDPageContentStream content, PDFont font, float size,
                              float x, float y, float width, String text) throws IOException {
        String fitted = fit(value(text), font, size, width - 8);
        float textWidth = font.getStringWidth(fitted) / 1000 * size;
        drawText(content, font, size, x + (width - textWidth) / 2, y, fitted);
    }

    private void drawWrapped(PDPageContentStream content, PDFont font, float size,
                             float x, float y, float width, String text, int maxLines, float lineHeight) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (char ch : value(text).toCharArray()) {
            if (ch == '\n' || (!line.isEmpty() && font.getStringWidth(line.toString() + ch) / 1000 * size > width)) {
                lines.add(line.toString());
                line.setLength(0);
                if (ch == '\n') continue;
            }
            line.append(ch);
        }
        if (!line.isEmpty()) lines.add(line.toString());
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            drawText(content, font, size, x, y - i * lineHeight, lines.get(i));
        }
    }

    private void drawSignature(PDDocument document, PDPageContentStream content, ItemLog log, float y) throws IOException {
        byte[] bytes = log.getSignatureImage();
        if (bytes == null || bytes.length == 0) return;
        PDImageXObject image = PDImageXObject.createFromByteArray(document, bytes, "signature-" + log.getId());
        float scale = Math.min(60f / image.getWidth(), 35f / image.getHeight());
        float width = image.getWidth() * scale;
        float height = image.getHeight() * scale;
        content.drawImage(image, 524 + (60 - width) / 2, y + (35 - height) / 2, width, height);
    }

    private void drawText(PDPageContentStream content, PDFont font, float size,
                          float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(value(text));
        content.endText();
    }

    private String fit(String text, PDFont font, float size, float width) throws IOException {
        String fitted = text;
        while (!fitted.isEmpty() && font.getStringWidth(fitted) / 1000 * size > width) {
            fitted = fitted.substring(0, fitted.length() - 1);
        }
        return fitted;
    }

    private String value(String text) {
        return text == null ? "" : text.replaceAll("[\\p{Cntrl}&&[^\\n]]+", " ").trim();
    }
}
