package com.micomunity.backend.controller;

import com.micomunity.backend.dto.CrearVotacionDTO;
import com.micomunity.backend.dto.VotacionResponseDTO;
import com.micomunity.backend.dto.VotoRequestDTO;
import com.micomunity.backend.service.VotacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/votaciones")
@RequiredArgsConstructor
public class VotacionController {

    private final VotacionService votacionService;

    @PostMapping
    public ResponseEntity<?> crearVotacion(@Valid @RequestBody CrearVotacionDTO dto, Principal principal) {
        votacionService.crearVotacion(dto, principal);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/votar")
    public ResponseEntity<?> votar(@Valid @RequestBody VotoRequestDTO dto, Principal principal) {
        votacionService.votar(dto, principal);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/activas")
    public ResponseEntity<List<VotacionResponseDTO>> obtenerVotacionesActivas(Principal principal) {
        return ResponseEntity.ok(votacionService.obtenerVotacionesActivas(principal));
    }

    @GetMapping("/cerradas")
    public ResponseEntity<List<VotacionResponseDTO>> obtenerVotacionesCerradas(Principal principal) {
        return ResponseEntity.ok(votacionService.obtenerVotacionesCerradas(principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarVotacion(@PathVariable Long id, Principal principal) {
        votacionService.eliminarVotacion(id, principal);
        return ResponseEntity.ok().build();
    }
}
