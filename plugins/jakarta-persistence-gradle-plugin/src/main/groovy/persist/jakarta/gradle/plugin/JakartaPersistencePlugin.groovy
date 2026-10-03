package persist.jakarta.gradle.plugin

import groovy.io.FileType
import groovy.xml.XmlParser
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentSelector
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.provider.Provider
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.language.jvm.tasks.ProcessResources
import persist.jakarta.gradle.extension.PersistenceExtension
import persist.jakarta.gradle.task.ProcessPersistenceDescriptor

/**
 * Gradle plugin that manages Jakarta Persistence (JPA) descriptor generation.
 * <p>
 * Registered with plugin id {@code io.github.jpersist.jpa}, this plugin:
 * </p>
 * <ol>
 *     <li>Applies the {@link org.gradle.api.plugins.JavaPlugin}.</li>
 *     <li>Creates a {@code jpa} dependency configuration for propagating
 *         platform/BOM version constraints into standard Java configurations
 *         ({@code implementation}, {@code compileOnly}, {@code annotationProcessor},
 *         {@code runtimeOnly}). When the {@link org.gradle.api.plugins.JavaLibraryPlugin}
 *         is applied, the {@code api} and {@code compileOnlyApi} configurations are
 *         also extended from {@code jpa}.</li>
 *     <li>Registers the {@code persistence}
 *         {@link org.gradle.api.NamedDomainObjectContainer} of
 *         {@link persist.jakarta.gradle.extension.PersistenceExtension} as the
 *         top-level DSL extension.</li>
 *     <li>For each Java source set (e.g.&nbsp;{@code main}, {@code test}):
 *         <ul>
 *             <li>Creates a {@code jarFile} (or {@code <sourceSet>JarFile})
 *                 dependency configuration for declaring module JARs to be
 *                 injected as {@code <jar-file>} entries. The {@code jarFile}
 *                 configuration extends {@code implementation} (and {@code api}
 *                 when the {@code java-library} plugin is present).</li>
 *             <li>Automatically initializes a
 *                 {@link persist.jakarta.gradle.extension.PersistenceExtension}
 *                 instance in the container, keyed by the source set name.</li>
 *             <li>Automatically parses any existing {@code persistence.xml}
 *                 template in the source set's resources and pre-registers
 *                 the {@code <persistence-unit>} names into the extension,
 *                 so they can be configured from the build script without
 *                 explicit re-declaration.</li>
 *             <li>Registers a {@code processPersistenceDescriptor} (or
 *                 {@code process<SourceSet>PersistenceDescriptor})
 *                 {@link ProcessPersistenceDescriptor} task that generates or
 *                 merges the final {@code persistence.xml}.</li>
 *             <li>Registers the generated resources directory into the source set
 *                 so that IDEs such as Eclipse Buildship and IntelliJ can index
 *                 the directory and resolve {@code META-INF/persistence.xml}
 *                 instantly.</li>
 *             <li>Wires the task output into the source set's
 *                 {@code processResources} so the generated descriptor ends up
 *                 in the JAR.</li>
 *         </ul>
 *     </li>
 * </ol>
 * <p>
 * Individual persistence units can be skipped by setting their
 * {@link persist.jakarta.gradle.extension.PersistenceUnitExtension#getEnabled() enabled}
 * property to {@code false}. The generated XML output can be customized via
 * the {@link persist.jakarta.gradle.extension.PersistenceExtension#getOutputProperties() outputProperties}
 * map or the {@code transformer} DSL block.
 * </p>
 * <p>
 * Persistence units already declared in a user-provided {@code persistence.xml}
 * template are automatically registered into the extension at configuration time,
 * allowing build scripts to customise them (e.g.&nbsp;disable or override properties)
 * without re-declaring them.
 * </p>
 * <p>
 * This plugin replaces the deprecated {@code persistence-gradle-plugin}
 * ({@code io.github.jpersist.persistence}).
 * </p>
 *
 * @since 1.1.0
 */
class JakartaPersistencePlugin implements Plugin<Project> {

