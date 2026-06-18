package sarangit.semin5.serveraccesslog.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import sarangit.semin5.serveraccesslog.domain.AccessLog;

@Service
public class AccessLogExcelService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] createWorkbook(List<AccessLog> accessLogs) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("서버 출입 대장");
            CreationHelper helper = workbook.getCreationHelper();
            Drawing<?> drawing = sheet.createDrawingPatriarch();

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            String[] columns = {"번호", "소속", "성명", "생년월일", "입실 시간", "퇴실 시간", "담당자", "출입사유", "등록 시간", "서명"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
                header.getCell(i).setCellStyle(headerStyle);
            }

            for (int i = 0; i < accessLogs.size(); i++) {
                AccessLog log = accessLogs.get(i);
                Row row = sheet.createRow(i + 1);
                row.setHeightInPoints(72);
                row.createCell(0).setCellValue(log.getId());
                row.createCell(1).setCellValue(log.getCompanyName());
                row.createCell(2).setCellValue(log.getVisitorName());
                row.createCell(3).setCellValue(log.getBirthDate() == null ? "" : log.getBirthDate().format(DATE_FORMATTER));
                row.createCell(4).setCellValue(log.getVisitedAt().format(DATE_TIME_FORMATTER));
                row.createCell(5).setCellValue(log.getExitedAt() == null ? "" : log.getExitedAt().format(DATE_TIME_FORMATTER));
                row.createCell(6).setCellValue(nullToBlank(log.getExitGuideName()));
                row.createCell(7).setCellValue(log.getContent());
                row.createCell(8).setCellValue(log.getCreatedAt().format(DATE_TIME_FORMATTER));
                row.createCell(9).setCellValue("");
                addSignatureImage(workbook, helper, drawing, log, i + 1);
            }

            for (int i = 0; i < columns.length - 1; i++) {
                sheet.autoSizeColumn(i);
            }
            sheet.setColumnWidth(9, 28 * 256);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("엑셀 파일 생성 중 오류가 발생했습니다.", e);
        }
    }

    private void addSignatureImage(
            Workbook workbook,
            CreationHelper helper,
            Drawing<?> drawing,
            AccessLog log,
            int rowIndex
    ) {
        byte[] signatureImage = log.getSignatureImage();
        if (signatureImage == null || signatureImage.length == 0) {
            return;
        }

        int pictureType = pictureType(log.getSignatureContentType());
        int pictureIndex = workbook.addPicture(signatureImage, pictureType);
        ClientAnchor anchor = helper.createClientAnchor();
        anchor.setCol1(9);
        anchor.setRow1(rowIndex);
        anchor.setCol2(10);
        anchor.setRow2(rowIndex + 1);
        anchor.setDx1(8 * 9525);
        anchor.setDy1(6 * 9525);
        anchor.setDx2(-8 * 9525);
        anchor.setDy2(-6 * 9525);
        drawing.createPicture(anchor, pictureIndex);
    }

    private int pictureType(String contentType) {
        if ("image/jpeg".equalsIgnoreCase(contentType) || "image/jpg".equalsIgnoreCase(contentType)) {
            return Workbook.PICTURE_TYPE_JPEG;
        }
        return Workbook.PICTURE_TYPE_PNG;
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
