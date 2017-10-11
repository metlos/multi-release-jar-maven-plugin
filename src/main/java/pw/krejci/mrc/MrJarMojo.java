package pw.krejci.mrc;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.jar.AbstractJarMojo;
import org.apache.maven.plugins.jar.JarMojo;
import org.apache.maven.shared.utils.io.FileUtils;

/**
 * @author Lukas Krejci
 * @since 0.1.0
 */
@Mojo( name = "jar", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.RUNTIME )
public class MrJarMojo extends JarMojo {

    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true, required = true)
    private File buildOutputDirectory;

    @Parameter(defaultValue = "${project.build.directory}/multi-release-jar", readonly = true, required = true)
    private File multiReleaseClasses;

    @Parameter(defaultValue = "${basedir}/src/main/java-mr")
    private File multiReleaseSourcesDirectory;

    @Parameter
    private String mainModuleInfo;


    @Override protected File getClassesDirectory() {
        if (MultiReleaseJarSupport.isAvailable()) {
            return multiReleaseClasses;
        } else {
            return super.getClassesDirectory();
        }
    }

    @Override public void execute() throws MojoExecutionException {
        if (!MultiReleaseJarSupport.isAvailable() || !multiReleaseSourcesDirectory.exists()) {
            if (!MultiReleaseJarSupport.isAvailable()) {
                getLog().info("This java version does not support multi-release jars.");
            }
            super.execute();
            return;
        }

        if (!multiReleaseClasses.mkdirs()) {
            throw new MojoExecutionException(
                    "Failed to create the directory for multi-release-jar: " + multiReleaseClasses);
        }

        try {
            FileUtils.copyDirectoryStructure(buildOutputDirectory, multiReleaseClasses);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to copy " + buildOutputDirectory + " to " + multiReleaseClasses + ".", e);
        }

        if (mainModuleInfo != null) {
            File sourceModuleInfo = new File(CompileMojo.getOutputDirectory(buildOutputDirectory, mainModuleInfo), "module-info.class");
            File targetModuleInfo = new File(multiReleaseClasses, "module-info.class");
            try {
                Files.move(sourceModuleInfo.toPath(), targetModuleInfo.toPath());
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "Failed to move module-info.class from " + sourceModuleInfo + " to " + targetModuleInfo, e);
            }
        }

        //noinspection ConstantConditions
        boolean addMultiReleaseEntry = false;
        File[] sourceDirs = multiReleaseSourcesDirectory.listFiles(File::isDirectory);

        if (sourceDirs != null && sourceDirs.length > 0) {
            for (File mrBase : sourceDirs) {
                String release = mrBase.getName();

                File releaseOutput = CompileMojo.getOutputDirectory(buildOutputDirectory, release);

                String[] directChildren = releaseOutput.list();

                addMultiReleaseEntry = addMultiReleaseEntry || (directChildren != null && directChildren.length > 0);

                try {
                    FileUtils.copyDirectoryStructure(releaseOutput, new File(multiReleaseClasses, "META-INF/versions/" + release));
                } catch (IOException e) {
                    throw new MojoExecutionException("Failed to copy " + releaseOutput + " to " + multiReleaseClasses + ".", e);
                }
            }

            if (addMultiReleaseEntry) {
                addMultiReleaseManifestEntry();
            }
        }

        super.execute();
    }

    private void addMultiReleaseManifestEntry() throws MojoExecutionException {
        try {
            Field archive = AbstractJarMojo.class.getDeclaredField("archive");
            archive.setAccessible(true);
            MavenArchiveConfiguration config = (MavenArchiveConfiguration) archive.get(this);
            config.addManifestEntry("Multi-Release", "true");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new MojoExecutionException("Could not modify the archive configuration.", e);
        }
    }
}
