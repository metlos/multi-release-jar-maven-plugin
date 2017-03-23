package pw.krejci.mrc;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.compiler.DependencyCoordinate;

/**
 * @author Lukas Krejci
 * @since 0.1.0
 */
public class CompileConfiguration {

    private String release;
    private Configuration configuration;

    public static CompileConfiguration emptyForRelease(String release) {
        CompileConfiguration cc = new CompileConfiguration();
        cc.setRelease(release);
        cc.setConfiguration(new Configuration());
        return cc;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public static class Configuration {
        private Set<String> includes;
        private Set<String> excludes;
        private File generatedSourcesDirectory;
        private Boolean skipMain;
        private Boolean failOnError;
        private Boolean failOnWarning;
        private Boolean debug;
        private Boolean verbose;
        private Boolean showDeprecation;
        private Boolean optimize;
        private Boolean showWarnings;
        private String source;
        private String target;
        private String release;
        private String encoding;
        private Integer staleMillis;
        private String compilerId;
        private String compilerVersion;
        private Boolean fork;
        private String meminitial;
        private String maxmem;
        private String executable;
        private String proc;
        private String[] annotationProcessors;
        private List<DependencyCoordinate> annotationProcessorPaths;
        private Map<String, String> compilerArguments;
        private List<String> compilerArgs;
        private String compilerArgument;
        private String outputFileName;
        private String debuglevel;
        private Map<String, String> jdkToolchain;
        private String compilerReuseStrategy;
        private Boolean skipMultiThreadWarning;
        private Boolean forceJavacCompilerUse;
        private List<String> fileExtensions;
        private Boolean useIncrementalCompilation;

        public Set<String> getIncludes() {
            return includes;
        }

        public void setIncludes(Set<String> includes) {
            this.includes = includes;
        }

        public Set<String> getExcludes() {
            return excludes;
        }

        public void setExcludes(Set<String> excludes) {
            this.excludes = excludes;
        }

        public File getGeneratedSourcesDirectory() {
            return generatedSourcesDirectory;
        }

        public void setGeneratedSourcesDirectory(File generatedSourcesDirectory) {
            this.generatedSourcesDirectory = generatedSourcesDirectory;
        }

        public Boolean getSkipMain() {
            return skipMain;
        }

        public void setSkipMain(Boolean skipMain) {
            this.skipMain = skipMain;
        }

        public Boolean getFailOnError() {
            return failOnError;
        }

        public void setFailOnError(Boolean failOnError) {
            this.failOnError = failOnError;
        }

        public Boolean getFailOnWarning() {
            return failOnWarning;
        }

        public void setFailOnWarning(Boolean failOnWarning) {
            this.failOnWarning = failOnWarning;
        }

        public Boolean getDebug() {
            return debug;
        }

        public void setDebug(Boolean debug) {
            this.debug = debug;
        }

        public Boolean getVerbose() {
            return verbose;
        }

        public void setVerbose(Boolean verbose) {
            this.verbose = verbose;
        }

        public Boolean getShowDeprecation() {
            return showDeprecation;
        }

        public void setShowDeprecation(Boolean showDeprecation) {
            this.showDeprecation = showDeprecation;
        }

        public Boolean getOptimize() {
            return optimize;
        }

        public void setOptimize(Boolean optimize) {
            this.optimize = optimize;
        }

        public Boolean getShowWarnings() {
            return showWarnings;
        }

        public void setShowWarnings(Boolean showWarnings) {
            this.showWarnings = showWarnings;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public String getRelease() {
            return release;
        }

        public void setRelease(String release) {
            this.release = release;
        }

        public String getEncoding() {
            return encoding;
        }

        public void setEncoding(String encoding) {
            this.encoding = encoding;
        }

        public Integer getStaleMillis() {
            return staleMillis;
        }

        public void setStaleMillis(Integer staleMillis) {
            this.staleMillis = staleMillis;
        }

        public String getCompilerId() {
            return compilerId;
        }

        public void setCompilerId(String compilerId) {
            this.compilerId = compilerId;
        }

        public String getCompilerVersion() {
            return compilerVersion;
        }

        public void setCompilerVersion(String compilerVersion) {
            this.compilerVersion = compilerVersion;
        }

        public Boolean getFork() {
            return fork;
        }

        public void setFork(Boolean fork) {
            this.fork = fork;
        }

        public String getMeminitial() {
            return meminitial;
        }

        public void setMeminitial(String meminitial) {
            this.meminitial = meminitial;
        }

        public String getMaxmem() {
            return maxmem;
        }

        public void setMaxmem(String maxmem) {
            this.maxmem = maxmem;
        }

        public String getExecutable() {
            return executable;
        }

        public void setExecutable(String executable) {
            this.executable = executable;
        }

        public String getProc() {
            return proc;
        }

        public void setProc(String proc) {
            this.proc = proc;
        }

        public String[] getAnnotationProcessors() {
            return annotationProcessors;
        }

        public void setAnnotationProcessors(String[] annotationProcessors) {
            this.annotationProcessors = annotationProcessors;
        }

        public List<DependencyCoordinate> getAnnotationProcessorPaths() {
            return annotationProcessorPaths;
        }

        public void setAnnotationProcessorPaths(
                List<DependencyCoordinate> annotationProcessorPaths) {
            this.annotationProcessorPaths = annotationProcessorPaths;
        }

        public Map<String, String> getCompilerArguments() {
            return compilerArguments;
        }

        public void setCompilerArguments(Map<String, String> compilerArguments) {
            this.compilerArguments = compilerArguments;
        }

        public List<String> getCompilerArgs() {
            return compilerArgs;
        }

        public void setCompilerArgs(List<String> compilerArgs) {
            this.compilerArgs = compilerArgs;
        }

        public String getCompilerArgument() {
            return compilerArgument;
        }

        public void setCompilerArgument(String compilerArgument) {
            this.compilerArgument = compilerArgument;
        }

        public String getOutputFileName() {
            return outputFileName;
        }

        public void setOutputFileName(String outputFileName) {
            this.outputFileName = outputFileName;
        }

        public String getDebuglevel() {
            return debuglevel;
        }

        public void setDebuglevel(String debuglevel) {
            this.debuglevel = debuglevel;
        }

        public Map<String, String> getJdkToolchain() {
            return jdkToolchain;
        }

        public void setJdkToolchain(Map<String, String> jdkToolchain) {
            this.jdkToolchain = jdkToolchain;
        }

        public String getCompilerReuseStrategy() {
            return compilerReuseStrategy;
        }

        public void setCompilerReuseStrategy(String compilerReuseStrategy) {
            this.compilerReuseStrategy = compilerReuseStrategy;
        }

        public Boolean getSkipMultiThreadWarning() {
            return skipMultiThreadWarning;
        }

        public void setSkipMultiThreadWarning(Boolean skipMultiThreadWarning) {
            this.skipMultiThreadWarning = skipMultiThreadWarning;
        }

        public Boolean getForceJavacCompilerUse() {
            return forceJavacCompilerUse;
        }

        public void setForceJavacCompilerUse(Boolean forceJavacCompilerUse) {
            this.forceJavacCompilerUse = forceJavacCompilerUse;
        }

        public List<String> getFileExtensions() {
            return fileExtensions;
        }

        public void setFileExtensions(List<String> fileExtensions) {
            this.fileExtensions = fileExtensions;
        }

        public Boolean getUseIncrementalCompilation() {
            return useIncrementalCompilation;
        }

        public void setUseIncrementalCompilation(Boolean useIncrementalCompilation) {
            this.useIncrementalCompilation = useIncrementalCompilation;
        }
    }
}
