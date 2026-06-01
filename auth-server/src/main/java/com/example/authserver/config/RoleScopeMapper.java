package com.example.authserver.config;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Role → Scope mapping table.
 *
 * To add a new role, you only need to add a case here.
 * No downstream service (order-service, product-service, etc.) needs to change.
 *
 * ┌─────────────────────┬──────────────────────────────────────────────────────────────┐
 * │ Role                │ Granted Scopes                                               │
 * ├─────────────────────┼──────────────────────────────────────────────────────────────┤
 * │ ROLE_USER           │ catalog:read                                                 │
 * │                     │ orders:read, orders:write                                    │
 * │                     │ cart:read, cart:write                                        │
 * │                     │ payments:read, payments:write                                │
 * │                     │ users:read, users:write                                      │
 * │                     │ notifications:read                                           │
 * ├─────────────────────┼──────────────────────────────────────────────────────────────┤
 * │ ROLE_PREMIUM_USER   │ All ROLE_USER scopes                                         │
 * │                     │ + payments:manage (premium features such as self-refunds)   │
 * ├─────────────────────┼──────────────────────────────────────────────────────────────┤
 * │ ROLE_MANAGER        │ All ROLE_USER scopes                                         │
 * │                     │ + catalog:write                                              │
 * │                     │ + orders:manage                                              │
 * │                     │ + payments:manage                                            │
 * │                     │ + notifications:write                                        │
 * ├─────────────────────┼──────────────────────────────────────────────────────────────┤
 * │ ROLE_ADMIN          │ All ROLE_MANAGER scopes                                      │
 * │                     │ + users:manage                                               │
 * └─────────────────────┴──────────────────────────────────────────────────────────────┘
 *
 * Rules:
 *   - Endpoints check only SCOPE_xxx and never use hasRole().
 *   - Authorization decisions are centralized in this file.
 *   - Service-to-service calls use service-account client_credentials tokens;
 *     the token customizer grants these scopes directly without consulting this mapper.
 */
public final class RoleScopeMapper {

    private RoleScopeMapper() {}

    // ── Base scope groups ───────────────────────────────────────────────────

    /** Base scopes available to all users */
    private static final Set<String> USER_SCOPES = Set.of(
            "catalog:read",
            "catalog:write",         // stock deduction in the order flow (order-service → product-service)
            "orders:read",   "orders:write",
            "orders:manage",
            "cart:read",     "cart:write",
            "payments:read", "payments:write",
            "users:read",    "users:write",
            "notifications:read",
            "notifications:write"    // notification sending in the order flow (order-service → notification-service)
    );

    /** Scopes granted to managers in addition to user scopes */
    private static final Set<String> MANAGER_EXTRA_SCOPES = Set.of(
            "orders:manage",
            "payments:manage"
    );

    /** Scopes granted to admins in addition to manager scopes */
    private static final Set<String> ADMIN_EXTRA_SCOPES = Set.of(
            "users:manage"
    );

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the scope set derived from the given role set.
     * The highest role wins (hierarchical merging).
     *
     * To add a new role, add a case here:
     *   case "ROLE_PREMIUM_USER" → scopes.addAll(USER_SCOPES); scopes.add("xxx"); break;
     */
    public static Set<String> scopesFor(Collection<String> roles) {
        Set<String> scopes = new LinkedHashSet<>();

        for (String role : roles) {
            switch (role) {

                case "ROLE_USER" -> scopes.addAll(USER_SCOPES);

                // ── Example of a new role: premium user ─────────────────────
                // Gets all USER scopes + self-refund capability (payments:manage)
                case "ROLE_PREMIUM_USER" -> {
                    scopes.addAll(USER_SCOPES);
                    scopes.add("payments:manage");       // can refund their own payments
                }

                case "ROLE_MANAGER" -> {
                    scopes.addAll(USER_SCOPES);
                    scopes.addAll(MANAGER_EXTRA_SCOPES);
                }

                case "ROLE_ADMIN" -> {
                    scopes.addAll(USER_SCOPES);
                    scopes.addAll(MANAGER_EXTRA_SCOPES);
                    scopes.addAll(ADMIN_EXTRA_SCOPES);
                }

                // Unknown roles are silently ignored
                default -> {}
            }
        }

        return scopes;
    }
}

