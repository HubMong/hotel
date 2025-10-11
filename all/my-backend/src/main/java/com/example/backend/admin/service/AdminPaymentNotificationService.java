package com.example.backend.admin.service;

import com.example.backend.admin.dto.CustomerNotificationRequest;
import com.example.backend.admin.dto.CustomerNotificationResponse;
import com.example.backend.hotel_reservation.domain.Reservation;
import com.example.backend.payment.domain.Payment;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPaymentNotificationService {

    private final JavaMailSender mailSender;

    public CustomerNotificationResponse sendCustomerNotification(Payment payment,
                                                                  String recipient,
                                                                  CustomerNotificationRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipient);
            helper.setSubject(request.getSubject());

            String htmlContent = buildEmailHtml(payment, request);
            String plainContent = buildPlainText(payment, request);
            helper.setText(plainContent, htmlContent);
            helper.setSentDate(java.sql.Timestamp.valueOf(LocalDateTime.now()));
            helper.setFrom("noreply@hotel-admin.com");
            mailSender.send(message);
            return CustomerNotificationResponse.builder()
                    .recipient(recipient)
                    .subject(request.getSubject())
                    .sentAt(LocalDateTime.now())
                    .success(true)
                    .messageId(message.getMessageID())
                    .build();
        } catch (Exception ex) {
            log.error("고객 알림 이메일 발송 실패 - paymentId={}", payment.getId(), ex);
            return CustomerNotificationResponse.builder()
                    .recipient(recipient)
                    .subject(request.getSubject())
                    .sentAt(LocalDateTime.now())
                    .success(false)
                    .messageId(null)
                    .build();
        }
    }

    public void sendPartialRefundNotification(Payment payment,
                                               int refundAmount,
                                               int fee,
                                               int remainingAmount) {
        if (payment.getUser() == null || payment.getUser().getEmail() == null) {
            return;
        }
        String subject = "[EGODA] 부분 환불이 처리되었습니다.";
        String body = String.join("\n",
                "안녕하세요, " + payment.getUser().getName() + " 고객님.",
                "요청하신 결제에 대해 부분 환불이 완료되었습니다.",
                "환불 금액: " + formatCurrency(refundAmount),
                "공제 수수료: " + formatCurrency(fee),
                "잔여 환불 가능액: " + formatCurrency(remainingAmount),
                "궁금하신 사항은 고객센터로 문의해주세요.");
        CustomerNotificationRequest request = CustomerNotificationRequest.builder()
                .subject(subject)
                .body(body)
                .build();
        sendCustomerNotification(payment, payment.getUser().getEmail(), request);
    }

    public void sendPaymentCompletedNotification(Payment payment, Reservation reservation) {
        if (payment == null) {
            return;
        }

        if (payment.getUser() == null || payment.getUser().getEmail() == null) {
            log.warn("결제 완료 알림을 전송하지 못했습니다. 사용자 이메일이 없습니다. paymentId={}", payment.getId());
            return;
        }

        String subject = "[EGODA] 결제가 완료되었습니다.";
        String body = buildPaymentCompletedBody(payment, reservation);

        CustomerNotificationRequest request = CustomerNotificationRequest.builder()
                .subject(subject)
                .body(body)
                .includeReceipt(true)
                .build();

        CustomerNotificationResponse response = sendCustomerNotification(payment, payment.getUser().getEmail(), request);
        if (!response.isSuccess()) {
            log.warn("결제 완료 안내 이메일 전송 실패 - paymentId={} recipient={}", payment.getId(), payment.getUser().getEmail());
        } else {
            log.info("결제 완료 안내 이메일 전송 - paymentId={} recipient={}", payment.getId(), payment.getUser().getEmail());
        }
    }

    private String formatCurrency(int amount) {
        return String.format("%,d원", amount);
    }

    private String buildPaymentCompletedBody(Payment payment, Reservation reservation) {
        StringBuilder builder = new StringBuilder();
        builder.append("EGODA 결제가 정상적으로 완료되었습니다.\n\n");
        builder.append("- 주문명: ")
                .append(Objects.requireNonNullElse(payment.getOrderName(), "호텔 예약 결제"))
                .append("\n");
        builder.append("- 결제 금액: ")
                .append(formatCurrencyIntl(payment.getTotalPrice()))
                .append("\n");
        builder.append("- 결제 일시: ")
                .append(formatDateTime(payment.getCreatedAt()))
                .append("\n");

        if (reservation != null) {
            String reservationPeriod = formatReservationPeriod(reservation);
            if (!reservationPeriod.isBlank()) {
                builder.append("- 투숙 일정: ").append(reservationPeriod).append("\n");
            }

            String guestSummary = formatGuestSummary(reservation);
            if (!guestSummary.isBlank()) {
                builder.append("- 투숙 인원: ").append(guestSummary).append("\n");
            }
        }

        builder.append("\n즐거운 여행 되시길 바랍니다.\nEGODA 드림");
        return builder.toString();
    }

    private String formatReservationPeriod(Reservation reservation) {
        if (reservation == null || reservation.getStartDate() == null || reservation.getEndDate() == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd (E)", Locale.KOREAN);
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        String start = reservation.getStartDate().atZone(zoneId).format(formatter);
        String end = reservation.getEndDate().atZone(zoneId).format(formatter);
        return start + " ~ " + end;
    }

    private String formatGuestSummary(Reservation reservation) {
        if (reservation == null) {
            return "";
        }
        int adult = reservation.getNumAdult() != null ? reservation.getNumAdult() : 0;
        int kid = reservation.getNumKid() != null ? reservation.getNumKid() : 0;
        int total = adult + kid;
        if (total <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(total).append("명");
        java.util.List<String> segments = new java.util.ArrayList<>();
        if (adult > 0) {
            segments.add("성인 " + adult + "명");
        }
        if (kid > 0) {
            segments.add("아동 " + kid + "명");
        }
        if (!segments.isEmpty()) {
            builder.append(" (")
                    .append(String.join(", ", segments))
                    .append(")");
        }
        if (reservation.getNumRooms() != null && reservation.getNumRooms() > 0) {
            builder.append(", 객실 ")
                    .append(reservation.getNumRooms())
                    .append("개");
        }
        return builder.toString();
    }

    private String buildEmailHtml(Payment payment, CustomerNotificationRequest request) {
        String brandColor = "#3D5AFE";
        String accentColor = "#00BFA5";
        String neutralColor = "#F4F6FB";
        String textColor = "#263238";

    String messageHtml = renderMessageHtml(request.getBody(), textColor);

        String receiptSection = "";
        if (request.isIncludeReceipt() && payment.getReceiptUrl() != null && !payment.getReceiptUrl().isBlank()) {
            receiptSection = "<tr>"
                    + "<td style=\"padding:16px 24px 24px;\">"
                    + "<div style=\"border-top:1px solid #E0E7FF; margin-top:12px; padding-top:16px;\">"
                    + "<p style=\"margin:0 0 8px; font-weight:600; color:" + textColor + ";\">영수증 확인</p>"
                    + "<a href=\"" + escapeHtml(payment.getReceiptUrl()) + "\" style=\"display:inline-block; padding:12px 20px; background-color:" + brandColor + "; color:#ffffff; border-radius:8px; text-decoration:none; font-weight:600;\">영수증 보기</a>"
                    + "</div>"
                    + "</td>"
                    + "</tr>";
        }

        String summaryRows = buildSummaryRows(payment, textColor, accentColor);

        return "<!DOCTYPE html>"
                + "<html lang=\"ko\">"
                + "<head>"
                + "<meta charset=\"UTF-8\" />"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />"
                + "<title>" + escapeHtml(request.getSubject()) + "</title>"
                + "</head>"
                + "<body style=\"margin:0; padding:24px 0; background-color:" + neutralColor + "; font-family:'Noto Sans KR','Apple SD Gothic Neo','Malgun Gothic',sans-serif;\">"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"max-width:640px; margin:0 auto; background-color:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 12px 32px rgba(61,90,254,0.16);\">"
                + "<tr>"
                + "<td style=\"padding:32px 32px 24px; background:linear-gradient(135deg," + brandColor + ",#536DFE); color:#ffffff;\">"
                + "<div style=\"font-size:14px; letter-spacing:4px; text-transform:uppercase; opacity:0.85;\">EGODA PAYMENTS</div>"
                + "<div style=\"margin-top:8px; font-size:24px; font-weight:700;\">" + escapeHtml(request.getSubject()) + "</div>"
                + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td style=\"padding:24px 32px;\">"
                + "<div style=\"background-color:#EEF2FF; border-radius:12px; padding:16px 20px; margin-bottom:24px;\">"
                + "<p style=\"margin:0; font-size:14px; color:#1A237E;\">안녕하세요, " + escapeHtml(resolveCustomerName(payment)) + " 고객님.</p>"
                + "</div>"
                + messageHtml
                + "</td>"
                + "</tr>"
                + summaryRows
                + receiptSection
                + "<tr>"
                + "<td style=\"padding:24px 32px; background-color:#F5F7FB;\">"
                + "<p style=\"margin:0 0 8px; color:#607D8B; font-size:13px;\">궁금하신 사항은 언제든지 EGODA 고객센터로 문의해 주세요.</p>"
                + "<p style=\"margin:0; color:#90A4AE; font-size:12px;\">ⓒ " + LocalDateTime.now().getYear() + " EGODA. All rights reserved.</p>"
                + "</td>"
                + "</tr>"
                + "</table>"
                + "</body>"
                + "</html>";
    }

    private String renderMessageHtml(String body, String textColor) {
        String normalized = normalizeLineBreaks(body);
        if (normalized.isBlank()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        String[] lines = normalized.split("\n");
        java.util.List<String> bulletBuffer = new java.util.ArrayList<>();

        java.util.function.Consumer<java.util.List<String>> flushBullets = bullets -> {
            if (bullets.isEmpty()) {
                return;
            }
            builder.append("<ul style=\"margin:0 0 16px 0; padding-left:20px; color:").append(textColor).append(";\">");
            for (String bullet : bullets) {
                builder.append("<li style=\"margin:4px 0; line-height:1.6;\">")
                        .append(escapeHtml(bullet.trim()))
                        .append("</li>");
            }
            builder.append("</ul>");
            bullets.clear();
        };

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                flushBullets.accept(bulletBuffer);
                continue;
            }

            if (line.startsWith("- ") || line.startsWith("• ") || line.startsWith("ㆍ ")) {
                String content = line.length() > 2 ? line.substring(2) : line;
                bulletBuffer.add(content);
            } else {
                flushBullets.accept(bulletBuffer);
                builder.append("<p style=\"margin:0 0 12px; line-height:1.6; color:")
                        .append(textColor)
                        .append(";\">")
                        .append(escapeHtml(line))
                        .append("</p>");
            }
        }

        flushBullets.accept(bulletBuffer);
        return builder.toString();
    }

    private String buildSummaryRows(Payment payment, String textColor, String accentColor) {
        String formattedAmount = formatCurrencyIntl(payment.getTotalPrice());
        String paymentDate = formatDateTime(payment.getCreatedAt());
        String paymentMethod = Objects.requireNonNullElse(payment.getDisplayMethod(), payment.getPaymentMethod());
        String orderName = Objects.requireNonNullElse(payment.getOrderName(), "호텔 예약 결제");

        StringBuilder builder = new StringBuilder();
        builder.append("<tr>");
        builder.append("<td style=\"padding:0 32px 32px;\">");
        builder.append("<div style=\"border:1px solid #E0E7FF; border-radius:12px; overflow:hidden;\">");
        builder.append("<div style=\"background-color:#EEF2FF; padding:16px 24px; font-weight:600; color:" + textColor + ";\">결제 정보</div>");
        builder.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;\">");

        appendSummaryRow(builder, "호텔 / 상품명", orderName, textColor);
        appendSummaryRow(builder, "결제 금액", formattedAmount, textColor);
        appendSummaryRow(builder, "결제 일시", paymentDate, textColor);
        appendSummaryRow(builder, "결제 수단", paymentMethod, textColor);

        if (payment.getStatus() != null) {
            appendSummaryRow(builder, "결제 상태", translateStatus(payment.getStatus()), accentColor);
        }

        builder.append("</table>");
        builder.append("</div>");
        builder.append("</td>");
        builder.append("</tr>");
        return builder.toString();
    }

    private void appendSummaryRow(StringBuilder builder, String label, String value, String textColor) {
        builder.append("<tr>");
        builder.append("<td style=\"padding:14px 24px; border-bottom:1px solid #E8EAED; font-size:13px; color:#607D8B; width:36%;\">");
        builder.append(escapeHtml(label));
        builder.append("</td>");
        builder.append("<td style=\"padding:14px 24px; border-bottom:1px solid #E8EAED; font-size:15px; font-weight:600; color:" + textColor + ";\">");
        builder.append(escapeHtml(value));
        builder.append("</td>");
        builder.append("</tr>");
    }

    private String buildPlainText(Payment payment, CustomerNotificationRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("안녕하세요, ")
                .append(resolveCustomerName(payment))
                .append(" 고객님.\n\n");
        builder.append(normalizeLineBreaks(request.getBody()).trim()).append("\n\n");
        builder.append("[결제 정보]\n");
        builder.append("- 호텔 / 상품명: ").append(Objects.requireNonNullElse(payment.getOrderName(), "호텔 예약 결제")).append("\n");
    builder.append("- 결제 금액: ").append(formatCurrencyIntl(payment.getTotalPrice())).append("\n");
        builder.append("- 결제 일시: ").append(formatDateTime(payment.getCreatedAt())).append("\n");
        builder.append("- 결제 수단: ").append(Objects.requireNonNullElse(payment.getDisplayMethod(), payment.getPaymentMethod())).append("\n");
        if (payment.getStatus() != null) {
            builder.append("- 결제 상태: ").append(translateStatus(payment.getStatus())).append("\n");
        }
        if (request.isIncludeReceipt() && payment.getReceiptUrl() != null && !payment.getReceiptUrl().isBlank()) {
            builder.append("- 영수증 링크: ").append(payment.getReceiptUrl()).append("\n");
        }
        builder.append("\n궁금하신 점이 있으시면 EGODA 고객센터로 문의해 주세요.");
        return builder.toString();
    }

    private String formatCurrencyIntl(Integer amount) {
        if (amount == null) {
            return "-";
        }
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("ko", "KR"));
        format.setMaximumFractionDigits(0);
        return format.format(amount);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd a hh:mm", Locale.KOREAN);
        return dateTime.format(formatter);
    }

    private String translateStatus(Payment.Status status) {
        return switch (status) {
            case COMPLETED -> "결제 완료";
            case PENDING -> "결제 대기";
            case CANCELLED -> "결제 취소";
            case FAILED -> "결제 실패";
        };
    }

    private String resolveCustomerName(Payment payment) {
        if (payment.getUser() != null && payment.getUser().getName() != null) {
            return payment.getUser().getName();
        }
        return "고객";
    }

    private String normalizeLineBreaks(String text) {
        return text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n');
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&' -> builder.append("&amp;");
                case '<' -> builder.append("&lt;");
                case '>' -> builder.append("&gt;");
                case '"' -> builder.append("&quot;");
                case '\'' -> builder.append("&#39;");
                default -> builder.append(c);
            }
    }
    return builder.toString();
    }
}
