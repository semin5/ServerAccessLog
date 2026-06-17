package sarangit.semin5.serveraccesslog.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import sarangit.semin5.serveraccesslog.domain.AccessLog;

@Service
public class AccessLogExcelService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] createWorkbook(List<AccessLog> accessLogs) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("서버 출입 대장");
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            String[] columns = {"번호", "상호명", "이름", "연락처", "담당자", "서버명", "출입 시간", "내용", "등록 시간", "PDF"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
                header.getCell(i).setCellStyle(headerStyle);
            }

            for (int i = 0; i < accessLogs.size(); i++) {
                AccessLog log = accessLogs.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(log.getId());
                row.createCell(1).setCellValue(log.getCompanyName());
                row.createCell(2).setCellValue(log.getVisitorName());
                row.createCell(3).setCellValue(nullToBlank(log.getContact()));
                row.createCell(4).setCellValue(nullToBlank(log.getHostName()));
                row.createCell(5).setCellValue(nullToBlank(log.getServerName()));
                row.createCell(6).setCellValue(log.getVisitedAt().format(DATE_TIME_FORMATTER));
                row.createCell(7).setCellValue(log.getContent());
                row.createCell(8).setCellValue(log.getCreatedAt().format(DATE_TIME_FORMATTER));
                row.createCell(9).setCellValue("/admin/" + log.getId() + "/pdf");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("엑셀 파일 생성 중 오류가 발생했습니다.", e);
        }
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
