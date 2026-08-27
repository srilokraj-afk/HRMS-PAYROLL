package com.payroll.application.service;

import com.payroll.application.model.Employee;
import com.payroll.application.model.PayrollRecord;
import com.payroll.application.repository.PayrollRecordRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Generates a professional, single-page A4 payslip.
 * The layout is intentionally close to a corporate payroll template:
 * branded header, employee information, earnings/deductions tables,
 * highlighted net pay and an authorization/footer area.
 */
@Service
public class PayslipPdfService {
    private static final PDType1Font REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font ITALIC = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

    private static final float PAGE_W = PDRectangle.A4.getWidth();
    private static final float PAGE_H = PDRectangle.A4.getHeight();
    private static final float LEFT = 42;
    private static final float RIGHT = PAGE_W - 42;
    private static final float BLUE_R = 0.08f;
    private static final float BLUE_G = 0.35f;
    private static final float BLUE_B = 0.68f;
    private static final float LIGHT_BLUE_R = 0.93f;
    private static final float LIGHT_BLUE_G = 0.96f;
    private static final float LIGHT_BLUE_B = 0.99f;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    private final PayrollRecordRepository repo;

    public PayslipPdfService(PayrollRecordRepository repo) {
        this.repo = repo;
    }

    public byte[] generate(Long id) {
        PayrollRecord record = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payroll record not found"));
        Employee employee = record.getEmployee();

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                drawBackground(cs);
                drawHeader(cs);
                float y = 735;
                y = drawEmployeeDetails(cs, employee, record, y);
                y -= 12;
                y = drawSectionTitle(cs, "EARNINGS", y, true);
                y = drawEarningsTable(cs, employee, record, y);
                y -= 18;
                y = drawSectionTitle(cs, "DEDUCTIONS", y, false);
                y = drawDeductionsTable(cs, employee, record, y);
                y -= 18;
                y = drawNetPayBox(cs, employee, record, y);
                drawFooter(cs, record, y - 22);
            }

