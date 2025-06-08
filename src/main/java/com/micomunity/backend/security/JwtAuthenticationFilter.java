package com.micomunity.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    
    private final List<String> PUBLIC_PATHS = Arrays.asList(
        "/api/auth/register/president",
        "/api/auth/register",
        "/api/auth/login",
        "/uploads",
        "/api/documentos/download",
        "/api/incidencias/download",
        "/error"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        // Logging específico para rutas de imágenes
        if (path.contains("/download/")) {
            log.info("=== JWT FILTER PARA DESCARGA ===");
            log.info("Path: {}", path);
            log.info("Method: {}", method);
        }

        // Permitir todas las peticiones OPTIONS (necesario para CORS)
        if (HttpMethod.OPTIONS.matches(method)) {
            log.debug("Permitiendo petición OPTIONS para: {}", path);
            return true;
        }

        // BYPASS DIRECTO: Cualquier ruta que contenga "/download/" es pública
        if (path.contains("/download/")) {
            log.info("BYPASS DIRECTO: Permitiendo descarga para: {}", path);
            return true;
        }

        // BYPASS DIRECTO: Permitir acceso a /uploads/**
        if (path.startsWith("/uploads/")) {
            log.info("BYPASS DIRECTO: Permitiendo acceso a uploads para: {}", path);
            return true;
        }

        // BYPASS DIRECTO: Permitir acceso a /api/uploads/** (por compatibilidad)
        if (path.startsWith("/api/uploads/")) {
            log.info("BYPASS DIRECTO: Permitiendo acceso a api/uploads para: {}", path);
            return true;
        }

        // Verificar rutas específicas de descarga de imágenes de incidencias
        if (path.startsWith("/api/incidencias/download/")) {
            log.info("Permitiendo acceso a descarga de imagen de incidencia: {}", path);
            return true;
        }

        // Permitir rutas públicas generales
        boolean isPublic = PUBLIC_PATHS.stream()
                .anyMatch(publicPath -> path.startsWith(publicPath));
        
        if (path.contains("/download/")) {
            log.info("¿Es ruta pública general? {}", isPublic);
            log.info("Rutas públicas configuradas: {}", PUBLIC_PATHS);
        }
        
        return isPublic;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            if (shouldNotFilter(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            final String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token no proporcionado o formato inválido");
                return;
            }

            final String jwt = authHeader.substring(7);
            final String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    filterChain.doFilter(request, response);
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Token inválido");
                }
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Usuario no encontrado o no autenticado");
            }
        } catch (Exception e) {
            log.error("Error en el filtro JWT", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error en la autenticación: " + e.getMessage());
        }
    }
} 