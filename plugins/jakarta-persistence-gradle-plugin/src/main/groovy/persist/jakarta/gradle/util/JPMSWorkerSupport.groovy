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

package persist.jakarta.gradle.util

import org.gradle.workers.ProcessWorkerSpec
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor

import java.lang.module.ModuleDescriptor
import java.nio.ByteBuffer
import java.nio.file.Files

/**
 * Utility class providing hybrid execution isolation for Gradle workers.
 * <p>
 * Automatically detects whether the target classes directory defines a JPMS module
 * ({@code module-info.class}). If modular, submits work via {@link WorkerExecutor#processIsolation}
 * configured with {@code --module-path}, {@code --add-modules}, and dynamic {@code --add-opens}
 * flags for Hibernate reflective access. Otherwise, falls back to fast {@link WorkerExecutor#classLoaderIsolation}.
 * </p>
 *
 * @since 1.9.0
 */
final class JPMSWorkerSupport {

    private static final String HIBERNATE_MODULES = "io.github.jpersist.tools.hibernate.orm,io.github.jpersist.tools.hibernate"

    private JPMSWorkerSupport() {}

    /**
     * Checks whether the compiled classes directory contains a JPMS {@code module-info.class} descriptor.
     */
    static boolean isModularProject(File classesDir) {
        return classesDir != null && new File(classesDir, "module-info.class").exists()
    }

    /**
     * Creates a configured {@link WorkQueue} using {@code processIsolation} for JPMS modules
     * or {@code classLoaderIsolation} for standard classpath projects.
     *
     * @param workerExecutor    The Gradle worker executor service.
     * @param workerClass       The {@link org.gradle.workers.WorkAction} implementation class
     * @param classesDir        The compiled classes directory of the target source set.
     * @param modularFiles      All JARs and directories belonging to the execution module-path/classpath.
     * @param extraClasspath    Additional non-modular directories (e.g. processed resources) to append.
     * @return A ready-to-submit {@link WorkQueue}.
     */
    static WorkQueue createWorkQueue(WorkerExecutor workerExecutor,
                                     Class<?> workerClass,
                                     File classesDir,
                                     Set<File> modularFiles,
                                     File... extraClasspath) {
        if (isModularProject(classesDir)) {
            return workerExecutor.processIsolation { ProcessWorkerSpec spec ->
                // Locate the plugin JAR containing worker class so the forked JVM can load it
                Set<File> workerBridgeFiles = resolveWorkerBridgeFiles(workerClass)

                // Keep Gradle worker action classes & parameters on the bootstrap classpath
                spec.classpath.from(workerBridgeFiles)
                if (extraClasspath) {
                    spec.classpath.from(extraClasspath)
                }

                List<File> modulePathEntries = new ArrayList<>(modularFiles)
                if (!modulePathEntries.contains(classesDir)) {
                    modulePathEntries.add(classesDir)
                }

                String modulePath = modulePathEntries.collect { it.absolutePath }.join(File.pathSeparator)

                List<String> jvmArgs = [
                    '--module-path', modulePath,
                    '--add-modules', 'ALL-MODULE-PATH',
                    '--add-opens', "java.base/java.lang=${HIBERNATE_MODULES}".toString(),
                    '--add-opens', "java.base/java.util=${HIBERNATE_MODULES}".toString(),
                    '--add-opens', "java.base/java.time=${HIBERNATE_MODULES}".toString()
                ]

                // Automatically open all packages of the user's named module to Hibernate providers
                appendDynamicUserModuleOpens(new File(classesDir, "module-info.class"), jvmArgs)

                spec.forkOptions { fork ->
                    fork.jvmArgs(jvmArgs)
                }
            }
        }

        // Standard classpath execution (no JVM fork overhead)
        return workerExecutor.classLoaderIsolation { spec ->
            spec.classpath.from(modularFiles)
            if (extraClasspath) {
                spec.classpath.from(extraClasspath)
            }
        }
    }

    /**
     * Parses {@code module-info.class} using standard {@link ModuleDescriptor} APIs and appends
     * {@code --add-opens <moduleName>/<pkg>=<hibernateModules>} for every package in the module.
     */
    private static void appendDynamicUserModuleOpens(File moduleInfoFile, List<String> jvmArgs) {
        try {
            byte[] bytes = Files.readAllBytes(moduleInfoFile.toPath())
            ModuleDescriptor descriptor = ModuleDescriptor.read(ByteBuffer.wrap(bytes))
            String moduleName = descriptor.name()

            descriptor.packages().each { String pkg ->
                jvmArgs.add('--add-opens')
                jvmArgs.add("${moduleName}/${pkg}=${HIBERNATE_MODULES}".toString())
            }
        } catch (Exception ignored) {
            // Safe fallback if module-info.class cannot be parsed
        }
    }

    private static Set<File> resolveWorkerBridgeFiles(Class<?> workerClass) {
        def location = workerClass.protectionDomain?.codeSource?.location
        return (location != null ? [new File(location.toURI())]: []) as Set<File>
    }

}
