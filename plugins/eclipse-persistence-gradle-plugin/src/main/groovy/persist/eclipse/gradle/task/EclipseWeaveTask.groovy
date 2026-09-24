package persist.eclipse.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations

import javax.inject.Inject

abstract class EclipseWeaveTask extends DefaultTask {

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getResourcesDir()

    // Source directory holds clean compiled files (Read-Only Input)
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    @SkipWhenEmpty
    abstract DirectoryProperty getSourceClassesDir()

    // Target directory holds newly generated woven outputs
    @OutputDirectory
    abstract DirectoryProperty getTargetClassesDir()

    @CompileClasspath
    abstract ConfigurableFileCollection getWeaveClasspath()

    @CompileClasspath
    abstract ConfigurableFileCollection getCompileClasspath()

    private ExecOperations execOperations

    // Inject ExecOperations directly into the task via Gradle's dependency injection
    @Inject
    EclipseWeaveTask(ExecOperations execOperations) {
        this.execOperations = execOperations
    }

    @TaskAction
    void weave() {
        def resourcesPath = getResourcesDir().get().asFile.absolutePath
        def sourcePath = getSourceClassesDir().get().asFile.absolutePath
        def targetPath = getTargetClassesDir().get().asFile.absolutePath

        execOperations.javaexec { spec ->
            spec.mainClass.set('org.eclipse.persistence.tools.weaving.jpa.StaticWeave')
            spec.classpath = getWeaveClasspath()
            spec.args(
                '-persistenceinfo', resourcesPath,
                '-classpath', getCompileClasspath().asPath,
                '-loglevel', 'FINE',
                sourcePath,
                targetPath
            )
        }
    }

}
