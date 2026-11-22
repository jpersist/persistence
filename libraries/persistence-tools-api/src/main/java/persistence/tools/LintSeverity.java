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

package persistence.tools;

/**
 * Enumerates the severity levels assigned to {@link LintDiagnostic} findings
 * reported by an {@link EntityModelLinter}.
 *
 * <p>Build tools and plugins can use these levels to determine whether to fail
 * the build or emit console warnings.</p>
 *
 * @since 4.2.0
 * @see LintDiagnostic
 * @see EntityModelLinter
 */
public enum LintSeverity {

    /**
     * Critical mapping error or severe anti-pattern that will likely cause runtime
     * failures or major performance degradation.
     */
    ERROR,

    /**
     * Suboptimal mapping or potential performance pitfall that should be reviewed.
     */
    WARNING,

    /**
     * Informational suggestion or minor best-practice recommendation.
     */
    INFO

}
