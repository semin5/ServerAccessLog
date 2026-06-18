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
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import sarangit.semin5.serveraccesslog.domain.AccessLog;

@Service
public class AccessLogBatchPdfService {

    private static final String TEMPLATE_PDF_PATH = "pdf/access-ledger-template.pdf";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final int ROWS_PER_PAGE = 17;
    private static final float PAGE_HEIGHT = 595;
    private static final float ROW_TOP = 125.69f;
    private static final float ROW_HEIGHT = 21.3f;
    private static final float[] COLUMN_X = {
            59.45f, 125.74f, 183.16f, 240.58f, 297.99f, 355.41f, 412.83f, 470.25f, 527.66f, 585.08f, 812.59f
    };

    public byte[] createLedgerPdf(List<AccessLog> accessLogs, int ledgerYear, LocalDate printDate) {
        try (
                PDDocument templateDocument = loadTemplateDocument();
                PDDocument outputDocument = new PDDocument();
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {
            PDFont font = loadKoreanFont(outputDocument);
            int totalPages = Math.max(1, (int) Math.ceil(accessLogs.size() / (double) ROWS_PER_PAGE));

            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                PDPage page = outputDocument.importPage(templateDocument.getPage(0));
                try (PDPageContentStream content = new PDPageContentStream(outputDocument, page, AppendMode.APPEND, true, true)) {
                    drawLedgerHeader(content, font, ledgerYear, printDate);
                    int start = pageIndex * ROWS_PER_PAGE;
                    int end = Math.min(start + ROWS_PER_PAGE, accessLogs.size());
                    for (int i = start; i < end; i++) {
                        drawLogRow(outputDocument, content, font, accessLogs.get(i), i - start);
                    }
                }
            }

            outputDocument.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("PDF 출력 파일 생성 중 오류가 발생했습니다.", e);
        }
    }

    private void drawLedgerHeader(PDPageContentStream content, PDFont font, int ledgerYear, LocalDate printDate) throws IOException {
        cover(content, 56, 511, 80, 20);
        drawText(content, font, 10, 62, 517, ledgerYear + "년");

        cover(content, 650, 511, 170, 20);
        if (printDate != null) {
            String printedAt = "출력일자: " + printDate.format(DATE_FORMATTER);
            float textWidth = font.getStringWidth(printedAt) / 1000 * 9;
            drawText(content, font, 9, 812 - textWidth, 517, printedAt);
        }
    }

    private void cover(PDPageContentStream content, float x, float y, float width, float height) throws IOException {
        content.setNonStrokingColor(1f, 1f, 1f);
        content.addRect(x, y, width, height);
        content.fill();
        content.setNonStrokingColor(0f, 0f, 0f);
    }

    private PDDocument loadTemplateDocument() throws IOException {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PDF_PATH);
        if (!resource.exists()) {
            throw new IOException("PDF 양식 파일을 찾을 수 없습니다: " + TEMPLATE_PDF_PATH);
        }
        return Loader.loadPDF(resource.getInputStream().readAllBytes());
    }

    private void drawLogRow(PDDocument document, PDPageContentStream content, PDFont font, AccessLog log, int rowIndex)
            throws IOException {
        float top = ROW_TOP + (ROW_HEIGHT * rowIndex);
        float baseline = PAGE_HEIGHT - top - 14;
        Guide guide = Guide.from(log.getExitGuideName());
        String[] values = {
                log.getVisitedAt().format(DATE_FORMATTER),
                value(log.getCompanyName()),
                value(log.getVisitorName()),
                log.getBirthDate() == null ? "" : log.getBirthDate().format(DATE_FORMATTER),
                log.getVisitedAt().format(TIME_FORMATTER),
                log.getExitedAt() == null ? "" : log.getExitedAt().format(TIME_FORMATTER),
                guide.rank(),
                guide.name(),
                value(log.getContent())
        };

        drawCentered(content, font, 7, COLUMN_X[0], baseline, COLUMN_X[1] - COLUMN_X[0], values[0]);
        drawWrapped(content, font, 7, COLUMN_X[1] + 3, baseline, COLUMN_X[2] - COLUMN_X[1] - 6, values[1], 1);
        drawCentered(content, font, 7, COLUMN_X[2], baseline, COLUMN_X[3] - COLUMN_X[2], values[2]);
        drawCentered(content, font, 6.5f, COLUMN_X[3], baseline, COLUMN_X[4] - COLUMN_X[3], values[3]);
        drawSignature(document, content, log, COLUMN_X[4] + 4, PAGE_HEIGHT - top - ROW_HEIGHT + 3, COLUMN_X[5] - COLUMN_X[4] - 8, ROW_HEIGHT - 6);
        drawCentered(content, font, 7, COLUMN_X[5], baseline, COLUMN_X[6] - COLUMN_X[5], values[4]);
        drawCentered(content, font, 7, COLUMN_X[6], baseline, COLUMN_X[7] - COLUMN_X[6], values[5]);
        drawCentered(content, font, 7, COLUMN_X[7], baseline, COLUMN_X[8] - COLUMN_X[7], values[6]);
        drawCentered(content, font, 7, COLUMN_X[8], baseline, COLUMN_X[9] - COLUMN_X[8], values[7]);
        drawWrapped(content, font, 7, COLUMN_X[9] + 4, baseline, COLUMN_X[10] - COLUMN_X[9] - 8, values[8], 1);
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
        throw new IOException("PDF 생성을 위한 한글 폰트를 찾을 수 없습니다.");
    }

    private void drawSignature(PDDocument document, PDPageContentStream content, AccessLog log, float x, float y, float maxWidth, float maxHeight)
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

    private void drawCentered(PDPageContentStream content, PDFont font, float size, float x, float y, float width, String text)
            throws IOException {
        String fitted = trimToWidth(value(text), font, size, width - 4);
        float textWidth = font.getStringWidth(fitted) / 1000 * size;
        drawText(content, font, size, x + Math.max(2, (width - textWidth) / 2), y, fitted);
    }

    private void drawWrapped(PDPageContentStream content, PDFont font, float size, float x, float y, float width, String text, int maxLines)
            throws IOException {
        List<String> lines = wrapText(value(text), font, size, width);
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            String line = lines.get(i);
            if (i == maxLines - 1 && lines.size() > maxLines) {
                line = trimToWidth(line + "...", font, size, width);
            }
            drawText(content, font, size, x, y - (i * 9), line);
        }
    }

    private void drawText(PDPageContentStream content, PDFont font, float size, float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
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
        String result = text;
        while (!result.isEmpty() && font.getStringWidth(result) / 1000 * size > width) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String value(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\p{Cntrl}+", " ").trim();
    }

    private record Guide(String rank, String name) {
        private static Guide from(String value) {
            if (value == null || value.isBlank()) {
                return new Guide("", "");
            }
            String[] parts = value.trim().split("\\s+");
            if (parts.length < 2) {
                return new Guide("", value.trim());
            }
            return new Guide(parts[parts.length - 1], String.join(" ", List.of(parts).subList(0, parts.length - 1)));
        }
    }
}
