package org.hawrylak.iphonephotorename;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IPhonePhotoRename {

    private static Logger logger = LogManager.getLogger(IPhonePhotoRename.class);
    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");

    public static void main(String[] args) {
        try {
            new IPhonePhotoRename().run(args[0]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void run(String directory) throws IOException {
        logger.info("IPhone Photo Rename (reads data from EXIF of jpeg");
        for (String file : listFiles(directory)) {
            var fileName = parseFileName(glue(directory, file));
            var date = readDateFromFile(fileName);
            var newName = generateNewName(date);
            logger.info(fileName.nameWithoutExtension() + " -> " + date + " -> " + newName);
        }
    }

    private String glue(String directory, String file) {
        return directory + File.separator + file;
    }

    private Set<String> listFiles(String dir) {
        return Stream.of(new File(dir).listFiles())
            .filter(file -> !file.isDirectory())
            .map(File::getName)
            .collect(Collectors.toSet());
    }

    private FileName parseFileName(String fileName) {
        var file = new File(fileName);
        Optional<String> nameWithoutExtension = getFileNameWithoutExtension(file.getName());
        Optional<String> extension = getFileNameExtension(fileName);
        return new FileName(file, file.getAbsolutePath(), file.getName(), nameWithoutExtension, extension);
    }

    private String generateNewName(Date date) {
        return formatter.format(date);
    }

    public Optional<String> getFileNameWithoutExtension(String filename) {
        return Optional.ofNullable(filename)
            .filter(f -> f.contains("."))
            .map(f -> f.substring(0, filename.lastIndexOf(".")));
    }

    public Optional<String> getFileNameExtension(String filename) {
        return Optional.ofNullable(filename)
            .filter(f -> f.contains("."))
            .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    private Date readDateFromFile(FileName fileName) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(fileName.file());
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            return directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
        } catch (ImageProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printAllMetadataFromFile(FileName fileName) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(fileName.file());
            for (Directory directory : metadata.getDirectories()) {
                logger.info("directory: " + directory.getName());
                for (Tag tag : directory.getTags()) {
                    logger.info("tag: " + tag);
                }
            }
        } catch (ImageProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}