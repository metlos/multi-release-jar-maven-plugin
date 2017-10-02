package pw.krejci.mrc;

import static java.util.Collections.emptyMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
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

                super.execute();
            }
        } finally {
            getProject().getBuild().setOutputDirectory(defaultOutputDirectory);
        }
    }

    @Override protected List<String> getCompileSourceRoots() {
        if (MultiReleaseJarSupport.isAvailable()) {
            return super.getCompileSourceRoots().stream()
                    .map(e -> currentSourceDirectory != null && e.equals(defaultSourceDirectory) ? currentSourceDirectory : e)
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

        return getOutputDirectory(super.getOutputDirectory(), currentConfiguration.getRelease());
    }

    protected static File getOutputDirectory(File defaultOutputDirectory, String modifier) {
        Path p = defaultOutputDirectory.toPath();
        Path parent = p.getParent();
        Path fileName = p.getFileName();
        Path newPath = parent.resolve(fileName.toString() + "-" + modifier);

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
