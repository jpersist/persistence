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

package hibernate.jakarta.persistence.tools;

import hibernate.base.persistence.tools.AbstractEntityModelLinter;
import hibernate.base.persistence.tools.EntityLintRule;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import persistence.tools.LintDiagnostic;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Hibernate ORM&nbsp;6 and Jakarta Persistence implementation of {@link persistence.tools.EntityModelLinter}.
 *
 * <p>Inherits standard reflection checks from {@link AbstractEntityModelLinter} (such as non-final
 * entity class rules, accessible no-arg constructors, and non-final getter/setter methods) and inspects
 * {@code jakarta.persistence} relationship annotations across fields and property getter methods:</p>
 *
 * <ul>
 *   <li>{@link EntityLintRule#EAGER_TO_MANY_ASSOCIATION} &ndash; flags {@code @OneToMany} and {@code @ManyToMany}
 *       associations configured with {@link FetchType#EAGER}.</li>
 *   <li>{@link EntityLintRule#MISSING_MAPPED_BY} &ndash; flags bidirectional {@code @OneToMany} associations
 *       omitting the {@code mappedBy} attribute.</li>
 *   <li>{@link EntityLintRule#EAGER_TO_ONE_ASSOCIATION} &ndash; flags {@code @ManyToOne} and {@code @OneToOne}
 *       associations configured with {@link FetchType#EAGER}.</li>
 *   <li>{@link EntityLintRule#UNORDERED_MANY_TO_MANY_LIST} &ndash; flags {@code @ManyToMany} associations typed as
 *       {@link List} without an accompanying {@link OrderColumn} annotation.</li>
 * </ul>
 *
 * @since 2.2.0
 * @see persistence.tools.EntityModelLinter
 * @see AbstractEntityModelLinter
 * @see EntityLintRule
 * @see hibernate.jakarta.persistence.tools.provider.HibernateEntityModelLinterFactory
 */
public class HibernateEntityModelLinter extends AbstractEntityModelLinter {

    private HibernateEntityModelLinter(List<String> entityClassNames, ClassLoader classLoader) {
        super(entityClassNames, classLoader);
    }

    /**
     * Creates a new {@code HibernateEntityModelLinter} for the given entity class names
     * and class loader.
     *
     * @param entityClassNames the list of fully-qualified entity class names to inspect; must not be {@code null}
     * @param classLoader      the class loader used to load and inspect entity classes; must not be {@code null}
     * @return a new {@code HibernateEntityModelLinter} instance
     * @throws NullPointerException if {@code entityClassNames} or {@code classLoader} is {@code null}
     */
    public static HibernateEntityModelLinter of(List<String> entityClassNames, ClassLoader classLoader) {
        return new HibernateEntityModelLinter(entityClassNames, classLoader);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Inspects declared fields and getter methods for {@code jakarta.persistence} association annotations
     * and evaluates them against standard anti-pattern rules.</p>
     *
     * @param entityClass the loaded entity class being inspected
     * @param diagnostics mutable list to collect discovered {@link LintDiagnostic} findings
     */
    @Override
    protected void lintEntityModel(Class<?> entityClass, List<LintDiagnostic> diagnostics) {
        for (Field field : entityClass.getDeclaredFields()) {
            inspectMember(entityClass.getName(), field.getName(), field.getType(), field, diagnostics);
        }
        for (Method method : entityClass.getDeclaredMethods()) {
            if (method.getParameterCount() == 0 && method.getReturnType() != void.class) {
                inspectMember(entityClass.getName(), method.getName(), method.getReturnType(), method, diagnostics);
            }
        }
    }

    private void inspectMember(String className, String memberName, Class<?> memberType,
                               AnnotatedElement element, List<LintDiagnostic> diagnostics) {
        OneToMany oneToMany = element.getAnnotation(OneToMany.class);
        if (oneToMany != null) {
            if (oneToMany.fetch() == FetchType.EAGER) {
                diagnostics.add(EntityLintRule.EAGER_TO_MANY_ASSOCIATION.diagnostic(className, memberName));
            }
            if (oneToMany.mappedBy().isEmpty()) {
                diagnostics.add(EntityLintRule.MISSING_MAPPED_BY.diagnostic(className, memberName));
            }
        }

        ManyToMany manyToMany = element.getAnnotation(ManyToMany.class);
        if (manyToMany != null) {
            if (manyToMany.fetch() == FetchType.EAGER) {
                diagnostics.add(EntityLintRule.EAGER_TO_MANY_ASSOCIATION.diagnostic(className, memberName));
            }
            if (List.class.isAssignableFrom(memberType) && !element.isAnnotationPresent(OrderColumn.class)) {
                diagnostics.add(EntityLintRule.UNORDERED_MANY_TO_MANY_LIST.diagnostic(className, memberName));
            }
        }

        ManyToOne manyToOne = element.getAnnotation(ManyToOne.class);
        if (manyToOne != null && manyToOne.fetch() == FetchType.EAGER) {
            diagnostics.add(EntityLintRule.EAGER_TO_ONE_ASSOCIATION.diagnostic(className, memberName));
        }

        OneToOne oneToOne = element.getAnnotation(OneToOne.class);
        if (oneToOne != null && oneToOne.fetch() == FetchType.EAGER) {
            diagnostics.add(EntityLintRule.EAGER_TO_ONE_ASSOCIATION.diagnostic(className, memberName));
        }
    }

}
