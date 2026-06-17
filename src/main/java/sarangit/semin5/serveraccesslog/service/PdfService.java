package sarangit.semin5.serveraccesslog.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import sarangit.semin5.serveraccesslog.web.AccessLogForm;

@Service
public class PdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] createAccessLogPdf(AccessLogForm form, byte[] signatureImage) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDFont font = loadKoreanFont(document);
            PDImageXObject signature = PDImageXObject.createFromByteArray(document, signatureImage, "signature");

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                drawTitle(content, font);
                float y = 720;
                y = drawRow(content, font, y, "소속", form.getCompanyName());
                y = drawRow(content, font, y, "성명", form.getVisitorName());
                y = drawRow(content, font, y, "생년월일", form.getBirthDate().format(DATE_FORMATTER));
                y = drawRow(content, font, y, "출입 시간", form.getVisitedAt().format(DATE_TIME_FORMATTER));
                y = drawMultilineRow(content, font, y, "출입사유", form.getContent());
                drawSignature(content, font, signature, y - 18, form.getVisitorName());
            }

            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("PDF 생성 중 오류가 발생했습니다.", e);
        }
    }

    private PDFont loadKoreanFont(PDDocument document) throws IOException {
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
        throw new IOException("한글 PDF 생성을 위한 시스템 폰트를 찾을 수 없습니다.");
    }

    private void drawTitle(PDPageContentStream content, PDFont font) throws IOException {
        content.beginText();
        content.setFont(font, 22);
        content.newLineAtOffset(72, 780);
        content.showText("서버 출입 대장");
        content.endText();
        content.moveTo(72, 760);
        content.lineTo(523, 760);
        content.stroke();
    }

    private float drawRow(PDPageContentStream content, PDFont font, float y, String label, String value) throws IOException {
        drawText(content, font, 11, 78, y, label);
        drawText(content, font, 12, 170, y, value);
        drawLine(content, y - 10);
        return y - 38;
    }

    private float drawMultilineRow(PDPageContentStream content, PDFont font, float y, String label, String value) throws IOException {
        drawText(content, font, 11, 78, y, label);
        List<String> lines = wrapText(value, font, 12, 340);
        float lineY = y;
        for (String line : lines) {
            drawText(content, font, 12, 170, lineY, line);
            lineY -= 18;
        }
        float nextY = Math.min(y - 58, lineY - 8);
        drawLine(content, nextY + 10);
        return nextY - 28;
    }

    private void drawSignature(PDPageContentStream content, PDFont font, PDImageXObject signature, float y, String visitorName)
            throws IOException {
        drawText(content, font, 11, 78, y, "서명");
        content.addRect(170, y - 88, 220, 90);
        content.stroke();

        float maxWidth = 200;
        float maxHeight = 70;
        float scale = Math.min(maxWidth / signature.getWidth(), maxHeight / signature.getHeight());
        float width = signature.getWidth() * scale;
        float height = signature.getHeight() * scale;
        content.drawImage(signature, 180, y - 78 + ((maxHeight - height) / 2), width, height);
        drawText(content, font, 10, 410, y - 78, visitorName + " 서명");
    }

    private void drawText(PDPageContentStream content, PDFont font, int size, float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(text == null ? "" : text);
        content.endText();
    }

    private void drawLine(PDPageContentStream content, float y) throws IOException {
        content.moveTo(72, y);
        content.lineTo(523, y);
        content.stroke();
    }

    private List<String> wrapText(String text, PDFont font, int fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        String normalized = value(text).replace("\r", "");
        for (String paragraph : normalized.split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (font.getStringWidth(candidate) / 1000 * fontSize <= maxWidth) {
                    line = new StringBuilder(candidate);
                } else {
                    if (!line.isEmpty()) {
                        lines.add(line.toString());
                    }
                    line = new StringBuilder(word);
                }
            }
            lines.add(line.toString());
        }
        return lines.isEmpty() ? List.of("") : lines;
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
