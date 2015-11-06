package org.gnusinpave.maven.installer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Укажите базовый путь под библиотеки");
        }

        Path basePath = Paths.get(args[0]);
        installJars(basePath);
    }

    private static void installJars(Path basePath) {
        JavaFilesVisitor visitor = new JavaFilesVisitor(basePath);
        try {
            Files.walkFileTree(basePath, visitor);
        } catch (IOException e) {
            e.printStackTrace();
        }
        visitor.finish();
    }
}