    // Define the custom attribute key for jar files location routing
    static final Attribute<String> JAR_LOCATION_ATTRIBUTE = Attribute.of('io.github.jpersist.jpa.location', String)

    @Override
    void apply(Project project) {
        project.plugins.apply(JavaPlugin)

        def jpaProvider = project.configurations.register('jpa') { config ->
            config.canBeConsumed = false
            config.canBeResolved = false
        }

        // 1. Create a NamedDomainObjectContainer using Gradle's ObjectFactory
        def container = project.objects.domainObjectContainer(PersistenceExtension)

        // 2. Expose the container as the top-level 'persistence' extension block
        project.extensions.add('persistence', container)

        project.extensions.getByType(SourceSetContainer).configureEach { sourceSet ->
            // 1. Create native dependency configuration
            def jarFileConfigName = sourceSet.name == 'main' ? "jarFile" : "${sourceSet.name}JarFile"
            def jarFileConfigProvider = project.configurations.register(jarFileConfigName) { config ->
                config.canBeConsumed = false
                config.canBeResolved = true
            }

            // 2. Register Extension API DSL
            // Automatically initialize a configuration instance inside the container matching the source set name
            // (e.g., this instantly builds 'persistence.main' and 'persistence.test')
            def extension = container.maybeCreate(sourceSet.name)

            // Auto-register persistence units already declared in the user's persistence.xml file
            autoRegisterExistingUnits(project, sourceSet, extension)

            // Propagate platform version constraints into all standard Java configurations
            extendProjectConfigurations(project, jpaProvider,
                sourceSet.implementationConfigurationName,
                sourceSet.compileOnlyConfigurationName,
                sourceSet.annotationProcessorConfigurationName,
                sourceSet.runtimeOnlyConfigurationName
            )

            extendProjectConfigurations(project, jarFileConfigProvider, sourceSet.implementationConfigurationName)

            project.plugins.withType(JavaLibraryPlugin).configureEach {
                extendProjectConfigurations(project, jpaProvider,
                    sourceSet.apiConfigurationName,
                    sourceSet.compileOnlyApiConfigurationName
                )

                extendProjectConfigurations(project, jarFileConfigProvider, sourceSet.apiConfigurationName)
            }

            // 3. Register standard descriptors processor task
            def processTaskName = sourceSet.name == 'main' ? "processPersistenceDescriptor" : "process${sourceSet.name.capitalize()}PersistenceDescriptor"
            def processTask = project.tasks.register(processTaskName, ProcessPersistenceDescriptor) { task ->
                task.xmlVersion.set(extension.version)

                // Connect transformer properties configuration securely
                task.transformerSettings.set(extension.outputProperties)

                // Lazily filter out disabled units or clear out the list if the root extension is disabled
                task.units.set(project.provider {
                    extension.persistenceUnits.matching { it.enabled.getOrElse(true)} as List
                })

                // Natively skip task execution when the units collection list evaluates to empty
                task.onlyIf {
                    // Safe lazy getter call evaluated exactly at execution time
                    !task.units.get().isEmpty()
                }

                // Dynamically fetch annotated class names lazily at execution time
                task.includedClasses.set(project.provider {
                    discoverJpaClasses(sourceSet)
                })

                // Resolve first level declared jars
                task.jarFileNames.set(project.provider {
                    def jarFileConfig = jarFileConfigProvider.get()
                    if (jarFileConfig.isEmpty()) return []

                    // 1. Build a robust path map using the unified component selection graph
                    Map<String, String> dependencyLocationOverrides = [:]

                    // ResolutionResult captures both Project and Module attributes accurately
                    jarFileConfig.incoming.resolutionResult.allDependencies.each { depResult ->
                        if (depResult instanceof DependencyResult) {
                            def requested = depResult.requested

                            // Extract the attribute directly from the requested builder notation metadata
                            String declaredLocation = requested.attributes.getAttribute(JAR_LOCATION_ATTRIBUTE)

                            if (declaredLocation) {
                                if (requested instanceof ProjectComponentSelector) {
                                    // Extract via requested.projectPath and strip the leading colons to get the pure module name
                                    String path = requested.projectPath
                                    String cleanProjectName = path.contains(':') ? path.substring(path.lastIndexOf(':') + 1) : path
                                    dependencyLocationOverrides.put(cleanProjectName, declaredLocation)
                                } else if (requested instanceof ModuleComponentSelector) {
                                    // Key format for external groups: "group:name"
                                    String artifactId = "${requested.group}:${requested.module}"
                                    dependencyLocationOverrides.put(artifactId, declaredLocation)
                                }
                            }
                        }
                    }

                    // 2. Loop over resolved artifacts and apply matching path configurations
                    jarFileConfig.resolvedConfiguration.firstLevelModuleDependencies.collectMany { dep ->
                        dep.moduleArtifacts.collect { artifact ->
                            String jarName = artifact.file.name

                            // Match using full coordinate string first, then fall back to the simple submodule name
                            String customLocation = dependencyLocationOverrides.get("${dep.moduleGroup}:${dep.moduleName}")
                                ?: dependencyLocationOverrides.get(dep.moduleName)

                            if (customLocation && !customLocation.trim().isEmpty()) {
                                // Normalize trailing slashes elegantly
                                String cleanLocation = customLocation.endsWith('/') ? customLocation : "${customLocation}/"
                                return "${cleanLocation}${jarName}"
                            }

                            return jarName // Default fallback path if no layout attribute was specified
                        }
                    }
                })

                // Safely hook into the sibling compilation task output lazily using Provider map arrays
                def compileJavaTaskProvider = project.tasks.named(sourceSet.compileJavaTaskName)

                // Explicitly enforce that compilation completes BEFORE this task runs
                task.mustRunAfter(compileJavaTaskProvider)

                // Natively bind compilation outputs as inputs to trigger accurate incremental build caching
                task.inputs.files(compileJavaTaskProvider.map { it.outputs.files })

                // Safe input lookup tracking direct source files — only set when the file exists
                // so that @Optional @InputFile allows the task to run in generate-from-scratch mode
                def sourcePath = "src/${sourceSet.name}/resources/META-INF/persistence.xml"
                def sourceFile = project.file(sourcePath)
                if (sourceFile.exists()) {
                    task.persistenceXml.set(project.layout.projectDirectory.file(sourcePath))
                }

                // Direct output tracking safely to resources destination
                task.destinationFile.set(project.layout.buildDirectory.file("generated/resources/${sourceSet.name}/META-INF/persistence.xml"))

                // Register the root of our generated resources folder into the Gradle SourceSet.
                // This tells Eclipse Buildship and IntelliJ to index the directory and resolve META-INF/persistence.xml instantly.
                sourceSet.resources.srcDir(project.layout.buildDirectory.dir("generated/resources/${sourceSet.name}"))
            }

            // 4. Feed output securely back to resource processor as an input source!
            project.tasks.named(sourceSet.processResourcesTaskName, ProcessResources).configure { resourceTask ->
                // Prevent duplicate/conflict matching by excluding the un-patched original file
                resourceTask.exclude("META-INF/persistence.xml")

                // Place the generated file into META-INF/ within the resources output
                resourceTask.from(processTask.flatMap { it.destinationFile }) {
                    into("META-INF")
                }
            }
        }
    }

