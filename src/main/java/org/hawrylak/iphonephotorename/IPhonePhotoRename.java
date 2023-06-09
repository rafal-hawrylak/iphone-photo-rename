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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IPhonePhotoRename {

    private static Logger logger = LogManager.getLogger(IPhonePhotoRename.class);
    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
    private static Pattern supportedImageFileNamePattern = Pattern.compile("^img_\\d+\\.jpeg$");
    private static Pattern supportedMovieFileNamePattern = Pattern.compile("^img_\\d+\\.mov$");

    public static void main(String[] args) {
        if (args.length == 0) {
            logger.error("usage:\r\nIPhonePhotoRename directory [earliestValidDate]");
            return;
        }
        String directory = args[0];
        boolean dryRun = args.length >= 2 ? Boolean.parseBoolean(args[1]) : true;
        Optional<Date> earliestValidDate = args.length >= 3 ? convertToDate(args[2]) : Optional.empty();
        new IPhonePhotoRename().run(directory, dryRun, earliestValidDate);
    }

    private static Optional<Date> convertToDate(String date) {
        try {
            return Optional.ofNullable(formatter.parse(date));
        } catch (ParseException e) {
            return Optional.empty();
        }
    }

    private void run(String directory, boolean dryRun, Optional<Date> earliestValidDate) {
        logger.info("IPhone Photo Rename (reads data from EXIF of jpeg");
        RenameInfo previousImage = null;
        for (String file : listFiles(directory)) {
            var fileName = parseFileName(glue(directory, file, Optional.empty()));
            if (isSupportedFileName(fileName, supportedImageFileNamePattern)) {
                var dateFromEXIF = readDateFromFile(fileName);
                validateDate(dateFromEXIF, earliestValidDate, fileName);
                var newName = getFirstFreeName(fileName, dateFromEXIF);
                previousImage = new RenameInfo(fileName, dateFromEXIF, newName);
                boolean moved = move(fileName, newName, dryRun);
                logger.info("photo | moved=" + moved + "; " + fileName.name() + " -> " + dateFromEXIF + " -> " + newName);
            } else if (isSupportedFileName(fileName, supportedMovieFileNamePattern)) {
                if (Objects.nonNull(previousImage)) {
                    var newName = getFirstFreeName(fileName, previousImage.date());
                    boolean moved = move(fileName, newName, dryRun);
                    logger.info("video | moved=" + moved + "; " + fileName.name() + " -> " + previousImage.date() + " -> " + newName);
                } else {
                    logger.warn("could not process movie file " + fileName);
                }
            } else {
                logger.warn("Not supported file name " + file);
            }
        }
    }

    private boolean move(FileName fileName, String newName, boolean dryRun) {
        if (dryRun) {
            return false;
        }
        String targetPath = newPath(fileName, newName);
        return fileName.file().renameTo(new File(targetPath));
    }

    private boolean isSupportedFileName(FileName fileName, Pattern supportedFileNamePattern) {
        return supportedFileNamePattern.matcher(fileName.name().toLowerCase()).matches();
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
        } while (fileExists(newPath(fileName, newName)));
        return newName;
    }

    private String newPath(FileName fileName, String newName) {
        return glue(fileName.path(), newName, fileName.extension());
    }

    private boolean fileExists(String fileName) {
        Path path = Paths.get(fileName);
        return Files.exists(path);
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
            return directory.getDateOriginal();
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