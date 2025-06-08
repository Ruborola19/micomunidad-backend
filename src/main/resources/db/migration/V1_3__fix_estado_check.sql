-- Primero eliminamos la restricción existente
ALTER TABLE incidencias DROP CONSTRAINT IF EXISTS incidencias_estado_check;

-- Luego añadimos la nueva restricción con los valores correctos
ALTER TABLE incidencias ADD CONSTRAINT incidencias_estado_check 
    CHECK (estado IN ('ABIERTA', 'EN_PROCESO', 'RESUELTA', 'CANCELADA')); 