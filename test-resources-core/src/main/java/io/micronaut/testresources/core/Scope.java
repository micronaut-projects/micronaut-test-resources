/*
 * Copyright 2017-2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.testresources.core;

/**
 * A scope represents the lifecycle of a test resource.
 * The root scope is handled by the test resources provider,
 * and lives as long as the provider is alive, or that it is
 * explicitly closed.
 *
 * The scope property is always implicitly requested.
 */
public final class Scope {

    /**
     * The default scope.
     */
    public static final Scope ROOT = new Scope(null, null);

    /**
     * The property name, for when the scope is passed as a property string.
     */
    public static final String PROPERTY_KEY = "micronaut.test.resources.scope";

    private final String id;
    private final Scope parent;

    private Scope(Scope parent, String id) {
        this.id = id;
        this.parent = parent;
    }

    /**
     * Creates a child scope with the given id.
     * @param id the child id
     * @return the child scope
     */
    public Scope child(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Child name cannot be null");
        }
        return new Scope(this, id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Scope scope = (Scope) o;

        if (id != null ? !id.equals(scope.id) : scope.id != null) {
            return false;
        }
        return parent != null ? parent.equals(scope.parent) : scope.parent == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (parent != null ? parent.hashCode() : 0);
        return result;
    }

    public String toString() {
        if (id == null || parent == null) {
            return "";
        }
        if (ROOT.equals(parent)) {
            return id;
        }
        return parent + "." + id;
    }

    /**
     * Determines if this scope is the supplied scope,
     * or the parent of the supplied scope.
     * @param scope the scope to check
     * @return true if this scope includes the supplied scope
     */
    public boolean includes(Scope scope) {
        Scope cur = scope;
        while (cur != null) {
            if (cur.equals(this)) {
                return true;
            }
            cur = cur.parent;
        }
        return false;
    }

    /**
     * Returns a new scope with the given id.
     * @param id the scope id
     * @return the scope
     */
    public static Scope of(String id) {
        if (id == null || id.isEmpty()) {
            return ROOT;
        }
        Scope scope = ROOT;
        for (String elem : id.split("[.]")) {
            scope = scope.child(elem);
        }
        return scope;
    }

}
