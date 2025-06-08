package com.micomunity.backend.controller;

import com.micomunity.backend.dto.PostDTO;
import com.micomunity.backend.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    @PreAuthorize("hasAnyRole('PRESIDENTE', 'VECINO')")
    public ResponseEntity<PostDTO> createPost(@RequestBody Map<String, String> request, Authentication authentication) {
        log.debug("Creando nuevo post para el usuario: {}", authentication.getName());
        String title = request.get("title");
        String content = request.get("content");
        
        if (title == null || content == null) {
            log.error("Intento de crear post sin título o contenido");
            throw new IllegalArgumentException("El título y el contenido son obligatorios");
        }

        try {
            PostDTO post = postService.createPost(title, content, authentication.getName());
            log.debug("Post creado exitosamente con ID: {}", post.getId());
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            log.error("Error al crear post: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/community/{communityCode}")
    public ResponseEntity<Page<PostDTO>> getCommunityPosts(
            @PathVariable String communityCode,
            @PageableDefault(size = 10, sort = "creationDate,desc") Pageable pageable) {
        log.debug("Obteniendo posts para la comunidad: {} con página: {} y tamaño: {}", 
                 communityCode, pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Page<PostDTO> posts = postService.getCommunityPosts(communityCode, pageable);
            log.debug("Encontrados {} posts para la comunidad {}", posts.getTotalElements(), communityCode);
            return ResponseEntity.ok(posts);
        } catch (IllegalArgumentException e) {
            log.error("Error al obtener posts de la comunidad {}: {}", communityCode, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al obtener posts de la comunidad {}: {}", communityCode, e.getMessage(), e);
            throw e;
        }
    }

    @DeleteMapping("/{postId}")
    @PreAuthorize("hasAnyRole('PRESIDENTE', 'VECINO')")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId, Authentication authentication) {
        log.debug("Eliminando post {} por el usuario {}", postId, authentication.getName());
        try {
            postService.deletePost(postId, authentication.getName());
            log.debug("Post {} eliminado exitosamente", postId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error al eliminar post {}: {}", postId, e.getMessage(), e);
            throw e;
        }
    }
}
