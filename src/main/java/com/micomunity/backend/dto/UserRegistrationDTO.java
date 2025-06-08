package com.micomunity.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import com.micomunity.backend.model.Role;

@Data
public class UserRegistrationDTO {
    
    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "^[0-9]{8}[A-Z]$", message = "El formato del DNI no es válido")
    private String dni;

    @NotBlank(message = "El nombre completo es obligatorio")
    private String fullName;

    @NotBlank(message = "El piso es obligatorio")
    private String floor;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    @NotBlank(message = "La confirmación de contraseña es obligatoria")
    private String confirmPassword;

    @NotNull(message = "El rol es obligatorio")
    private Role role;

    @NotBlank(message = "El código de la comunidad es obligatorio")
    private String communityCode;
} 