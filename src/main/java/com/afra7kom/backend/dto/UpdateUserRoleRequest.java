package com.afra7kom.backend.dto;

import com.afra7kom.backend.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRoleRequest {

    @NotNull(message = "Role names cannot be null")
    private Set<Role.RoleName> roleNames;
}



