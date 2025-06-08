package com.micomunity.backend.dto;

import com.micomunity.backend.model.TipoDocumento;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoRequest {
    private String comentario;
    private TipoDocumento tipo;
    private List<MultipartFile> archivos;
} 