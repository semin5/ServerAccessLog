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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import sarangit.semin5.serveraccesslog.domain.CarryLog;

@Service
public class CarryLogLedgerPdfService {

    private static final String TEMPLATE_PDF_PATH = "pdf/carry-ledger-template.pdf";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final int ROWS_PER_PAGE = 3;
    private static final float[] BLOCK_TOP_Y = {703, 484, 260};

    public byte[] createLedgerPdf(List<CarryLog> carryLogs, int ledgerYear, LocalDate printDate) {
        try (
                PDDocument templateDocument = loadTemplateDocument();
                PDDocument outputDocument = new PDDocument();
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {
            PDFont font = loadKoreanFont(outputDocument);
            int totalPages = Math.max(1, (int) Math.ceil(carryLogs.size() / (double) ROWS_PER_PAGE));

            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                PDPage page = outputDocument.importPage(templateDocument.getPage(0));
                try (PDPageContentStream content = new PDPageContentStream(outputDocument, page, AppendMode.APPEND, true, true)) {
                    drawPageHeader(content, font, ledgerYear, printDate);
                    int start = pageIndex * ROWS_PER_PAGE;
                    int end = Math.min(start + ROWS_PER_PAGE, carryLogs.size());
                    for (int i = start; i < end; i++) {
                        drawLogBlock(content, font, carryLogs.get(i), i - start);
                    }
                }
            }

            outputDocument.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("반출입 관리대장 PDF 생성 중 오류가 발생했습니다.", e);
        }
    }

    private PDDocument loadTemplateDocument() throws IOException {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PDF_PATH);
        if (!resource.exists()) {
            throw new IOException("PDF 양식 파일을 찾을 수 없습니다: " + TEMPLATE_PDF_PATH);
        }
        return Loader.loadPDF(resource.getInputStream().readAllBytes());
    }

    private void drawPageHeader(PDPageContentStream content, PDFont font, int ledgerYear, LocalDate printDate) throws IOException {
        drawText(content, font, 9, 31, 772, ledgerYear + "년");
        if (printDate != null) {
            String printedAt = "출력일자: " + printDate.format(DATE_FORMATTER);
            float textWidth = font.getStringWidth(printedAt) / 1000 * 9;
            drawText(content, font, 9, 564 - textWidth, 772, printedAt);
        }
    }

    private void drawLogBlock(PDPageContentStream content, PDFont font, CarryLog log, int index) throws IOException {
        float y = BLOCK_TOP_Y[index];
        drawCentered(content, font, 9, 206, y + 37, 119, log.getVisitorName());
        drawCentered(content, font, 9, 452, y + 39, 34, String.valueOf(log.getWorkDate().getYear()));
        drawCentered(content, font, 9, 504, y + 39, 18, String.format("%02d", log.getWorkDate().getMonthValue()));
        drawCentered(content, font, 9, 531, y + 39, 18, String.format("%02d", log.getWorkDate().getDayOfMonth()));

        drawCentered(content, font, 9, 206, y + 13, 119, log.getCompanyName() + " / " + log.getJobTitle());
        drawCentered(content, font, 9, 449, y + 15, 47, log.getVisitedAt().format(TIME_FORMATTER));
        drawCentered(content, font, 9, 510, y + 15, 47, log.getExitedAt() == null ? "" : log.getExitedAt().format(TIME_FORMATTER));
        drawCentered(content, font, 9, 206, y - 11, 119, log.getContact());
        drawCentered(content, font, 9, 444, y - 11, 119, guideName(log.getExitGuideName()));

        drawCheck(content, log.isEquipmentInbound(), 109, y - 35);
        drawCheck(content, log.isEquipmentOutbound(), 153, y - 35);
        drawText(content, font, 9, 255, y - 34, value(log.getEquipmentModelName()));
        drawCheck(content, log.isStorageInbound(), 109, y - 59);
        drawCheck(content, log.isStorageOutbound(), 153, y - 59);
        drawText(content, font, 9, 255, y - 58, value(log.getStorageModelName()));
        drawWrapped(content, font, 9, 92, y - 114, 455, log.getWorkContent(), 3);
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
        content.setLineWidth(1.1f);
        content.moveTo(x + 2, y + 4);
        content.lineTo(x + 5, y + 1);
        content.lineTo(x + 11, y + 10);
        content.stroke();
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

    private void drawWrapped(PDPageContentStream content, PDFont font, float size, float x, float y, float width, String text, int maxLines)
            throws IOException {
        List<String> lines = wrapText(value(text), font, size, width);
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            drawText(content, font, size, x, y - (i * 14), lines.get(i));
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
