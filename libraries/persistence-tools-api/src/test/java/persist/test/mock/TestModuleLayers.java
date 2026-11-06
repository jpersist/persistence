package persist.test.mock;

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public final class TestModuleLayers {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private TestModuleLayers() {
    }

    public static ModuleLayer createModularLayer(
        ClassLoader parentClassLoader,
        Class<?> serviceClass,
        Class<?> providerClass
    ) {
        int id = COUNTER.incrementAndGet();
        String apiModuleName = "io.github.jpersist.tools.api.test" + id;
        String providerModuleName = "test.provider.module" + id;
        String providerPkg = providerClass.getPackageName();

        ModuleDescriptor apiDescriptor = ModuleDescriptor.newModule(apiModuleName)
            .requires("java.base")
            .exports("persist.api.spi")
            .exports("persist.lint.api.spi")
            .exports("persist.lint.api")
            .exports("persist.hibernate.enhance.spi")
            .uses(serviceClass.getName())
            .build();

        ModuleDescriptor providerDescriptor = ModuleDescriptor.newModule(providerModuleName)
            .requires("java.base")
            .requires(apiModuleName)
            .exports(providerPkg)
            .opens(providerPkg)
            .provides(serviceClass.getName(), List.of(providerClass.getName()))
            .build();

        ModuleReference apiRef = createModuleReference(apiModuleName, apiDescriptor, parentClassLoader);
        ModuleReference providerRef = createModuleReference(providerModuleName, providerDescriptor, parentClassLoader);

        Map<String, ModuleReference> apiModules = Map.of(apiModuleName, apiRef);
        ModuleFinder apiFinder = new ModuleFinder() {
            @Override
            public Optional<ModuleReference> find(String name) {
                return Optional.ofNullable(apiModules.get(name));
            }

            @Override
            public Set<ModuleReference> findAll() {
                return Set.copyOf(apiModules.values());
            }
        };

        Configuration apiConfig = ModuleLayer.boot().configuration().resolve(
            apiFinder,
            ModuleFinder.of(),
            List.of(apiModuleName)
        );

        ModuleLayer apiLayer = ModuleLayer.boot().defineModulesWithOneLoader(
            apiConfig,
            parentClassLoader
        );

        Map<String, ModuleReference> providerModules = Map.of(providerModuleName, providerRef);
        ModuleFinder providerFinder = new ModuleFinder() {
            @Override
            public Optional<ModuleReference> find(String name) {
                return Optional.ofNullable(providerModules.get(name));
            }

            @Override
            public Set<ModuleReference> findAll() {
                return Set.copyOf(providerModules.values());
            }
        };

        Configuration providerConfig = apiConfig.resolve(
            providerFinder,
            ModuleFinder.of(),
            List.of(providerModuleName)
        );

        return apiLayer.defineModulesWithOneLoader(
            providerConfig,
            parentClassLoader
        );
    }

    public static Object invokeStatic(
        ModuleLayer layer,
        Class<?> targetClass,
        String methodName,
        Class<?>[] paramTypes,
        Object... args
    ) {
        try {
            ClassLoader loader = layer.modules().stream()
                .filter(m -> m.getName().startsWith("io.github.jpersist.tools.api.test"))
                .findFirst()
                .orElse(layer.parents().get(0).modules().stream()
                    .filter(m -> m.getName().startsWith("io.github.jpersist.tools.api.test"))
                    .findFirst()
                    .orElseThrow())
                .getClassLoader();

            Class<?> loadedClass = loader.loadClass(targetClass.getName());
            Class<?>[] resolvedParamTypes = new Class<?>[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                if (paramTypes[i].getName().startsWith("persist.")) {
                    resolvedParamTypes[i] = loader.loadClass(paramTypes[i].getName());
                } else {
                    resolvedParamTypes[i] = paramTypes[i];
                }
            }

            Object[] resolvedArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Class<?> cls && cls.getName().startsWith("persist.")) {
                    resolvedArgs[i] = loader.loadClass(cls.getName());
                } else {
                    resolvedArgs[i] = args[i];
                }
            }

            Method method = loadedClass.getMethod(methodName, resolvedParamTypes);
            return method.invoke(null, resolvedArgs);
        } catch (Exception e) {
            if (e instanceof java.lang.reflect.InvocationTargetException ite) {
                Throwable target = ite.getTargetException();
                if (target instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException(target);
            }
            throw new RuntimeException(e);
        }
    }

    private static ModuleReference createModuleReference(
        String moduleName,
        ModuleDescriptor descriptor,
        ClassLoader parentClassLoader
    ) {
        return new ModuleReference(descriptor, URI.create("module://" + moduleName)) {
            @Override
            public ModuleReader open() {
                return new ModuleReader() {
                    @Override
                    public Optional<URI> find(String name) {
                        URL url = parentClassLoader.getResource(name);
                        if (url != null) {
                            try {
                                return Optional.of(url.toURI());
                            } catch (Exception ignored) {
                            }
                        }
                        return Optional.empty();
                    }

                    @Override
                    public Optional<InputStream> open(String name) {
                        return Optional.ofNullable(parentClassLoader.getResourceAsStream(name));
                    }

                    @Override
                    public Optional<ByteBuffer> read(String name) throws IOException {
                        try (InputStream in = parentClassLoader.getResourceAsStream(name)) {
                            if (in == null) return Optional.empty();
                            return Optional.of(ByteBuffer.wrap(in.readAllBytes()));
                        }
                    }

                    @Override
                    public Stream<String> list() {
                        return Stream.empty();
                    }

                    @Override
                    public void close() {
                    }
                };
            }
        };
    }
}