            document.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate payslip PDF", e);
        }
    }

    private void drawBackground(PDPageContentStream cs) throws Exception {
        cs.setStrokingColor(BLUE_R, BLUE_G, BLUE_B);
        cs.setLineWidth(1.2f);
        cs.addRect(18, 18, PAGE_W - 36, PAGE_H - 36);
        cs.stroke();

        cs.setNonStrokingColor(LIGHT_BLUE_R, LIGHT_BLUE_G, LIGHT_BLUE_B);
        cs.addRect(18, 18, PAGE_W - 36, 30);
        cs.fill();
    }

    private void drawHeader(PDPageContentStream cs) throws Exception {
        // Simple vector company mark: building + underline.
        cs.setStrokingColor(BLUE_R, BLUE_G, BLUE_B);
        cs.setNonStrokingColor(BLUE_R, BLUE_G, BLUE_B);
        cs.setLineWidth(2.2f);
        cs.addRect(48, 755, 22, 32);
        cs.stroke();
        cs.addRect(76, 755, 18, 44);
        cs.stroke();
        for (int i = 0; i < 4; i++) {
            cs.setLineWidth(1f);
            cs.moveTo(52, 760 + i * 6);
            cs.lineTo(66, 760 + i * 6);
            cs.stroke();
            cs.moveTo(80, 761 + i * 8);
            cs.lineTo(90, 761 + i * 8);
            cs.stroke();
        }
        cs.setLineWidth(2f);
        cs.moveTo(44, 751);
        cs.lineTo(100, 751);
        cs.stroke();

        text(cs, BOLD, 23, 116, 785, "PayFlow");
        text(cs, BOLD, 18, 116, 763, "PAYROLL SLIP", BLUE_R, BLUE_G, BLUE_B);
        text(cs, REGULAR, 8.5f, RIGHT - 170, 785, "Payroll Management System", 0.25f, 0.30f, 0.38f);
        text(cs, REGULAR, 8.5f, RIGHT - 170, 772, "Professional employee payslip", 0.40f, 0.45f, 0.52f);

        cs.setStrokingColor(BLUE_R, BLUE_G, BLUE_B);
        cs.setLineWidth(1.4f);
        cs.moveTo(42, 735);
        cs.lineTo(RIGHT, 735);
        cs.stroke();
    }

    private float drawEmployeeDetails(PDPageContentStream cs, Employee e, PayrollRecord r, float y) throws Exception {
        float leftX = 50;
        float rightX = 310;
        float labelW = 82;
        float row = 17;

        detail(cs, leftX, y, "Month", formatMonth(r));
        detail(cs, leftX, y - row, "Employee Name", safe(e.getName()));
        detail(cs, leftX, y - row * 2, "Employee ID", String.valueOf(e.getId()));
        detail(cs, leftX, y - row * 3, "Department", safe(e.getDepartment()));
        detail(cs, leftX, y - row * 4, "Email", safe(e.getEmail()));

        detail(cs, rightX, y, "Pay Date", r.getPayrollDate() == null ? "-" : r.getPayrollDate().format(DATE));
        detail(cs, rightX, y - row, "Pay Type", "Monthly");
        detail(cs, rightX, y - row * 2, "Status", safe(r.getStatus().name()));
        detail(cs, rightX, y - row * 3, "Currency", "INR");
        detail(cs, rightX, y - row * 4, "Pay Slip No.", "PF-" + r.getId() + "-" + r.getPayrollMonth());

        return y - row * 5;
    }

    private void detail(PDPageContentStream cs, float x, float y, String label, String value) throws Exception {
        text(cs, BOLD, 8.3f, x, y, label, 0.10f, 0.25f, 0.48f);
        text(cs, REGULAR, 8.3f, x + 82, y, ":  " + truncate(value, 32), 0.18f, 0.22f, 0.28f);
    }

    private float drawSectionTitle(PDPageContentStream cs, String title, float y, boolean earnings) throws Exception {
        cs.setStrokingColor(BLUE_R, BLUE_G, BLUE_B);
        cs.setLineWidth(1.1f);
        cs.addRect(48, y - 2, 20, 20);
        cs.stroke();
        text(cs, BOLD, 8.5f, 54, y + 3, earnings ? "+" : "-", BLUE_R, BLUE_G, BLUE_B);
        text(cs, BOLD, 13, 78, y + 1, title, BLUE_R, BLUE_G, BLUE_B);
        return y - 22;
    }

    private float drawEarningsTable(PDPageContentStream cs, Employee e, PayrollRecord r, float top) throws Exception {
        float x = 48;
        float width = RIGHT - x;
        float descW = 325;
        float rowH = 22;
        float y = top;
        tableHeader(cs, x, y, width, descW);
        y -= rowH;

        row(cs, x, y, width, descW, rowH, "Basic Salary", money(e.getBasicSalary()), false);
        y -= rowH;
        row(cs, x, y, width, descW, rowH, "Allowances", money(e.getAllowances()), false);
        y -= rowH;
        double other = Math.max(0d, r.getGrossSalary() - e.getBasicSalary() - e.getAllowances());
        if (other > 0.005d) {
            row(cs, x, y, width, descW, rowH, "Other Earnings", money(other), false);
            y -= rowH;
        }
        row(cs, x, y, width, descW, rowH, "TOTAL EARNINGS", money(r.getGrossSalary()), true);
        return y - 2;
    }

    private float drawDeductionsTable(PDPageContentStream cs, Employee e, PayrollRecord r, float top) throws Exception {
        float x = 48;
        float width = RIGHT - x;
        float descW = 325;
        float rowH = 22;
        float y = top;
        tableHeader(cs, x, y, width, descW);
        y -= rowH;

        row(cs, x, y, width, descW, rowH, "Payroll Deductions", money(e.getDeductions()), false);
        y -= rowH;
        double derived = Math.max(0d, r.getGrossSalary() - r.getNetSalary() - e.getDeductions());
        if (derived > 0.005d) {
            row(cs, x, y, width, descW, rowH, "Other Deductions", money(derived), false);
            y -= rowH;
        }
        double totalDeductions = r.getGrossSalary() - r.getNetSalary();
        row(cs, x, y, width, descW, rowH, "TOTAL DEDUCTIONS", money(totalDeductions), true);
        return y - 2;
    }

    private void tableHeader(PDPageContentStream cs, float x, float y, float width, float descW) throws Exception {
        cs.setNonStrokingColor(BLUE_R, BLUE_G, BLUE_B);
        cs.addRect(x, y - 2, width, 22);
        cs.fill();
        text(cs, BOLD, 8.5f, x + 135, y + 5, "DESCRIPTION", 1, 1, 1);
        text(cs, BOLD, 8.5f, x + descW + 65, y + 5, "AMOUNT (INR)", 1, 1, 1);
    }

    private void row(PDPageContentStream cs, float x, float y, float width, float descW, float h,
                     String description, String amount, boolean total) throws Exception {
        cs.setNonStrokingColor(total ? LIGHT_BLUE_R : 1, total ? LIGHT_BLUE_G : 1, total ? LIGHT_BLUE_B : 1);
        cs.addRect(x, y - h + 1, width, h);
        cs.fill();

        cs.setStrokingColor(0.70f, 0.78f, 0.88f);
        cs.setLineWidth(0.55f);
        cs.addRect(x, y - h + 1, width, h);
        cs.moveTo(x + descW, y - h + 1);
        cs.lineTo(x + descW, y + 1);
        cs.stroke();

        if (total) {
            text(cs, BOLD, 8.5f, x + 105, y - 13, description, BLUE_R, BLUE_G, BLUE_B);
            text(cs, BOLD, 9.5f, x + descW + 67, y - 13, amount, BLUE_R, BLUE_G, BLUE_B);
        } else {
            text(cs, REGULAR, 8.5f, x + 10, y - 13, description, 0.18f, 0.22f, 0.28f);
            text(cs, REGULAR, 8.5f, x + descW + 70, y - 13, amount, 0.18f, 0.22f, 0.28f);
        }
    }

    private float drawNetPayBox(PDPageContentStream cs, Employee e, PayrollRecord r, float y) throws Exception {
        float x = 48;
        float width = RIGHT - x;
        float h = 70;
        cs.setNonStrokingColor(LIGHT_BLUE_R, LIGHT_BLUE_G, LIGHT_BLUE_B);
        cs.setStrokingColor(0.55f, 0.70f, 0.88f);
        cs.setLineWidth(1f);
        cs.addRect(x, y - h, width, h);
        cs.fill();
        cs.addRect(x, y - h, width, h);
        cs.stroke();

        // Wallet/currency icon.
        cs.setStrokingColor(BLUE_R, BLUE_G, BLUE_B);
        cs.setLineWidth(1.6f);
        cs.addRect(x + 16, y - 53, 32, 27);
        cs.stroke();
        text(cs, BOLD, 10, x + 23, y - 44, "INR", BLUE_R, BLUE_G, BLUE_B);

        text(cs, BOLD, 8.5f, x + 62, y - 24, "NET PAY", BLUE_R, BLUE_G, BLUE_B);
        text(cs, BOLD, 21, x + 62, y - 45, money(r.getNetSalary()), BLUE_R, BLUE_G, BLUE_B);
        text(cs, ITALIC, 7.8f, x + 62, y - 58, "Final payable amount", 0.35f, 0.40f, 0.48f);

        cs.setStrokingColor(0.68f, 0.76f, 0.86f);
        cs.moveTo(x + 280, y - 12);
        cs.lineTo(x + 280, y - h + 12);
        cs.stroke();

        detailSmall(cs, x + 300, y - 22, "Gross", money(r.getGrossSalary()));
        detailSmall(cs, x + 300, y - 38, "Deductions", money(r.getGrossSalary() - r.getNetSalary()));
        detailSmall(cs, x + 300, y - 54, "Department", truncate(safe(e.getDepartment()), 22));
        return y - h;
    }

    private void detailSmall(PDPageContentStream cs, float x, float y, String label, String value) throws Exception {
        text(cs, BOLD, 7.6f, x, y, label, BLUE_R, BLUE_G, BLUE_B);
        text(cs, REGULAR, 7.6f, x + 72, y, ":  " + value, 0.18f, 0.22f, 0.28f);
    }

    private void drawFooter(PDPageContentStream cs, PayrollRecord r, float y) throws Exception {
        float lineY = Math.max(62, y);
        cs.setStrokingColor(BLUE_R, BLUE_G, BLUE_B);
        cs.setLineWidth(1.1f);
        cs.moveTo(48, lineY);
        cs.lineTo(RIGHT, lineY);
        cs.stroke();

        text(cs, BOLD, 7.5f, 50, lineY - 17, "Authorized by:", BLUE_R, BLUE_G, BLUE_B);
        text(cs, REGULAR, 7.5f, 50, lineY - 29, "Payroll Administrator - PayFlow", 0.18f, 0.22f, 0.28f);
        text(cs, REGULAR, 7.5f, RIGHT - 170, lineY - 17, "Status: " + safe(r.getStatus().name()), BLUE_R, BLUE_G, BLUE_B);
        text(cs, REGULAR, 7.2f, RIGHT - 170, lineY - 29, "Record ID: " + r.getId(), 0.35f, 0.40f, 0.48f);
        text(cs, ITALIC, 7f, 150, 27, "This is a computer-generated payslip and does not require a signature.", 0.30f, 0.36f, 0.45f);
    }

    private void text(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String value) throws Exception {
        text(cs, font, size, x, y, value, 0.12f, 0.16f, 0.22f);
    }

    private void text(PDPageContentStream cs, PDType1Font font, float size, float x, float y,
                      String value, float r, float g, float b) throws Exception {
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(r, g, b);
        cs.newLineAtOffset(x, y);
        cs.showText(safe(value));
        cs.endText();
    }

    private String money(double amount) {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return "INR " + format.format(amount);
    }

    private String formatMonth(PayrollRecord record) {
        try {
            if (record.getPayrollMonth() != null && record.getPayrollMonth().matches("\\d{4}-\\d{2}")) {
                return java.time.YearMonth.parse(record.getPayrollMonth()).format(MONTH);
            }
        } catch (Exception ignored) {
        }
        return safe(record.getPayrollMonth());
    }

    private String truncate(String value, int max) {
        String s = safe(value);
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 3)) + "...";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value.replace('\n', ' ').replace('\r', ' ');
    }
}