    /**
     * Parses the source {@code persistence.xml} (if it exists) and automatically
     * pre-populates the {@link PersistenceExtension#getPersistenceUnits() persistenceUnits}
     * container with the {@code <persistence-unit>} names found inside.
     * <p>
     * This allows users to configure (e.g.&nbsp;enable/disable, override properties)
     * persistence units that are already declared in their template descriptor
     * without having to re-declare them in the build script. A non-validating,
     * namespace-unaware parse is used so that temporarily malformed XML does not
     * break Gradle configuration sync.
     * </p>
     *
     * @param project   The Gradle project used to resolve the source file path.
     * @param sourceSet The source set whose resources directory is inspected.
     * @param extension The persistence extension whose unit container is populated.
     */
    private static void autoRegisterExistingUnits(Project project, SourceSet sourceSet, PersistenceExtension extension) {
        def sourcePath = "src/${sourceSet.name}/resources/META-INF/persistence.xml"
        def sourceFile = project.file(sourcePath)

        if (sourceFile.exists()) {
            try {
                // Defensive, non-validating parse just to extract the names block during configuration phase
                def xmlParser = new XmlParser(false, false)
                def rootNode = xmlParser.parse(sourceFile)

                // Extract the root version attribute from the <persistence> tag
                String xmlVersionAttr = (String) rootNode.attribute("version")
                if (xmlVersionAttr && !xmlVersionAttr.trim().isEmpty()) {
                    // Update the extension's version convention to match the physical XML version seamlessly
                    extension.version.convention(xmlVersionAttr.trim())
                }

                // Process the unit elements as before
                List<?> unitNodes = (List<?>) rootNode.get("persistence-unit")
                unitNodes.each { Object unitObj ->
                    String unitName = (String) ((Node) unitObj).attribute("name")
                    if (unitName && !unitName.trim().isEmpty()) {
                        // Automatically register the unit in the extension container if it isn't already there
                        extension.persistenceUnits.maybeCreate(unitName)
                    }
                }
            } catch (Exception ignored) {
                // Fail-safe skip to avoid crashing Gradle configuration sync if the user's working XML is temporarily malformed
            }
        }
    }

