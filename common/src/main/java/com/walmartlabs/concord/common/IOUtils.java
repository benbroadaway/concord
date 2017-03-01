package com.walmartlabs.concord.common;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class IOUtils {

    public static boolean matches(Path p, String... filters) {
        String n = p.getName(p.getNameCount() - 1).toString();
        for (String f : filters) {
            if (n.matches(f)) {
                return true;
            }
        }
        return false;
    }

    public static void zipFile(ZipOutputStream zip, Path src, String name) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        try (InputStream in = Files.newInputStream(src)) {
            copy(in, zip);
        }
    }

    public static void zip(ZipOutputStream zip, Path srcDir, String... filters) throws IOException {
        zip(zip, null, srcDir, filters);
    }

    public static void zip(ZipOutputStream zip, String dstPrefix, Path srcDir, String... filters) throws IOException {
        Files.walkFileTree(srcDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (dir.toAbsolutePath().equals(srcDir)) {
                    return FileVisitResult.CONTINUE;
                }

                if (matches(dir, filters)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (matches(file, filters)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                String n = srcDir.relativize(file).toString();
                if (dstPrefix != null) {
                    n = dstPrefix + n;
                }

                zipFile(zip, file, n);

                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void unzip(ZipInputStream in, Path targetDir) throws IOException {
        ZipEntry e;
        while ((e = in.getNextEntry()) != null) {
            File f = targetDir.resolve(e.getName()).toFile();
            if (e.isDirectory()) {
                f.mkdirs();
            } else {
                File parent = f.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }

                try (OutputStream out = new FileOutputStream(f)) {
                    copy(in, out);
                }
            }
        }
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] ab = new byte[4096];
        int read;
        while ((read = in.read(ab)) > 0) {
            out.write(ab, 0, read);
        }
    }

    public static void copy(Path src, Path dst) throws IOException {
        Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path p = dst.resolve(src.relativize(file));

                Path pp = p.getParent();
                if (!Files.exists(pp)) {
                    Files.createDirectories(pp);
                }

                Files.copy(file, p);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static List<String> grep(String pattern, byte[] ab) throws IOException {
        return grep(pattern, new ByteArrayInputStream(ab));
    }

    public static List<String> grep(String pattern, InputStream in) throws IOException {
        List<String> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.matches(pattern)) {
                    result.add(line);
                }
            }
        }
        return result;
    }

    private IOUtils() {
    }
}
