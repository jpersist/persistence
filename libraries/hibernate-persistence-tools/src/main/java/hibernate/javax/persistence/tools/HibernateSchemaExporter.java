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

import hibernate.base.persistence.tools.HibernateUtil;
import hibernate.javax.persistence.tools.internal.NoOpConnectionProvider;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerHaltImpl;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToWriter;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import persistence.tools.SchemaExporter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Hibernate&nbsp;5 implementation of {@link SchemaExporter} that generates offline DDL
 * scripts ({@code CREATE}, {@code DROP}, or {@code BOTH}) from legacy Java Persistence
 * entity metadata without requiring an active database connection.
 *
 * <p>Uses Hibernate&nbsp;5 {@link SchemaManagementTool} with a custom {@link ScriptTargetOutput}
 * targeting {@link File}, {@link OutputStream}, or {@link Writer} destinations. Predefined default
 * settings provide offline dialect and formatting options, while connection invariants guarantee
 * that live database interactions remain disabled.</p>
 *
 * @since 1.2.0
 * @see SchemaExporter
 * @see hibernate.javax.persistence.tools.provider.HibernateSchemaExporterFactory
 */
public class HibernateSchemaExporter implements SchemaExporter {

    private static final Map<String, Object> DEFAULT_SETTINGS = Map.of(
        AvailableSettings.HBM2DDL_DATABASE_ACTION, "none",
        AvailableSettings.DIALECT, "org.hibernate.dialect.H2Dialect",
        AvailableSettings.HBM2DDL_DELIMITER, ";",
        AvailableSettings.FORMAT_SQL, "true"
    );

    private static final Map<String, Object> INVARIANT_SETTINGS = Map.of(
        AvailableSettings.CONNECTION_PROVIDER, NoOpConnectionProvider.class.getName()
    );

    private final List<String> entityClassNames;
    private final Map<String, Object> settings;
    private final ClassLoader classLoader;

    private HibernateSchemaExporter(List<String> entityClassNames, Map<String, Object> settings, ClassLoader classLoader) {
        this.entityClassNames = List.copyOf(Objects.requireNonNull(entityClassNames, "entityClassNames must not be null"));
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader must not be null");
        this.settings = HibernateUtil.mergeSettings(DEFAULT_SETTINGS, settings, INVARIANT_SETTINGS);
    }

    /**
     * Creates a new {@code HibernateSchemaExporter} for the specified entity class names,
     * configuration settings, and class loader.
     *
     * @param entityClassNames the list of fully-qualified entity class names to include; must not be {@code null}
     * @param settings         custom configuration overrides (e.g.&nbsp;target SQL dialect or delimiter); may be {@code null}
     * @param classLoader      the class loader used to load entity classes and resources; must not be {@code null}
     * @return a new {@code HibernateSchemaExporter} instance
     * @throws NullPointerException if {@code entityClassNames} or {@code classLoader} is {@code null}
     */
    public static HibernateSchemaExporter of(List<String> entityClassNames, Map<String, Object> settings, ClassLoader classLoader) {
        return new HibernateSchemaExporter(entityClassNames, settings, classLoader);
    }

    /**
     * {@inheritDoc}
     *
     * @param action the schema generation action ({@code CREATE}, {@code DROP}, or {@code BOTH}); must not be {@code null}
     * @param file   the destination file to write the generated DDL statements to; must not be {@code null}
     * @throws IOException          if an I/O error occurs while creating or writing to the file
     * @throws NullPointerException if {@code action} or {@code file} is {@code null}
     */
    @Override
    public void export(Action action, File file) throws IOException {
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(file, "file must not be null");
        try (Writer writer = HibernateUtil.toWriter(file)) {
            export(action, writer);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param action the schema generation action ({@code CREATE}, {@code DROP}, or {@code BOTH}); must not be {@code null}
     * @param stream the output stream to write the generated DDL statements to; must not be {@code null}
     * @throws IOException          if an I/O error occurs while writing to the stream
     * @throws NullPointerException if {@code action} or {@code stream} is {@code null}
     */
    @Override
    public void export(Action action, OutputStream stream) throws IOException {
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(stream, "stream must not be null");
        try (Writer writer = HibernateUtil.toWriter(stream)) {
            export(action, writer);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param action the schema generation action ({@code CREATE}, {@code DROP}, or {@code BOTH}); must not be {@code null}
     * @param writer the writer to receive the generated DDL statements; must not be {@code null}
     * @throws IOException          if an I/O error occurs while writing
     * @throws NullPointerException if {@code action} or {@code writer} is {@code null}
     */
    @Override
    public void export(Action action, Writer writer) throws IOException {
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(writer, "writer must not be null");
        ScriptTargetOutput output = new ScriptTargetOutputToWriter(writer);
        exportToTargetOutput(action, output);
        writer.flush();
    }

    private void exportToTargetOutput(Action action, ScriptTargetOutput scriptTargetOutput) {
        var bootstrapRegistry = new BootstrapServiceRegistryBuilder()
            .applyClassLoader(classLoader)
            .build();

        var serviceRegistry = new StandardServiceRegistryBuilder(bootstrapRegistry)
            .applySettings(settings)
            .build();

        try {
            var sources = new MetadataSources(serviceRegistry);
            entityClassNames.forEach(sources::addAnnotatedClassName);
            var metadata = sources.buildMetadata();

            var tool = serviceRegistry.requireService(SchemaManagementTool.class);
            var options = SchemaManagementToolCoordinator.buildExecutionOptions(
                settings,
                ExceptionHandlerHaltImpl.INSTANCE
            );

            var schemaExport = new SchemaExport();
            var sourceDescriptor = new SourceDescriptor() {
                @Override
                public SourceType getSourceType() {
                    return SourceType.METADATA;
                }

                @Override
                public ScriptSourceInput getScriptSourceInput() {
                    return null;
                }
            };

            var targetDescriptor = new TargetDescriptor() {
                @Override
                public EnumSet<TargetType> getTargetTypes() {
                    return EnumSet.of(TargetType.SCRIPT);
                }

                @Override
                public ScriptTargetOutput getScriptTargetOutput() {
                    return scriptTargetOutput;
                }
            };

            switch (action) {
                case CREATE -> tool.getSchemaCreator(settings)
                    .doCreation(metadata, options, sourceDescriptor, targetDescriptor);
                case DROP -> tool.getSchemaDropper(settings)
                    .doDrop(metadata, options, sourceDescriptor, targetDescriptor);
                case BOTH -> {
                    tool.getSchemaDropper(settings)
                        .doDrop(metadata, options, sourceDescriptor, targetDescriptor);
                    tool.getSchemaCreator(settings)
                        .doCreation(metadata, options, sourceDescriptor, targetDescriptor);
                }
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(serviceRegistry);
        }
    }

}
