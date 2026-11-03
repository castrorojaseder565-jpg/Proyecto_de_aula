# Data Warehouse de BoviControl

## Objetivo

Analizar la producción de leche y el control de vacunas del hato mediante un almacén separado de la aplicación operacional.

## Herramientas y flujo

```text
BoviControl (MySQL) -> Apache Hop (extracción y transformación) -> PostgreSQL (DW) -> Power BI
```

MySQL sigue siendo la base de datos de BoviControl. PostgreSQL se usa como destino analítico. Hop copia los datos de origen a `staging`, los limpia y carga las dimensiones antes de las tablas de hechos.

## Modelo dimensional propuesto

### Dimensiones

- `dw.dim_tiempo`: calendario compartido. En vacunación se relaciona con la fecha de aplicación y la próxima dosis.
- `dw.dim_bovino`: identificación, arete, raza, peso, edad, sexo, salud y propietario. Para este laboratorio se actualiza con estrategia SCD Tipo 1.
- `dw.dim_turno`: mañana, tarde y noche para los registros de leche.
- `dw.dim_tipo_vacuna`: catálogo de vacunas derivado de los nombres registrados.

Cada dimensión incluye el miembro `-1` para representar datos desconocidos.

### Hechos y grano

- `dw.fact_produccion_leche`: una fila por registro de `leche_registro`; contiene los litros registrados para un bovino, fecha y turno.
- `dw.fact_vacunacion`: una fila por registro de `vacuna`; conserva bovino, tipo de vacuna, fecha aplicada, próxima dosis e intervalo.

Las claves de origen (`id_registro_origen` e `id_vacuna_origen`) permiten que Hop recargue los hechos sin duplicarlos.

## Indicadores para Power BI

- Litros producidos: suma de `fact_produccion_leche[litros]`.
- Registros de ordeño: conteo de filas de `fact_produccion_leche`.
- Producción promedio por registro: litros divididos por registros.
- Aplicaciones de vacunas: conteo de filas de `fact_vacunacion`.
- Dosis vencidas o próximas: comparar la fecha de próxima dosis con la fecha actual. El estado puede cambiar con el tiempo, así que no se debe confiar únicamente en el estado guardado en el origen.

## Carga ETL sugerida en Apache Hop

1. Crear conexión de origen MySQL a la misma base que usa BoviControl y conexión de destino PostgreSQL.
2. Leer `ganado`, `vacuna` y `leche_registro` del origen.
3. Cargar cada extracción a su tabla `staging.stg_*` correspondiente.
4. Limpiar nulos y normalizar turnos (`MAÑANA`/`MANANA` a `MANANA`). Validar litros positivos y fechas.
5. Cargar primero `dim_bovino`, `dim_turno` y `dim_tipo_vacuna`; la dimensión de tiempo ya se genera con el SQL.
6. Resolver las claves sustitutas (`sk_*`) desde las dimensiones.
7. Cargar `fact_produccion_leche` y `fact_vacunacion`, usando `-1` para una dimensión sin correspondencia y como fecha de próxima dosis si el origen no la tiene.
8. Ejecutar de nuevo el flujo y comprobar que los conteos no se duplican.

## Validaciones SQL iniciales

```sql
SELECT 'ganado' AS tabla, COUNT(*) AS filas FROM dw.dim_bovino
UNION ALL SELECT 'leche', COUNT(*) FROM dw.fact_produccion_leche
UNION ALL SELECT 'vacunacion', COUNT(*) FROM dw.fact_vacunacion;

SELECT id_registro_origen, COUNT(*)
FROM dw.fact_produccion_leche
GROUP BY id_registro_origen
HAVING COUNT(*) > 1;

SELECT COUNT(*) AS referencias_bovino_huerfanas
FROM dw.fact_produccion_leche f
LEFT JOIN dw.dim_bovino d ON d.sk_bovino = f.sk_bovino
WHERE d.sk_bovino IS NULL;

SELECT fecha, SUM(litros) AS litros
FROM dw.fact_produccion_leche f
JOIN dw.dim_tiempo t ON t.sk_fecha = f.sk_fecha
GROUP BY fecha
ORDER BY fecha;
```

El SQL `01_modelo_dw_postgresql.sql` crea los esquemas, tablas, índices, miembros desconocidos y calendario inicial. Se debe ejecutar en la base PostgreSQL del almacén, no en la base MySQL de la aplicación.

## Consideración de alcance

El código actual permite editar o eliminar registros de leche del día actual y guarda una fecha de creación. Si se requiere conservar cada cambio como histórico analítico, se deberá acordar una estrategia incremental y de auditoría; una carga que solo copia el estado actual no recupera filas borradas antes de la extracción.
