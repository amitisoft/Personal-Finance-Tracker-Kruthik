package com.financetracker.dto;

import com.financetracker.entity.AccountMembershipRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class AccountMembershipDtos {

    public record InviteRequest(
            @NotBlank @Email String email,
            @NotNull AccountMembershipRole role
    ) {
    }

    public record MemberUpdateRequest(
            @NotNull AccountMembershipRole role
    ) {
    }

    public record MemberResponse(
            String userId,
            String email,
            String displayName,
            AccountMembershipRole role
    ) {
    }

    public record MemberListResponse(
            String accountId,
            String accountName,
            List<MemberResponse> members
    ) {
    }
}
