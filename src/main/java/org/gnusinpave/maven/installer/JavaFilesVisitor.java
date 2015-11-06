package org.gnusinpave.maven.installer;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class JavaFilesVisitor implements FileVisitor<Path> {
    private final Path basePath;

    private Path jar;
    private Path pom;

    private ExecutorService executorService = Executors.newFixedThreadPool(10);

    public JavaFilesVisitor(Path basePath) {
        this.basePath = basePath;
    }

    public void finish() {
        executorService.shutdown();
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (file.toString().endsWith(".jar")) {
            jar = file;
        }
        if (file.toString().endsWith(".pom")) {
            pom = file;
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (jar != null) {
            installDependency(jar, "jar");
        }
        if (pom != null) {
            installDependency(pom, "pom");
        }

        jar = null;
        pom = null;

        return FileVisitResult.CONTINUE;
    }

    private void installDependency(final Path file, final String packaging) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                int nameCount = file.getNameCount();
                String version = file.getParent().getName(nameCount - 2).getFileName().toString();
                String artifactId = file.getParent().getName(nameCount - 3).getFileName().toString();
                String groupId = buildGroupId(file, nameCount - 4);
                String fullPath = file.toString();

                try {
                    String command = String.format(
                            "mvn install:install-file -Dfile=%s -DartifactId=%s -DgroupId=%s -Dpackaging=%s -Dversion=%s",
                            fullPath, artifactId, groupId, packaging, version);
//                    System.out.println("command = " + command);
                    Process process = Runtime.getRuntime().exec(command);
                    int waitFor = process.waitFor();
                    System.out.println("Result: " + waitFor + ", " + file);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

//        System.out.println("version = " + version);
//        System.out.println("artifactId = " + artifactId);
//        System.out.println("groupId = " + groupId);
    }

    private String buildGroupId(Path file, int count) {
        String groupId = "";
        for (int i = count; i >= 0; --i) {
            groupId = File.separator + file.getName(i).getFileName().toString() + groupId;
        }
        return groupId.replace(basePath.toString(), "").replace(File.separator, ".").substring(1);
    }
}