    /**
     * Scans the output classes directories of a SourceSet and returns fully qualified class names
     * containing targeted Jakarta Persistence annotations.
     */
    private static List<String> discoverJpaClasses(SourceSet sourceSet) {
        Set<String> targetAnnotations = [
            'Ljakarta/persistence/Entity;',
            'Ljakarta/persistence/Embeddable;',
            'Ljakarta/persistence/MappedSuperclass;',
            'Ljakarta/persistence/Converter;'
        ] as Set<String>

        List<String> jpaClasses = []

        sourceSet.output.classesDirs.each { File classesDir ->
            if (!classesDir.exists()) return

            classesDir.traverse(type: FileType.FILES, nameFilter: ~/.*\.class$/) { File classFile ->
                classFile.withInputStream { InputStream is ->
                    try {
                        ClassReader reader = new ClassReader(is)
                        boolean hasJpaAnnotation = false

                        // ASM custom structure lookup loop
                        reader.accept(new ClassVisitor(Opcodes.ASM9) {

                            @Override
                            AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                                if (targetAnnotations.contains(descriptor)) {
                                    hasJpaAnnotation = true
                                }
                                return null
                            }

                        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES)

                        if (hasJpaAnnotation) {
                            // Convert internal bytecode name (org/example/MyClass) to fully qualified binary name
                            jpaClasses.add(Type.getObjectType(reader.className).className)
                        }
                    } catch (Exception ignored) {
                        // Fail-safe skip for corrupt or unreadable class binaries
                    }
                }
            }
        }

        return jpaClasses.sort()
    }

    /**
     * Makes each of the named configurations extend from the given parent
     * configuration, so that dependency constraints declared in the parent
     * propagate automatically.
     * <p>
     * Only configurations that are actually registered in the project are
     * matched, making this safe to call with names that may not yet exist
     * (e.g.&nbsp;{@code api} before the {@code java-library} plugin is applied).
     * </p>
     *
     * @param project The Gradle project whose configurations are extended.
     * @param parent  A lazy provider for the parent configuration.
     * @param names   The names of the configurations to extend.
     */
    private static void extendProjectConfigurations(Project project, Provider<Configuration> parent, String... names) {
        Set<String> targetConfigNames = names as Set
        // Safely match only the configurations that are actually registered
        project.configurations.matching { config ->
            targetConfigNames.contains(config.name)
        }.configureEach { config ->
            config.extendsFrom(parent.get())
        }
    }

}
