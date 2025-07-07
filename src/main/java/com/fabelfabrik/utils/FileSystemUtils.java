package com.fabelfabrik.utils;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * Utility class for file system operations.
 * Contains methods for managing temporary files and directories.
 */
public class FileSystemUtils {
    
    private static final Logger LOG = Logger.getLogger(FileSystemUtils.class);
    
    /**
     * Cleans up the temporary directory if it exceeds a predefined size limit.
     * Specifically, this method targets files that are related to S3 downloads
     * (those with names starting with "s3-download-") for deletion to free up space.
     *
     * The method calculates the current size of the temporary directory and logs the result.
     * If the size exceeds 1.9GB, it identifies relevant S3 files and deletes them to
     * reduce the directory size. After cleanup, the new size is calculated and logged.
     *
     * If any errors occur during directory size calculation, file filtering, or deletion,
     * they are logged with appropriate error messages.
     */
    public static void cleanupTempFolderIfNeeded() {
        try {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
            long maxSizeBytes = 1_900_000_000L; // 1.9GB in bytes
        
            long currentSize = calculateDirectorySize(tempDir);
            LOG.infof("Current temp directory size: %d bytes (%.2f GB)", currentSize, currentSize / 1_000_000_000.0);
        
            if (currentSize > maxSizeBytes) {
                LOG.infof("Temp directory exceeds 1.9GB, cleaning up S3 download files...");
            
                // Delete all files starting with "s3-download-"
                try (Stream<Path> files=Files.walk(tempDir, 1)) {
                    List<Path> s3Files = getS3Files(files);
                    deleteFiles(s3Files);

                    // Log new size after cleanup
                    long newSize = calculateDirectorySize(tempDir);
                    LOG.infof("New temp directory size after cleanup: %d bytes (%.2f GB)", newSize, newSize / 1_000_000_000.0);
                }
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error during temp folder cleanup: %s", e.getMessage());
        }
    }

    /**
     * Filters a stream of file paths to retrieve a list of paths related to S3 downloads.
     * Only regular files with names starting with "s3-download-" are included in the result.
     *
     * @param files A stream of file paths to be processed.
     * @return A list of paths that are regular files and have names starting with "s3-download-".
     */
    private static List<Path> getS3Files(Stream<Path> files) {
        return files
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().startsWith("s3-download-"))
                .toList();
    }

    /**
     * Deletes a list of files from the filesystem and logs the result.
     * Files that cannot be deleted are logged with a warning.
     *
     * @param s3Files The list of file paths to be deleted.
     */
    private static void deleteFiles(List<Path> s3Files) {
        long deletedSize = 0;
        int deletedCount = 0;

        for (Path file : s3Files) {
            try {
                long fileSize = Files.size(file);
                Files.delete(file);
                deletedSize += fileSize;
                deletedCount++;
                LOG.infof("Deleted S3 temp file: %s (size: %d bytes)", file.getFileName(), fileSize);
            } catch (IOException e) {
                LOG.warnf("Failed to delete S3 temp file: %s - %s", file.getFileName(), e.getMessage());
            }
        }

        LOG.infof("Cleanup completed: deleted %d S3 files, freed %d bytes (%.2f GB)",
            deletedCount, deletedSize, deletedSize / 1_000_000_000.0);
    }

    /**
     * Calculates the total size of all regular files in the specified directory.
     * The size is calculated by summing up the sizes of each individual file.
     *
     * If an error occurs while accessing a file or calculating its size, the
     * method logs the error and excludes the problematic file from the total size.
     *
     * @param directory The path to the directory whose size is to be calculated.
     * @return The total size of the regular files in the directory in bytes.
     *         Returns 0 if the directory cannot be accessed or an error occurs.
     */
    private static long calculateDirectorySize(Path directory) {
        try (var files = Files.walk(directory, 1)) {
            return files
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        LOG.warnf("Could not get size of file: %s - %s", path, e.getMessage());
                        return 0L;
                    }
                })
                .sum();
        } catch (IOException e) {
            LOG.errorf(e, "Error calculating directory size: %s", e.getMessage());
            return 0L;
        }
    }
}