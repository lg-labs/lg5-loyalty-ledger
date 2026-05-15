---
kind: intent
feature: 002-points-expiration
version: 0.1.0
status: drafted
---

# Intent: Points Expiration — `002-points-expiration`

## Problem statement
Actualmente, los puntos de lealtad en `lg5-loyalty-ledger` se acumulan indefinidamente. Esto genera un pasivo financiero creciente y no incentiva la recurrencia de los clientes. El sistema carece de un mecanismo para limpiar saldos de cuentas abandonadas o inactivas.

## Users / Roles
- **Finanzas:** Requiere previsibilidad sobre el pasivo de puntos y la capacidad de expirar deudas técnicas de lealtad.
- **Marketing:** Desea utilizar la expiración como una herramienta de urgencia para reactivar clientes.
- **Plataforma:** Se beneficia de la limpieza de datos inactivos en el ledger.

## Why now?
El crecimiento del ledger sin políticas de expiración está empezando a afectar las proyecciones de balance contable de la compañía. Se requiere alinear la política de puntos con los términos y condiciones de uso de la plataforma.

## Desired outcome
- Los saldos de clientes que no han tenido actividad (créditos o débitos) en los últimos 12 meses deben ponerse a cero.
- Cada expiración debe registrarse como un movimiento específico en el ledger para trazabilidad.
- El sistema debe emitir una señal externa cada vez que ocurra una expiración.

## Success metrics
- Reducción del pasivo circulante de puntos acumulados por clientes inactivos.
- Trazabilidad completa de cada evento de expiración en el historial del cliente.

## Constraints & Assumptions
- La inactividad se define estrictamente como la ausencia de movimientos en el ledger de lealtad.
- La expiración resetea el saldo total actual a cero.
- El periodo de inactividad es de 12 meses.

## Non-goals
- No se implementará la gestión de notificaciones directas al usuario (Push/Email) dentro de este servicio.
- No se manejarán expiraciones parciales por lotes de puntos (FIFO/LIFO) en esta primera versión; solo expiración por inactividad total.

## Open questions
- ¿Deben las expiraciones ser reversibles por el equipo de soporte?
- ¿A qué hora del día debe ejecutarse el proceso de expiración?
- [NEEDS CLARIFICATION] ¿Se requiere un pre-aviso (ej. a los 11 meses) o solo la notificación del hecho consumado?
