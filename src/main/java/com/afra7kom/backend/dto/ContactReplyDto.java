package com.afra7kom.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactReplyDto {

    @NotBlank(message = "Reply message is required")
    @Size(max = 2000, message = "Reply message cannot exceed 2000 characters")
    private String replyMessage;

    private Boolean sendEmail = true;
}





