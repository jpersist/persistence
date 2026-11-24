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

import org.junit.jupiter.api.Test;
import persistence.tools.LintDiagnostic;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractEntityModelLinterTest {

    @Test
    void shouldPassValidEntityAndInvokeProviderHook() {
        AtomicBoolean hookCalled = new AtomicBoolean(false);
        TestEntityModelLinter linter = new TestEntityModelLinter(
            List.of(ValidEntity.class.getName()),
            getClass().getClassLoader(),
            (entityClass, diagnostics) -> hookCalled.set(true)
        );

        List<LintDiagnostic> diagnostics = linter.lint();

        assertThat(diagnostics).isEmpty();
        assertThat(hookCalled).isTrue();
        assertThat(linter.getEntityClassNames()).containsExactly(ValidEntity.class.getName());
        assertThat(linter.getClassLoader()).isSameAs(getClass().getClassLoader());
    }

    @Test
    void shouldDetectFinalClassAndMissingPublicOrProtectedNoArgConstructor() {
        TestEntityModelLinter linter = new TestEntityModelLinter(
            List.of(FinalNoArgMissingEntity.class.getName()),
            getClass().getClassLoader(),
            (entityClass, diagnostics) -> {}
        );

        assertThat(linter.lint())
            .extracting(LintDiagnostic::ruleId)
            .containsExactlyInAnyOrder("JPA001", "JPA002");
    }

    @Test
    void shouldDetectPrivateNoArgConstructorAsInvalid() {
        TestEntityModelLinter linter = new TestEntityModelLinter(
            List.of(PrivateConstructorEntity.class.getName()),
            getClass().getClassLoader(),
            (entityClass, diagnostics) -> {}
        );

        assertThat(linter.lint())
            .singleElement()
            .extracting(LintDiagnostic::ruleId)
            .isEqualTo("JPA001");
    }

    @Test
    void shouldDetectFinalNonStaticNonPrivateMethodOnly() {
        TestEntityModelLinter linter = new TestEntityModelLinter(
            List.of(FinalMethodEntity.class.getName()),
            getClass().getClassLoader(),
            (entityClass, diagnostics) -> {}
        );

        assertThat(linter.lint())
            .singleElement()
            .satisfies(d -> {
                assertThat(d.ruleId()).isEqualTo("JPA003");
                assertThat(d.attributeName()).isEqualTo("getName");
            });
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenEntityClassNotFound() {
        TestEntityModelLinter linter = new TestEntityModelLinter(
            List.of("com.example.NonExistentEntity"),
            getClass().getClassLoader(),
            (entityClass, diagnostics) -> {}
        );

        assertThatThrownBy(linter::lint)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("com.example.NonExistentEntity");
    }

    // --- Sample Test Fixtures ---

    public static class ValidEntity {
        protected ValidEntity() {
        }

        public String getId() {
            return "1";
        }
    }

    public static final class FinalNoArgMissingEntity {
        public FinalNoArgMissingEntity(String id) {
        }
    }

    public static class PrivateConstructorEntity {
        private PrivateConstructorEntity() {
        }
    }

    public static class FinalMethodEntity {
        public FinalMethodEntity() {
        }

        public final String getName() {
            return "test";
        }

        public static final String staticHelper() {
            return "ignored";
        }

        private final String privateHelper() {
            return "ignored";
        }
    }

    @FunctionalInterface
    interface ProviderHook {
        void execute(Class<?> entityClass, List<LintDiagnostic> diagnostics);
    }

    static class TestEntityModelLinter extends AbstractEntityModelLinter {

        private final ProviderHook hook;

        TestEntityModelLinter(List<String> entityClassNames, ClassLoader classLoader, ProviderHook hook) {
            super(entityClassNames, classLoader);
            this.hook = hook;
        }

        @Override
        protected void lintEntityModel(Class<?> entityClass, List<LintDiagnostic> diagnostics) {
            hook.execute(entityClass, diagnostics);
        }
    }

}
