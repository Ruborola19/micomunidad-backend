-- Script de depuración para verificar roles de usuarios
SELECT 
    u.id,
    u.full_name,
    u.email,
    u.role,
    u.community_id,
    c.name as community_name,
    c.president_id
FROM users u
LEFT JOIN communities c ON u.community_id = c.id
ORDER BY u.community_id, u.role, u.full_name;

-- Verificar si hay inconsistencias entre president_id y roles
SELECT 
    'Inconsistencia: Usuario es presidente en tabla communities pero su rol no es PRESIDENTE' as problema,
    u.id,
    u.full_name,
    u.role,
    c.name as community_name
FROM users u
JOIN communities c ON c.president_id = u.id
WHERE u.role != 'PRESIDENTE'

UNION ALL

SELECT 
    'Inconsistencia: Usuario tiene rol PRESIDENTE pero no está asignado como presidente en communities' as problema,
    u.id,
    u.full_name,
    u.role,
    c.name as community_name
FROM users u
LEFT JOIN communities c ON c.president_id = u.id
WHERE u.role = 'PRESIDENTE' AND c.president_id IS NULL; 