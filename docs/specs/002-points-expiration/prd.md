---
kind: prd
feature: 002-points-expiration
version: 0.1.0
status: drafted
---

# PRD: Points Expiration — `002-points-expiration`

## 1. Executive Summary
El sistema debe gestionar la expiración de puntos de lealtad tras 12 meses de inactividad del cliente. Un saldo expirado debe volver a cero y generar un evento de notificación.

## 2. Requirements (Functional)

### REQ-001: Detección de Inactividad
El sistema debe identificar a los clientes cuyo último movimiento en el ledger sea superior a 12 meses.
- **AC1:** La inactividad se mide desde el campo `appended_at` del último registro en la tabla `movement`.

### REQ-002: Ejecución de Expiración
Cuando un cliente es identificado como inactivo, su saldo debe ajustarse a cero.
- **AC1:** Se debe insertar un nuevo movimiento de tipo `DEBIT` con la causa `POINTS_EXPIRED`.
- **AC2:** El monto del débito debe ser exactamente igual al saldo actual del cliente.
- **AC3:** Tras la operación, el saldo en `customer_balance` debe ser 0.

### REQ-003: Notificación de Expiración
Cada vez que un saldo expire, se debe emitir un evento a través de Kafka.
- **AC1:** El evento debe contener el `customerId`, el monto expirado y el `timestamp` de la operación.
- **AC2:** La notificación debe ser confiable (usando el patrón Outbox).

### REQ-004: Trazabilidad
La expiración debe ser visible en el historial de movimientos del cliente.
- **AC1:** El movimiento de expiración debe aparecer en la API de movimientos (`GET /loyalty/customers/{id}/movements`).

## 3. Non-Functional Requirements
- **NFR-001:** El proceso de expiración no debe impactar el rendimiento de la ingesta de puntos en tiempo real.
- **NFR-002:** El sistema debe ser capaz de procesar expiraciones de forma masiva (batch) diariamente.

## 4. Acceptance Criteria (System)
- **Given** un cliente con 500 puntos y su último movimiento fue hace 13 meses.
- **When** se ejecuta el proceso de expiración.
- **Then** el saldo del cliente pasa a 0, se registra un movimiento de débito por 500 y se emite un evento de `PointsExpired`.

## 5. Constraints
- La expiración es irreversible por procesos automáticos.
- Solo se expiran saldos positivos > 0.

## 6. Open Questions
- [NEEDS CLARIFICATION] ¿Se requiere pre-aviso a los 11 meses? (Fuera de alcance inicial).
