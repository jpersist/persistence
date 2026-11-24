/*
 * Copyright 2024-2026 JPersist
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hibernate.base.persistence.tools;

import persistence.tools.EntityModelLinter;
import persistence.tools.LintDiagnostic;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Base template implementation of {@link EntityModelLinter} that executes
 * namespace-agnostic Java reflection rules and delegates JPA-specific metamodel
 * inspection to concrete Hibernate provider subclasses.
 *
 * <p>This abstract class performs common bytecode and reflection checks across all configured
 * entity classes, including:</p>
 * <ul>
 *   <li>Validating that entity classes are not declared {@code final} ({@link EntityLintRule#FINAL_ENTITY_CLASS}).</li>
 *   <li>Verifying the presence of a {@code public} or {@code protected} no-argument constructor ({@link EntityLintRule#MISSING_NO_ARG_CONSTRUCTOR}).</li>
 *   <li>Ensuring persistent getter and setter methods are not declared {@code final} ({@link EntityLintRule#FINAL_PERSISTENT_METHOD}).</li>
 * </ul>
 *
 * <p>Concrete subclasses implement {@link #lintEntityModel(Class, List)} to apply JPA annotation
 * and Hibernate-specific metamodel inspections for their respective specification versions.</p>
 *
 * @since 1.2.0
 * @see EntityModelLinter
 * @see EntityLintRule
 * @see LintDiagnostic
 */
public abstract class AbstractEntityModelLinter implements EntityModelLinter {

    private final List<String> entityClassNames;
    private final ClassLoader classLoader;

    /**
     * Constructs a new {@code AbstractEntityModelLinter} with the specified entity class names
     * and class loader.
     *
     * @param entityClassNames the list of fully-qualified entity class names to inspect; must not be {@code null}
     * @param classLoader      the class loader used to load and inspect entity classes; must not be {@code null}
     * @throws NullPointerException if {@code entityClassNames} or {@code classLoader} is {@code null}
     */
    protected AbstractEntityModelLinter(List<String> entityClassNames, ClassLoader classLoader) {
        this.entityClassNames = List.copyOf(Objects.requireNonNull(entityClassNames, "entityClassNames must not be null"));
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader must not be null");
    }

    /**
     * Returns the unmodifiable list of fully-qualified entity class names to be inspected.
     *
     * @return the list of entity class names
     */
    protected List<String> getEntityClassNames() {
        return entityClassNames;
    }

    /**
     * Returns the {@link ClassLoader} used to load entity classes for inspection.
     *
     * @return the class loader
     */
    protected ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Executes common structural reflection checks across all configured entity classes and invokes
     * {@link #lintEntityModel(Class, List)} for JPA-specific inspections.</p>
     *
     * @return an unmodifiable list of discovered {@link LintDiagnostic} findings
     * @throws IllegalArgumentException if an entity class cannot be loaded by the configured class loader
     */
    @Override
    public final List<LintDiagnostic> lint() {
        List<LintDiagnostic> diagnostics = new ArrayList<>();

        for (String className : entityClassNames) {
            Class<?> entityClass = loadEntityClass(className);
            checkFinalEntityClass(entityClass, diagnostics);
            checkNoArgConstructor(entityClass, diagnostics);
            checkFinalMethods(entityClass, diagnostics);

            // Delegate JPA annotation / Hibernate Metamodel checks to Hibernate 5 or ORM 6 subclass
            lintEntityModel(entityClass, diagnostics);
        }

        return Collections.unmodifiableList(diagnostics);
    }

    /**
     * Hook for concrete Hibernate 5 ({@code javax}) and Hibernate ORM 6 ({@code jakarta})
     * implementations to inspect JPA annotations and Hibernate boot metadata.
     *
     * @param entityClass the loaded entity class being inspected
     * @param diagnostics mutable list to collect discovered {@link LintDiagnostic} findings
     */
    protected abstract void lintEntityModel(Class<?> entityClass, List<LintDiagnostic> diagnostics);

    private Class<?> loadEntityClass(String className) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to load entity class: " + className, e);
        }
    }

    private void checkFinalEntityClass(Class<?> entityClass, List<LintDiagnostic> diagnostics) {
        if (Modifier.isFinal(entityClass.getModifiers())) {
            diagnostics.add(EntityLintRule.FINAL_ENTITY_CLASS.diagnostic(entityClass.getName()));
        }
    }

    private void checkNoArgConstructor(Class<?> entityClass, List<LintDiagnostic> diagnostics) {
        boolean hasValidNoArgConstructor = false;
        for (Constructor<?> constructor : entityClass.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == 0) {
                int modifiers = constructor.getModifiers();
                if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
                    hasValidNoArgConstructor = true;
                    break;
                }
            }
        }
        if (!hasValidNoArgConstructor) {
            diagnostics.add(EntityLintRule.MISSING_NO_ARG_CONSTRUCTOR.diagnostic(entityClass.getName()));
        }
    }

    private void checkFinalMethods(Class<?> entityClass, List<LintDiagnostic> diagnostics) {
        for (Method method : entityClass.getDeclaredMethods()) {
            int modifiers = method.getModifiers();
            if (!Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers) && Modifier.isFinal(modifiers)) {
                diagnostics.add(EntityLintRule.FINAL_PERSISTENT_METHOD.diagnostic(
                    entityClass.getName(),
                    method.getName()
                ));
            }
        }
    }

}
