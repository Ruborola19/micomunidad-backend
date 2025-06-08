package com.micomunity.backend.service;

import com.micomunity.backend.dto.PostDTO;
import com.micomunity.backend.model.Community;
import com.micomunity.backend.model.Post;
import com.micomunity.backend.model.Role;
import com.micomunity.backend.model.User;
import com.micomunity.backend.repository.CommunityRepository;
import com.micomunity.backend.repository.PostRepository;
import com.micomunity.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommunityRepository communityRepository;

    @Transactional
    public PostDTO createPost(String title, String content, String userEmail) {
        log.debug("Intentando crear post con título: {} para el usuario: {}", title, userEmail);

        User author = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userEmail));

        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setAuthor(author);
        post.setCommunity(author.getCommunity());

        post = postRepository.save(post);
        log.debug("Post creado exitosamente con ID: {}", post.getId());

        return convertToDTO(post);
    }

    @Transactional(readOnly = true)
    public Page<PostDTO> getCommunityPosts(String communityCode, Pageable pageable) {
        if (communityCode == null || communityCode.trim().isEmpty()) {
            log.error("Código de comunidad nulo o vacío");
            throw new IllegalArgumentException("El código de comunidad es obligatorio");
        }

        log.debug("Buscando posts para la comunidad: {} con paginación: {}", communityCode, pageable);

        Community community = communityRepository.findByCommunityCode(communityCode)
                .orElseThrow(() -> {
                    log.error("Comunidad no encontrada con código: {}", communityCode);
                    return new IllegalArgumentException("Comunidad no encontrada: " + communityCode);
                });

        Pageable correctedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "creationDate")
        );

        Page<Post> posts = postRepository.findByCommunityId(community.getId(), correctedPageable);
        log.debug("Encontrados {} posts para la comunidad {}", posts.getTotalElements(), communityCode);

        return posts.map(this::convertToDTO);
    }

    @Transactional
    public void deletePost(Long postId, String userEmail) {
        log.debug("Intentando eliminar post {} por usuario {}", postId, userEmail);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post no encontrado: " + postId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userEmail));

        if (!canDeletePost(user, post)) {
            log.error("Usuario {} no tiene permiso para eliminar el post {}", userEmail, postId);
            throw new IllegalArgumentException("No tienes permiso para eliminar este post");
        }

        postRepository.delete(post);
        log.debug("Post {} eliminado exitosamente", postId);
    }

    private boolean canDeletePost(User user, Post post) {
        if (user.getRole() == Role.PRESIDENTE) {
            return true;
        }

        if (user.getRole() == Role.VECINO) {
            return post.getAuthor().getId().equals(user.getId());
        }

        return false;
    }

    private PostDTO convertToDTO(Post post) {
        PostDTO dto = new PostDTO();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setAuthorId(post.getAuthor().getId());
        dto.setAuthorName(post.getAuthor().getFullName());
        dto.setAuthorRole(post.getAuthor().getRole().name());
        dto.setCommunityCode(post.getCommunity().getCommunityCode());
        dto.setCreationDate(post.getCreationDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return dto;
    }
} 
