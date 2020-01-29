/*
 * Copyright 2018 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hivemq.extensions.rbac.configuration;

import com.hivemq.extensions.rbac.configuration.entities.*;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigCredentialsValidator {

    public static @NotNull ValidationResult validateConfig(
            final @NotNull ExtensionConfig extensionConfig, final @NotNull FileAuthConfig config) {

        final List<String> errors = new ArrayList<>();
        boolean validationSuccessful = true;

        final String defaultRole = config.getDefaultRole();
        final List<User> users = config.getUsers();
        if ((defaultRole == null || defaultRole.isEmpty()) && users.isEmpty()) {
            errors.add("No users or default role found in configuration file");
            validationSuccessful = false;
        }

        final List<Role> roles = config.getRoles();
        if (roles.isEmpty()) {
            errors.add("No roles found in configuration file");
            validationSuccessful = false;
        }

        //if users or roles are missing stop here
        if (!validationSuccessful) {
            return new ValidationResult(errors, false);
        }

        final Set<String> roleIds = new HashSet<>();

        for (final Role role : roles) {
            if (role.getId() == null || role.getId().isEmpty()) {
                errors.add("A role is missing an ID");
                validationSuccessful = false;
                continue;
            }

            if (roleIds.contains(role.getId())) {
                errors.add("Duplicate ID '" + role.getId() + "' for role");
                validationSuccessful = false;
                continue;
            }

            roleIds.add(role.getId());

            if (role.getPermissions() == null || role.getPermissions().isEmpty()) {
                errors.add("Role '" + role.getId() + "' is missing permissions");
                validationSuccessful = false;
                continue;
            }

            for (final Permission permission : role.getPermissions()) {
                if (permission.getTopic() == null || permission.getTopic().isEmpty()) {
                    errors.add("A permission for role with id '" + role.getId() + "' is missing a topic filter");
                    validationSuccessful = false;
                }

                if (permission.getActivity() == null) {
                    errors.add("Invalid value for activity in permission for role with id '" + role.getId() + "'");
                    validationSuccessful = false;
                }

                if (permission.getQos() == null) {
                    errors.add("Invalid value for QoS in permission for role with id '" + role.getId() + "'");
                    validationSuccessful = false;
                }

                if (permission.getRetain() == null) {
                    errors.add("Invalid value for retain in permission for role with id '" + role.getId() + "'");
                    validationSuccessful = false;
                }

                if (permission.getSharedGroup() == null || permission.getSharedGroup().isEmpty()) {
                    errors.add("Invalid value for shared group in permission for role with id '" + role.getId() + "'");
                    validationSuccessful = false;
                }

                if (permission.getSharedSubscription() == null) {
                    errors.add("Invalid value for shared subscription in permission for role with id '" + role.getId() + "'");
                    validationSuccessful = false;
                }
            }
        }

        if (!(defaultRole == null || defaultRole.isEmpty()) && !roleIds.contains(defaultRole)) {
            errors.add("No role defined for default role '" + defaultRole + "'");
            validationSuccessful = false;
        }

        final Set<String> userNames = new HashSet<>();

        for (final User user : users) {
            if (user.getName() == null || user.getName().isEmpty()) {
                errors.add("A user is missing a name");
                validationSuccessful = false;
                continue;
            }

            if (userNames.contains(user.getName())) {
                errors.add("Duplicate name '" + user.getName() + "' for user");
                validationSuccessful = false;
                continue;
            }

            userNames.add(user.getName());

            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                errors.add("User '" + user.getName() + "' is missing a password");
                validationSuccessful = false;
                continue;
            }

            if (extensionConfig.getPasswordType() == PasswordType.HASHED) {

                final String password = user.getPassword();
                final String[] split = password.split(":");

                if (split.length < 2 || split[0].isEmpty() || split[1].isEmpty()) {
                    errors.add("User '" + user.getName() + "' has invalid password");
                    validationSuccessful = false;
                    continue;
                }
            }

            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                errors.add("User '" + user.getName() + "' is missing roles");
                validationSuccessful = false;
                continue;
            }

            for (final String role : user.getRoles()) {
                if (role == null || role.isEmpty()) {
                    errors.add("Invalid role for user '" + user.getName() + "'");
                    validationSuccessful = false;
                    continue;
                }

                if (!roleIds.contains(role)) {
                    errors.add("Unknown role '" + role + "' for user '" + user.getName() + "'");
                    validationSuccessful = false;
                }
            }
        }

        return new ValidationResult(errors, validationSuccessful);
    }

    public static class ValidationResult {
        private final @NotNull List<String> errors;
        private final boolean validationSuccessful;

        private ValidationResult(final @NotNull List<String> errors, final boolean validationSuccessful) {
            this.errors = errors;
            this.validationSuccessful = validationSuccessful;
        }

        public @NotNull List<String> getErrors() {
            return errors;
        }

        public boolean isValidationSuccessful() {
            return validationSuccessful;
        }
    }
}
