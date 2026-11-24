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

package hibernate.javax.persistence.tools;

import org.junit.jupiter.api.Test;
import persistence.tools.LintDiagnostic;
import persistence.tools.LintSeverity;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HibernateEntityModelLinterTest {

    public static class CleanEntity {
        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private CleanEntity parent;

        @OneToMany(fetch = FetchType.LAZY, mappedBy = "parent")
        private List<CleanEntity> children;

        @ManyToMany(fetch = FetchType.LAZY)
        private Set<CleanEntity> related;

        @ManyToMany(fetch = FetchType.LAZY)
        @OrderColumn
        private List<CleanEntity> orderedRelated;

        @OneToOne(fetch = FetchType.LAZY)
        private CleanEntity details;

        public CleanEntity() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    public static final class FinalClassEntity {
        @Id
        private Long id;

        public FinalClassEntity() {
        }
    }

    public static class NoNoArgConstructorEntity {
        @Id
        private Long id;

        public NoNoArgConstructorEntity(Long id) {
            this.id = id;
        }
    }

    public static class PrivateConstructorEntity {
        @Id
        private Long id;

        private PrivateConstructorEntity() {
        }
    }

    public static class FinalMethodEntity {
        @Id
        private Long id;

        public FinalMethodEntity() {
        }

        public final Long getId() {
            return id;
        }
    }

    public static class EagerOneToManyEntity {
        @Id
        private Long id;

        @OneToMany(fetch = FetchType.EAGER, mappedBy = "id")
        private List<CleanEntity> items;

        public EagerOneToManyEntity() {
        }
    }

    public static class MissingMappedByOneToManyEntity {
        @Id
        private Long id;

        @OneToMany(fetch = FetchType.LAZY)
        private List<CleanEntity> items;

        public MissingMappedByOneToManyEntity() {
        }
    }

    public static class EagerManyToManyEntity {
        @Id
        private Long id;

        @ManyToMany(fetch = FetchType.EAGER)
        private Set<CleanEntity> items;

        public EagerManyToManyEntity() {
        }
    }

    public static class UnorderedManyToManyListEntity {
        @Id
        private Long id;

        @ManyToMany(fetch = FetchType.LAZY)
        private List<CleanEntity> items;

        public UnorderedManyToManyListEntity() {
        }
    }

    public static class EagerManyToOneEntity {
        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.EAGER)
        private CleanEntity parent;

        public EagerManyToOneEntity() {
        }
    }

    public static class EagerOneToOneEntity {
        @Id
        private Long id;

        @OneToOne(fetch = FetchType.EAGER)
        private CleanEntity details;

        public EagerOneToOneEntity() {
        }
    }

    public static class MethodAnnotatedEntity {
        private Long id;
        private CleanEntity parent;

        public MethodAnnotatedEntity() {
        }

        @Id
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        @ManyToOne(fetch = FetchType.EAGER)
        public CleanEntity getParent() {
            return parent;
        }

        public void setParent(CleanEntity parent) {
            this.parent = parent;
        }
    }

    @Test
    void of_withNullEntityClassNames_throwsNullPointerException() {
        assertThatThrownBy(() -> HibernateEntityModelLinter.of(null, getClass().getClassLoader()))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void of_withNullClassLoader_throwsNullPointerException() {
        assertThatThrownBy(() -> HibernateEntityModelLinter.of(List.of("SomeClass"), null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void lint_withEmptyEntityList_returnsEmptyDiagnostics() {
        HibernateEntityModelLinter linter = HibernateEntityModelLinter.of(
            List.of(),
            getClass().getClassLoader()
        );

        List<LintDiagnostic> diagnostics = linter.lint();
        assertThat(diagnostics).isEmpty();
    }

    @Test
    void lint_withNonExistentClass_throwsIllegalArgumentException() {
        HibernateEntityModelLinter linter = HibernateEntityModelLinter.of(
            List.of("non.existent.Class"),
            getClass().getClassLoader()
        );

        assertThatThrownBy(linter::lint)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unable to load entity class");
    }

    @Test
    void lint_withCleanEntity_returnsNoDiagnostics() {
        HibernateEntityModelLinter linter = HibernateEntityModelLinter.of(
            List.of(CleanEntity.class.getName()),
            getClass().getClassLoader()
        );

        List<LintDiagnostic> diagnostics = linter.lint();
        assertThat(diagnostics).isEmpty();
    }

    @Test
    void lint_withFinalEntityClass_reportsJPA002() {
        HibernateEntityModelLinter linter = HibernateEntityModelLinter.of(
            List.of(FinalClassEntity.class.getName()),
            getClass().getClassLoader()
        );

        List<LintDiagnostic> diagnostics = linter.lint();
        assertThat(diagnostics).hasSize(1);
        LintDiagnostic diag = diagnostics.get(0);
        assertThat(diag.ruleId()).isEqualTo("JPA002");
        assertThat(diag.severity()).isEqualTo(LintSeverity.ERROR);
        assertThat(diag.className()).isEqualTo(FinalClassEntity.class.getName());
    }

    @Test
    void lint_withNoNoArgConstructor_reportsJPA003() {
        HibernateEntityModelLinter linter = HibernateEntityModelLinter.of(
            List.of(NoNoArgConstructorEntity.class.getName()),
            getClass().getClassLoader()
        );

        List<LintDiagnostic> diagnostics = linter.lint();
        assertThat(diagnostics).hasSize(1);
        LintDiagnostic diag = diagnostics.get(0);
        assertThat(diag.ruleId()).isEqualTo("JPA001");
        assertThat(diag.severity()).isEqualTo(LintSeverity.ERROR);
        assertThat(diag.className()).isEqualTo(NoNoArgConstructorEntity.class.getName());
    }

    @Test
    void lint_withPrivateConstructor_reportsJPA003() {
        HibernateEntityModelLinter linter = HibernateEntityModelLinter.of(
            List.of(PrivateConstructorEntity.class.getName()),
            getClass().getClassLoader()
        );

        List<LintDiagnostic> diagnostics = linter.lint();
        assertThat(diagnostics).hasSize(1);
        LintDiagnostic diag = diagnostics.get(0);
        assertThat(diag.ruleId()).isEqualTo("JPA001");
        assertThat(diag.severity()).isEqualTo(LintSeverity.ERROR);
        assertThat(diag.className()).isEqualTo(PrivateConstructorEntity.class.getName());
    }

    @Test
    void lint_withFinalPersistentMethod_reportsJPA004() {
        HibernateEntityModelLinter linter = HibernateEntityModelLinter.of(
            List.of(FinalMethodEntity.class.getName()),
            getClass().getClassLoader()
        );

        List<LintDiagnostic> diagnostics = linter.lint();
        assertThat(diagnostics).hasSize(1);
        LintDiagnostic diag = diagnostics.get(0);
        assertThat(diag.ruleId()).isEqualTo("JPA003");
        assertThat(diag.severity()).isEqualTo(LintSeverity.ERROR);
        assertThat(diag.className()).isEqualTo(FinalMethodEntity.class.getName());
        assertThat(diag.attributeName()).isEqualTo("getId");
    }

    @Test
    void lint_withEagerOneToMany_reportsJPA005() {
        HibernateEntityModelLinter linter = HibernateEntityModelLinter.of(
            List.of(EagerOneToManyEntity.class.getName()),
            getClass().getClassLoader()
        );

        List<LintDiagnostic> diagnostics = linter.lint();
        assertThat(diagnostics).hasSize(1);
        LintDiagnostic diag = diagnostics.get(0);
        assertThat(diag.ruleId()).isEqualTo("JPA004");
        assertThat(diag.severity()).isEqualTo(LintSeverity.WARNING);
        assertThat(diag.className()).isEqualTo(EagerOneToManyEntity.class.getName());
        assertThat(diag.attributeName()).isEqualTo("items");
    }

    @Test
    void lint_withMissingMappedByOneToMany_reportsJPA006() {
        HibernateEntityModelLinter linter = HibernateEntityModelLinter.of(
            List.of(MissingMappedByOneToManyEntity.class.getName()),
            getClass().getClassLoader()
        );

        List<LintDiagnostic> diagnostics = linter.lint();
        assertThat(diagnostics).hasSize(1);
        LintDiagnostic diag = diagnostics.get(0);
        assertThat(diag.ruleId()).isEqualTo("JPA006");
        assertThat(diag.severity()).isEqualTo(LintSeverity.WARNING);
        assertThat(diag.className()).isEqualTo(MissingMappedByOneToManyEntity.class.getName());
        assertThat(diag.attributeName()).isEqualTo("items");
    }

    @Test
    void lint_withEagerManyToMany_reportsJPA005() {
        HibernateEntityModelLinter linter = HibernateEntityModelLinter.of(
            List.of(EagerManyToManyEntity.class.getName()),
            getClass().getClassLoader()
        );

        List<LintDiagnostic> diagnostics = linter.lint();
        assertThat(diagnostics).hasSize(1);
        LintDiagnostic diag = diagnostics.get(0);
        assertThat(diag.ruleId()).isEqualTo("JPA004");
        assertThat(diag.severity()).isEqualTo(LintSeverity.WARNING);
        assertThat(diag.className()).isEqualTo(EagerManyToManyEntity.class.getName());
        assertThat(diag.attributeName()).isEqualTo("items");
    }

    @Test
    void lint_withUnorderedManyToManyList_reportsJPA007() {
        HibernateEntityModelLinter linter = HibernateEntityModelLinter.of(
            List.of(UnorderedManyToManyListEntity.class.getName()),
            getClass().getClassLoader()
        );

        List<LintDiagnostic> diagnostics = linter.lint();
        assertThat(diagnostics).hasSize(1);
        LintDiagnostic diag = diagnostics.get(0);
        assertThat(diag.ruleId()).isEqualTo("JPA007");
        assertThat(diag.severity()).isEqualTo(LintSeverity.WARNING);
        assertThat(diag.className()).isEqualTo(UnorderedManyToManyListEntity.class.getName());
        assertThat(diag.attributeName()).isEqualTo("items");
    }

    @Test
    void lint_withEagerManyToOne_reportsJPA005() {
        HibernateEntityModelLinter linter = HibernateEntityModelLinter.of(
            List.of(EagerManyToOneEntity.class.getName()),
            getClass().getClassLoader()
        );

        List<LintDiagnostic> diagnostics = linter.lint();
        assertThat(diagnostics).hasSize(1);
        LintDiagnostic diag = diagnostics.get(0);
        assertThat(diag.ruleId()).isEqualTo("JPA005");
        assertThat(diag.severity()).isEqualTo(LintSeverity.INFO);
        assertThat(diag.className()).isEqualTo(EagerManyToOneEntity.class.getName());
        assertThat(diag.attributeName()).isEqualTo("parent");
    }

    @Test
    void lint_withEagerOneToOne_reportsJPA005() {
        HibernateEntityModelLinter linter = HibernateEntityModelLinter.of(
            List.of(EagerOneToOneEntity.class.getName()),
            getClass().getClassLoader()
        );

        List<LintDiagnostic> diagnostics = linter.lint();
        assertThat(diagnostics).hasSize(1);
        LintDiagnostic diag = diagnostics.get(0);
        assertThat(diag.ruleId()).isEqualTo("JPA005");
        assertThat(diag.severity()).isEqualTo(LintSeverity.INFO);
        assertThat(diag.className()).isEqualTo(EagerOneToOneEntity.class.getName());
        assertThat(diag.attributeName()).isEqualTo("details");
    }

    @Test
    void lint_withMethodAnnotatedEntity_reportsJPA005() {
        HibernateEntityModelLinter linter = HibernateEntityModelLinter.of(
            List.of(MethodAnnotatedEntity.class.getName()),
            getClass().getClassLoader()
        );

        List<LintDiagnostic> diagnostics = linter.lint();
        assertThat(diagnostics).hasSize(1);
        LintDiagnostic diag = diagnostics.get(0);
        assertThat(diag.ruleId()).isEqualTo("JPA005");
        assertThat(diag.severity()).isEqualTo(LintSeverity.INFO);
        assertThat(diag.className()).isEqualTo(MethodAnnotatedEntity.class.getName());
        assertThat(diag.attributeName()).isEqualTo("getParent");
    }

}
