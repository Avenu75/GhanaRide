package com.ghanaride.service;

import com.ghanaride.entity.Booking;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class PdfService {

    private static final DeviceRgb GREEN = new DeviceRgb(15, 157, 88);
    private static final DeviceRgb GOLD = new DeviceRgb(252, 209, 22);
    private static final DeviceRgb DARK = new DeviceRgb(26, 26, 46);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a");

    public byte[] generateBookingReceipt(Booking booking) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Title
            document.add(new Paragraph("GhanaRide")
                    .setFontColor(GREEN)
                    .setFontSize(28)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("Booking Confirmation Receipt")
                    .setFontColor(DARK)
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            // Table
            float[] columnWidths = {200F, 300F};
            Table table = new Table(UnitValue.createPointArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            addRow(table, "Booking Reference", booking.getBookingReference());
            addRow(table, "Passenger", booking.getUser().getFullName());
            addRow(table, "From", booking.getTrip().getFromLocation());
            addRow(table, "To", booking.getTrip().getToLocation());

            if (booking.getTrip().getDepartureTime() != null) {
                addRow(table, "Departure", FORMATTER.format(booking.getTrip().getDepartureTime()));
            }

            addRow(table, "Seat Number", String.valueOf(booking.getSeatNumber()));
            addRow(table, "Amount Paid", "GHC " + booking.getTotalAmount());

            if (booking.getBookingDate() != null) {
                addRow(table, "Booked On", FORMATTER.format(booking.getBookingDate()));
            }

            addRow(table, "Status", booking.getStatus().name());
            document.add(table);

            document.add(new Paragraph("\nThank you for choosing GhanaRide!")
                    .setFontColor(GREEN)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20));

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("PDF generation failed for booking: {}", booking.getBookingReference(), e);
            return new byte[0];
        }
    }

    private void addRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addCell(new Cell().add(new Paragraph(value == null ? "N/A" : value)));
    }
}
