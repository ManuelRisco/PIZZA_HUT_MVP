package com.example.dtos;

public record OrderEmailDispatchResultDTO(
        boolean customerEmailSent,
        String customerEmail,
        boolean adminEmailSent,
        int adminRecipientsCount
) {
    public static OrderEmailDispatchResultDTO notSent(String customerEmail) {
        return new OrderEmailDispatchResultDTO(false, customerEmail, false, 0);
    }
}
