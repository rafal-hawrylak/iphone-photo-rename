package org.hawrylak.iphonephotorename;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IPhonePhotoRename {

    static Logger logger = LogManager.getLogger(IPhonePhotoRename.class);

    public static void main(String[] args) {
        new IPhonePhotoRename().run(args[0]);
    }

    private void run(String sampleFile) {
        logger.info("IPhone Photo Rename (reads data from EXIF of jpeg");
        var date = readDateFromFile(sampleFile);
        logger.info(sampleFile + " -> " + date);
    }

    private Date readDateFromFile(String fileName) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new File(fileName));
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            return directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
        } catch (ImageProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printAllMetadataFromFile(String sampleFile) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new File(sampleFile));
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