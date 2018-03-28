package pw.krejci.mrc;

import static java.util.Collections.asLifoQueue;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.compiler.CompilationFailureException;
import org.apache.maven.plugin.compiler.CompilerMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;

import pw.krejci.mrc.CompileConfiguration.Configuration;

/**
 * @author Lukas Krejci
 * @since 0.1.0
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class CompileMojo extends CompilerMojo {

    @Parameter(defaultValue = "${basedir}/src/main/java-mr")
    private File multiReleaseSourcesDirectory;

    @Parameter
    private List<CompileConfiguration> perReleaseConfiguration;

    // need to redefine includes and excludes, because they are not accessible in the parent and we need to them to
    // reimplement some of the methods used by the parent.
    /**
     * A list of inclusion filters for the compiler.
     */
    @Parameter
    private Set<String> includes = new HashSet<>();

    /**
     * A list of exclusion filters for the compiler.
     */
    @Parameter
    private Set<String> excludes = new HashSet<>();

    private CompileConfiguration currentConfiguration;
    private String currentSourceDirectory;
    private String defaultOutputDirectory;
    private String defaultSourceDirectory;
    private boolean compilingModuleDescriptor;

    @Override public void execute() throws MojoExecutionException, CompilationFailureException {
        if (!MultiReleaseJarSupport.isAvailable() || !multiReleaseSourcesDirectory.exists()) {
            if (!MultiReleaseJarSupport.isAvailable()) {
                getLog().info("This java version does not support multi-release jars.");
            }
            super.execute();
            return;
        }

        Map<String, CompileConfiguration> configMap;
        if (perReleaseConfiguration == null || perReleaseConfiguration.isEmpty()) {
            configMap = emptyMap();
        } else {
            configMap = perReleaseConfiguration.stream()
                    .collect(Collectors.toMap(CompileConfiguration::getRelease, identity()));
        }

        defaultOutputDirectory = getProject().getBuild().getOutputDirectory();
        defaultSourceDirectory = getProject().getBuild().getSourceDirectory();

        super.execute();

        try {
            //noinspection ConstantConditions
            for (File mrBase : multiReleaseSourcesDirectory.listFiles(File::isDirectory)) {
                String release = mrBase.getName();
                if (configMap.containsKey(release)) {
                    currentConfiguration = configMap.get(release);
                } else {
                    currentConfiguration = CompileConfiguration.emptyForRelease(release);
                }

                currentSourceDirectory = mrBase.getAbsolutePath();

                getProject().getBuild().setOutputDirectory(getOutputDirectory().toString());

                if (currentConfiguration.getConfiguration().getRelease() == null) {
                    currentConfiguration.getConfiguration().setRelease(release);
                }

                if (currentConfiguration.getConfiguration().getSource() == null) {
                    currentConfiguration.getConfiguration().setSource(release);
                }

                if (currentConfiguration.getConfiguration().getTarget() == null) {
                    currentConfiguration.getConfiguration().setTarget(release);
                }

                if (currentConfiguration.getConfiguration().getCompilerArgs() != null) {
                    this.compilerArgs = currentConfiguration.getConfiguration().getCompilerArgs();
                }

                if (currentConfiguration.getConfiguration().getRelease() != null) {
                    this.release = currentConfiguration.getConfiguration().getRelease();
                }

                File moduleDescriptor = new File(mrBase, "module-info.java");

                if (moduleDescriptor.exists()) {
                    // now, magic. In order to be able to effectively compile all sources just once, we need to make
                    // sure that the module-info.java is actually compiled in a separate step and from a separate
                    // directory than the rest of the MR sources. This is because module-info.java is compiled even if
                    // it is not specifically mentioned amongst the source files. Because the main sources are already
                    // compiled above we want to just reference them by classpath, not have them compiled again in the
                    // target folder of this MR "section". But that would fail compilation, because module-info.java
                    // would not be able to find their sources (if we added the base sources to the source path then any
                    // referenced class (i.e. a class used in the MR sources) would be compiled again - we don't want
                    // that).

                    File copiedSourcesRoot = new File(new File(defaultOutputDirectory).getParent(),
                            "sources-" + release);
                    File sources = new File(copiedSourcesRoot, "sources");
                    File descriptor = new File(copiedSourcesRoot, "descriptor");

                    FileUtils.copyDirectory(mrBase, sources);
                    FileUtils.moveFile(new File(sources, "module-info.java"), new File(descriptor, "module-info.java"));

                    // ok, now we actually don't care whether we compile the module descriptor or the sources first.
                    // so let's just start with the module descriptor...

                    currentSourceDirectory = descriptor.getAbsolutePath();

                    Set<String> configuredIncludes = currentConfiguration.getConfiguration().getIncludes();
                    Set<String> configuredExcludes = currentConfiguration.getConfiguration().getExcludes();

                    currentConfiguration.getConfiguration().setIncludes(singleton("module-info.java"));
                    currentConfiguration.getConfiguration().setExcludes(emptySet());

                    compilingModuleDescriptor = true;

                    super.execute();

                    // fine, now let's compile the rest...
                    currentSourceDirectory = sources.getAbsolutePath();
                    currentConfiguration.getConfiguration().setIncludes(configuredIncludes);
                    currentConfiguration.getConfiguration().setExcludes(configuredExcludes);

                    compilingModuleDescriptor = false;
                } else {
                    currentSourceDirectory = mrBase.getAbsolutePath();
                }

                super.execute();
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to prepare multi-release sources for staged compilation.", e);
        } finally {
            getProject().getBuild().setOutputDirectory(defaultOutputDirectory);
        }
    }

    @Override protected List<String> getCompileSourceRoots() {
        if (MultiReleaseJarSupport.isAvailable()) {
            return super.getCompileSourceRoots().stream()
                    .flatMap(e -> {
                        if (currentConfiguration != null && e.equals(defaultSourceDirectory)) {
                            if (compilingModuleDescriptor) {
                                // when compiling the module descriptor, we need to see all the classes that can
                                // participate in the module - these are all in the default source...
                                return Stream.of(currentSourceDirectory, defaultSourceDirectory);
                            } else {
                                return Stream.of(currentSourceDirectory);
                            }
                        } else {
                            return Stream.of(e);
                        }
                    })
                    .collect(toList());
        } else {
            return super.getCompileSourceRoots();
        }
    }

    @Override protected List<String> getClasspathElements() {
        if (MultiReleaseJarSupport.isAvailable()) {
            String currentOutput = getProject().getBuild().getOutputDirectory();
            return super.getClasspathElements().stream()
                    .map(e -> defaultOutputDirectory != null && e.equals(currentOutput) ? defaultOutputDirectory : e)
                    .collect(toList());
        } else {
            return super.getClasspathElements();
        }
    }

    @Override protected File getOutputDirectory() {
        if (!MultiReleaseJarSupport.isAvailable() || currentConfiguration == null) {
            return super.getOutputDirectory();
        }

        return compilingModuleDescriptor
                ? getOutputDirectoryForModuleDescriptor(super.getOutputDirectory(), currentConfiguration.getRelease())
                : getOutputDirectory(super.getOutputDirectory(), currentConfiguration.getRelease());
    }

    protected static File getOutputDirectory(File defaultOutputDirectory, String modifier) {
        Path p = defaultOutputDirectory.toPath();
        Path parent = p.getParent();
        Path fileName = p.getFileName();
        Path newPath = parent.resolve(fileName.toString() + "-" + modifier);

        return newPath.toFile();
    }

    protected static File getOutputDirectoryForModuleDescriptor(File defaultOutputDirectory, String modifier) {
        Path p = defaultOutputDirectory.toPath();
        Path parent = p.getParent();
        Path fileName = p.getFileName();
        Path newPath = parent.resolve(fileName.toString() + "-" + modifier + "-descriptor");

        return newPath.toFile();
    }

    @Override protected SourceInclusionScanner getSourceInclusionScanner(int staleMillis) {
        Set<String> includes;
        Set<String> excludes;

        if (MultiReleaseJarSupport.isAvailable()) {
            includes = getOrCall(Configuration::getIncludes, () -> this.includes);
            excludes = getOrCall(Configuration::getExcludes, () -> this.excludes);
        } else {
            includes = this.includes;
            excludes = this.excludes;
        }

        SourceInclusionScanner scanner;

        if (includes.isEmpty() && excludes.isEmpty()) {
            scanner = new StaleSourceScanner(staleMillis);
        } else {
            if (includes.isEmpty()) {
                includes.add("**/*.java");
            }
            scanner = new StaleSourceScanner(staleMillis, includes, excludes);
        }

        return scanner;
    }

    @Override protected SourceInclusionScanner getSourceInclusionScanner(String inputFileEnding) {
        Set<String> includes;
        Set<String> excludes;

        if (MultiReleaseJarSupport.isAvailable()) {
            includes = getOrCall(Configuration::getIncludes, () -> this.includes);
            excludes = getOrCall(Configuration::getExcludes, () -> this.excludes);
        } else {
            includes = this.includes;
            excludes = this.excludes;
        }

        SourceInclusionScanner scanner;

        // it's not defined if we get the ending with or without the dot '.'
        String defaultIncludePattern = "**/*" + (inputFileEnding.startsWith(".") ? "" : ".") + inputFileEnding;

        if (includes.isEmpty() && excludes.isEmpty()) {
            includes = Collections.singleton(defaultIncludePattern);
            scanner = new SimpleSourceInclusionScanner(includes, Collections.<String>emptySet());
        } else {
            if (includes.isEmpty()) {
                includes.add(defaultIncludePattern);
            }
            scanner = new SimpleSourceInclusionScanner(includes, excludes);
        }

        return scanner;
    }

    @Override protected String getSource() {
        return getOrCall(Configuration::getSource, () -> super.getSource());
    }

    @Override protected String getTarget() {
        return getOrCall(Configuration::getTarget, () -> super.getTarget());
    }

    @Override protected String getRelease() {
        return getOrCall(Configuration::getRelease, () -> super.getRelease());
    }

    @Override protected String getCompilerArgument() {
        return getOrCall(Configuration::getCompilerArgument, () -> super.getCompilerArgument());
    }

    @Override protected Map<String, String> getCompilerArguments() {
        return getOrCall(Configuration::getCompilerArguments, () -> super.getCompilerArguments());
    }

    @Override protected File getGeneratedSourcesDirectory() {
        return getOrCall(Configuration::getGeneratedSourcesDirectory, () -> super.getGeneratedSourcesDirectory());
    }

    @SuppressWarnings("unchecked")
    private <T> T getOrCall(Function<Configuration, T> configOption, Supplier<T> call) {
        if (!MultiReleaseJarSupport.isAvailable()) {
            return call.get();
        }

        if (currentConfiguration != null) {
            T val = configOption.apply(currentConfiguration.getConfiguration());
            if (val != null) {
                return val;
            }
        }
        return call.get();
    }
}
