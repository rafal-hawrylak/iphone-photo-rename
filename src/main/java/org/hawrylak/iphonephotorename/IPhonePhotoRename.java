package org.hawrylak.iphonephotorename;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IPhonePhotoRename {

    private static Logger logger = LogManager.getLogger(IPhonePhotoRename.class);
    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
    private static String supportedExtention = "jpeg";

    public static void main(String[] args) {
        if (args.length == 0) {
            logger.error("usage:\r\nIPhonePhotoRename directory [earliestValidDate]");
            return;
        }
        String directory = args[0];
        Optional<Date> earliestValidDate = args.length >= 2 ? convertToDate(args[1]) : Optional.empty();
        new IPhonePhotoRename().run(directory, earliestValidDate);
    }

    private static Optional<Date> convertToDate(String date) {
        try {
            return Optional.ofNullable(formatter.parse(date));
        } catch (ParseException e) {
            return Optional.empty();
        }
    }

    private void run(String directory, Optional<Date> earliestValidDate) {
        logger.info("IPhone Photo Rename (reads data from EXIF of jpeg");
        for (String file : listFiles(directory)) {
            var fileName = parseFileName(glue(directory, file, Optional.empty()));
            if (!isSupportedExtention(fileName)) {
                logger.warn("Not supported file type " + file);
                continue;
            }
            var dateFromEXIF = readDateFromFile(fileName);
            validateDate(dateFromEXIF, earliestValidDate, fileName);
            var newName = getFirstFreeName(fileName, dateFromEXIF);
            logger.info(fileName.nameWithoutExtension() + " -> " + dateFromEXIF + " -> " + newName);
        }
    }

    private String getFirstFreeName(FileName fileName, Date date) {
        var newName = "";
        Date newDate;
        try {
            newDate = formatter.parse(formatter.format(date));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        do {
            newName = generateNewName(newDate);
            newDate.setTime(newDate.getTime() + 1000);
        } while (fileExists(glue(fileName.path(), newName, fileName.extension())));
        return newName;
    }

    private boolean fileExists(String fileName) {
        Path path = Paths.get(fileName);
        return Files.exists(path);
    }

    private boolean isSupportedExtention(FileName fileName) {
        return fileName.extension().isPresent() && fileName.extension().get().equalsIgnoreCase(supportedExtention);
    }

    private Boolean validateDate(Date date, Optional<Date> earliestValidDate, FileName fileName) {
        if (Objects.isNull(date)) {
            logger.error("Could not read date from " + fileName.name());
            return false;
        }
        if (earliestValidDate.isPresent() && date.before(earliestValidDate.get())) {
            logger.error("Date " + date + " is before " + earliestValidDate.get() + " for file " + fileName.name());
            return false;
        }
        return true;
    }

    private String glue(String directory, String file, Optional<String> extension) {
        return directory + File.separator + file + extension.map(s -> "." + s).orElse("");
    }

    private List<String> listFiles(String dir) {
        File[] files = new File(dir).listFiles();
        if (Objects.isNull(files)) {
            throw new IllegalArgumentException("Invalid directory: " + dir);
        }
        if (files.length == 0) {
            throw new IllegalArgumentException("Empty directory: " + dir);
        }
        return Stream.of(files)
            .filter(file -> !file.isDirectory())
            .map(File::getName)
            .sorted()
            .collect(Collectors.toList());
    }

    private FileName parseFileName(String fileName) {
        var file = new File(fileName);
        Optional<String> nameWithoutExtension = getFileNameWithoutExtension(file.getName());
        Optional<String> extension = getFileNameExtension(fileName);
        return new FileName(file, file.getParent(), file.getName(), nameWithoutExtension, extension);
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