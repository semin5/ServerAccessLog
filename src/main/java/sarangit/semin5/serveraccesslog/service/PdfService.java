package sarangit.semin5.serveraccesslog.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import sarangit.semin5.serveraccesslog.domain.AccessLog;
import sarangit.semin5.serveraccesslog.web.AccessLogForm;

@Service
public class PdfService {

    private static final String TEMPLATE_PDF_PATH = "pdf/individual-template-source.pdf";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public byte[] createAccessLogPdf(AccessLogForm form, byte[] signatureImage) {
        return createPdf(
                form.getCompanyName(),
                form.getVisitorName(),
                form.getBirthDate(),
                form.getVisitedAt(),
                null,
                form.getContent(),
                signatureImage
        );
    }

    public byte[] createAccessLogPdf(AccessLog accessLog) {
        return createPdf(
                accessLog.getCompanyName(),
                accessLog.getVisitorName(),
                accessLog.getBirthDate(),
                accessLog.getVisitedAt(),
                accessLog.getExitedAt(),
                accessLog.getContent(),
                accessLog.getSignatureImage()
        );
    }

    private byte[] createPdf(
            String companyName,
            String visitorName,
            LocalDate birthDate,
            LocalDateTime visitedAt,
            LocalDateTime exitedAt,
            String contentText,
            byte[] signatureImage
    ) {
        try (PDDocument document = loadTemplate(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDFont font = loadKoreanFont(document);
            PDImageXObject signature = signatureImage == null || signatureImage.length == 0
                    ? null
                    : PDImageXObject.createFromByteArray(document, signatureImage, "signature");
            PDPage page = document.getPage(0);

            try (PDPageContentStream content = new PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
            )) {
                drawValues(content, font, companyName, visitorName, birthDate, visitedAt, exitedAt, contentText);
                drawSignature(content, signature);
            }

            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("확인서 PDF 생성 중 오류가 발생했습니다.", e);
        }
    }

    private PDDocument loadTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PDF_PATH);
        if (!resource.exists()) {
            throw new IOException("확인서 PDF 양식 파일을 찾을 수 없습니다: " + TEMPLATE_PDF_PATH);
        }
        return Loader.loadPDF(resource.getInputStream().readAllBytes());
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

    private void drawValues(
            PDPageContentStream content,
            PDFont font,
            String companyName,
            String visitorName,
            LocalDate birthDate,
            LocalDateTime visitedAt,
            LocalDateTime exitedAt,
            String contentText
    ) throws IOException {
        drawCentered(content, font, 10, 127, 696, 436, value(companyName));
        drawCentered(content, font, 10, 127, 670, 170, value(visitorName));
        drawCentered(content, font, 10, 394, 670, 170, formatDate(birthDate));

        drawCentered(content, font, 10, 127, 613, 436, formatDateTimeDate(visitedAt));
        drawCentered(content, font, 10, 127, 586, 170, formatTime(visitedAt));
        drawCentered(content, font, 10, 394, 586, 170, formatTime(exitedAt));

        drawWrapped(content, font, 11, 42, 532, 500, value(contentText), 5, 17);

        LocalDate pledgeDate = visitedAt == null ? LocalDate.now() : visitedAt.toLocalDate();
        drawText(content, font, 10, 430, 216, String.valueOf(pledgeDate.getYear()));
        drawText(content, font, 10, 488, 216, String.format("%02d", pledgeDate.getMonthValue()));
        drawText(content, font, 10, 536, 216, String.format("%02d", pledgeDate.getDayOfMonth()));
        drawText(content, font, 10, 462, 178, value(visitorName));
    }

    private void drawSignature(PDPageContentStream content, PDImageXObject signature) throws IOException {
        if (signature == null) {
            return;
        }

        float maxWidth = 76;
        float maxHeight = 42;
        float scale = Math.min(maxWidth / signature.getWidth(), maxHeight / signature.getHeight());
        float width = signature.getWidth() * scale;
        float height = signature.getHeight() * scale;
        float centerX = 557;
        float centerY = 184;
        content.drawImage(signature, centerX - (width / 2), centerY - (height / 2), width, height);
    }

    private void drawText(PDPageContentStream content, PDFont font, int size, float x, float y, String text)
            throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(value(text));
        content.endText();
    }

    private void drawCentered(
            PDPageContentStream content,
            PDFont font,
            int size,
            float x,
            float y,
            float width,
            String text
    ) throws IOException {
        String normalized = value(text);
        float textWidth = font.getStringWidth(normalized) / 1000 * size;
        drawText(content, font, size, x + ((width - textWidth) / 2), y, normalized);
    }

    private void drawWrapped(
            PDPageContentStream content,
            PDFont font,
            int size,
            float x,
            float y,
            float width,
            String text,
            int maxLines,
            float lineHeight
    ) throws IOException {
        List<String> lines = wrapText(text, font, size, width);
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            drawText(content, font, size, x, y - (i * lineHeight), lines.get(i));
        }
    }

    private List<String> wrapText(String text, PDFont font, int fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        String normalized = value(text);
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
                    line = new StringBuilder(trimToWidth(word, font, fontSize, maxWidth));
                }
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
            }
        }
        return lines.isEmpty() ? List.of("") : lines;
    }

    private String trimToWidth(String text, PDFont font, int size, float width) throws IOException {
        String result = value(text);
        while (!result.isEmpty() && font.getStringWidth(result) / 1000 * size > width) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String formatDate(LocalDate value) {
        return value == null ? "" : value.format(DATE_FORMATTER);
    }

    private String formatDateTimeDate(LocalDateTime value) {
        return value == null ? "" : value.toLocalDate().format(DATE_FORMATTER);
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? "" : value.toLocalTime().format(TIME_FORMATTER);
    }

    private String value(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("\\p{Cntrl}+", " ").trim();
    }
}
