package persist.jakarta.gradle.plugin

import groovy.io.FileType
import groovy.xml.XmlParser
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentSelector
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.provider.Provider
import org.gradle.language.base.plugins.LifecycleBasePlugin
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
import persist.jakarta.gradle.task.GenerateJPAGraalVMMetadata
import persist.jakarta.gradle.task.ProcessPersistenceDescriptor
import persist.jakarta.gradle.task.ValidatePersistenceSchema

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
 *             <li>Registers a {@code validatePersistenceSchema} (or
 *                 {@code validate<SourceSet>PersistenceSchema})
 *                 {@link ValidatePersistenceSchema} task that validates entity
 *                 mappings against the database schema using an isolated worker
 *                 process.</li>
 *             <li>Registers a {@code generateJPAGraalVMMetadata} (or
 *                 {@code generate<SourceSet>JPAGraalVMMetadata})
 *                 {@link GenerateJPAGraalVMMetadata} task that produces a
 *                 GraalVM {@code reflect-config.json} for all discovered JPA
 *                 entity classes.</li>
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

            // Create a single, shared memorized lazy data provider for this source set
            def discoveredClassesProvider = project.provider {
                discoverJpaClasses(sourceSet)
            }

            configureProcessPersistenceDescriptorTask(project, sourceSet, extension, discoveredClassesProvider, jarFileConfigProvider)

            configureGenerateJPAGraalVMMetadataTask(project, sourceSet, discoveredClassesProvider)

            configureValidatePersistenceSchemaTask(project, sourceSet, extension)
        }
    }

    private static void configureProcessPersistenceDescriptorTask(Project project, SourceSet sourceSet, PersistenceExtension extension,
                                                                  Provider<List<String>> discoveredClassesProvider,
                                                                  NamedDomainObjectProvider<Configuration> jarFileConfigProvider) {
        // 3. Register standard descriptors processor task
        def processTaskName = sourceSet.name == 'main' ? "processPersistenceDescriptor" : "process${sourceSet.name.capitalize()}PersistenceDescriptor"
        def processTask = project.tasks.register(processTaskName, ProcessPersistenceDescriptor) { task ->
            task.group = LifecycleBasePlugin.BUILD_GROUP
            task.description = "Merges or generates from scratch the persistence.xml for the ${sourceSet.name} source set."

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
            task.includedClasses.set(discoveredClassesProvider)

            // Resolve first level declared jars
            task.jarFileNames.set(project.provider {
                resolveFirstLevelJarFileNames(jarFileConfigProvider, task)
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
        project.tasks.named(sourceSet.processResourcesTaskName, ProcessResources).configure { resourcesTask ->
            // Prevent duplicate/conflict matching by excluding the un-patched original file
            resourcesTask.exclude("META-INF/persistence.xml")

            // Place the generated file into META-INF/ within the resources output
            resourcesTask.from(processTask.flatMap { it.destinationFile }) {
                into("META-INF")
            }
        }
    }

    private static void configureGenerateJPAGraalVMMetadataTask(Project project, SourceSet sourceSet, Provider<List<String>> discoveredClassesProvider) {
        // 1. Register GraalVM Native Image Metadata Generation Task
        def graalVMTaskName = sourceSet.name == 'main' ? "generateJPAGraalVMMetadata" : "generate${sourceSet.name.capitalize()}JPAGraalVMMetadata"

        def graalVMTaskProvider = project.tasks.register(graalVMTaskName, GenerateJPAGraalVMMetadata) { task ->
            task.group = LifecycleBasePlugin.BUILD_GROUP
            task.description = "Generates GraalVM reflect-config.json metadata for source set ${sourceSet.name}"

            // Natively hook into your existing high-speed lazy ASM bytecode scanner!
            task.includedClasses.set(discoveredClassesProvider)

            // Use the group and name parameters to isolate the target path perfectly following GraalVM spec standards
            String groupPath = project.group ? project.group.toString().replace('.', '/') : 'unspecified'
            String targetPath = "generated/graalvm-resources/${sourceSet.name}/META-INF/native-image/${groupPath}/${project.name}/reflect-config.json"

            task.outputFile.set(project.layout.buildDirectory.file(targetPath))
        }

        // 2. Wire the generated GraalVM output directory straight back as an active resource path!
        // This ensures the JSON is automatically indexed by IDEs and packaged into the final JAR distribution.
        project.layout.buildDirectory.dir("generated/graalvm-resources/${sourceSet.name}").tap { generatedGraalVMDir ->
            sourceSet.resources.srcDir(generatedGraalVMDir)
        }

        // Ensure our sibling processing resource task executes in the correct logical sequence
        project.tasks.named(sourceSet.processResourcesTaskName, ProcessResources).configure { resourceTask ->
            resourceTask.dependsOn(graalVMTaskProvider)
        }
    }

    private static void configureValidatePersistenceSchemaTask(Project project, SourceSet sourceSet, PersistenceExtension extension) {
        def processTaskName = sourceSet.name == 'main' ? "processPersistenceDescriptor" : "process${sourceSet.name.capitalize()}PersistenceDescriptor"
        def processTask = project.tasks.named(processTaskName, ProcessPersistenceDescriptor)

        // Register a single base validation driver task per SourceSet
        def validationTaskName = sourceSet.name == 'main' ? "validatePersistenceSchema" : "validate${sourceSet.name.capitalize()}PersistenceSchema"

        extension.validation { validation ->
            validation.url.convention("jdbc:h2:mem:schema_validate_db;DB_CLOSE_DELAY=-1")
            validation.driver.convention("org.h2.Driver")
            validation.user.convention("sa")
            validation.password.convention("")
        }

        project.tasks.register(validationTaskName, ValidatePersistenceSchema) { task ->
            task.group = LifecycleBasePlugin.VERIFICATION_GROUP
            task.description = "Validates the JPA schema alignment for all active units in the ${sourceSet.name} source set against an in-memory database snapshot."

            // Lazily pull the active names list from the processor task at execution time
            task.persistenceUnitNames.set(project.provider {
                // Collect names of all enabled units managed by the processor task
                List<String> unitNames = processTask.get().units.get().collect { it.name }
                // Join names with a comma to pass them into our main worker process safely
                return unitNames.join(',')
            })

            // Lazily put validation extension to the validation task
            task.validation.set(extension.validation)

            // Natively skip task execution when the unit names string payload evaluates to empty
            // This prevents the task from running if no active units are declared or enabled!
            task.onlyIf {
                String units = task.persistenceUnitNames.getOrElse("")
                return !units.trim().isEmpty()
            }

            // FIX: Enforce an explicit dependency rule on the processResources task instance.
            // This guarantees the directory is physically present before validation triggers!
            def processResourcesTask = project.tasks.named(sourceSet.processResourcesTaskName, ProcessResources)
            task.getResourcesDir().set(processResourcesTask.flatMap { it.destinationDirectory })
            task.dependsOn(processResourcesTask)

            // 1. Add pure compiled java entity classes directories ONLY (No resources included)
            task.getClasspath().from(sourceSet.output.classesDirs)

            // 2. Add external framework dependencies from compileClasspath configuration
            // compileClasspath contains Hibernate/EclipseLink but does NOT track processResources outputs!
            task.getClasspath().from(project.configurations.named(sourceSet.compileClasspathConfigurationName))

            // 3. Add the Plugin's own compiled files directory so SchemaValidationWorker can be found
            // This safely uses the class protection domain location which we verified is stable
            def pluginLocation = JakartaPersistencePlugin.class.protectionDomain.codeSource?.location
            if (pluginLocation != null) {
                task.getClasspath().from(project.files(pluginLocation))
            } else {
                task.getClasspath().from(project.files(JakartaPersistencePlugin.class.classLoader.getResource(".")).builtBy())
            }

            // 4. Transparently inject the H2 driver file on-the-fly for the user
            task.getClasspath().from(project.files(project.buildscript.configurations.detachedConfiguration(
                project.dependencies.create("com.h2database:h2:2.2.224")
            )))
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
        def sourceFile = project.file("src/${sourceSet.name}/resources/META-INF/persistence.xml")

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

    private static List<String> resolveFirstLevelJarFileNames(NamedDomainObjectProvider<Configuration> jarFileConfigProvider,
                                                              ProcessPersistenceDescriptor processTask) {
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

        // Aggregate unique mappings across all active persistence units configured for the task
        Map<String, String> customJarFileMappings = [:]
        processTask.units.get().each { unit ->
            customJarFileMappings.putAll(unit.jarFileMappings.getOrElse([:]))
        }

        // 2. Loop over resolved artifacts and apply matching path configurations
        jarFileConfig.resolvedConfiguration.firstLevelModuleDependencies.collectMany { dep ->
            dep.moduleArtifacts.collect { artifact ->
                String jarName = artifact.file.name

                // Determine the clean project path coordinate if it is a local module dependency
                String projectPath = artifact.id.componentIdentifier instanceof ProjectComponentIdentifier
                    ? ((ProjectComponentIdentifier) artifact.id.componentIdentifier).projectPath
                    : null

                // 1. HIGHEST PRIORITY: Check for a direct explicit mapping override in the new DSL map
                String directOverride = customJarFileMappings.get(projectPath)
                    ?: customJarFileMappings.get("${dep.moduleGroup}:${dep.moduleName}")

                if (directOverride) {
                    return directOverride
                }

                // 2. FALLBACK: Use standard Namespaced Location Attribute routing from v1.4.1
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
