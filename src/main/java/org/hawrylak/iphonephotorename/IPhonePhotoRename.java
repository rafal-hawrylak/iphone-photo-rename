package org.hawrylak.iphonephotorename;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import java.io.File;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IPhonePhotoRename {

    static Logger logger = LogManager.getLogger(IPhonePhotoRename.class);

    public static void main(String[] args) {
        new IPhonePhotoRename().run(args[0]);
    }

    private void run(String sampleFile) {
        logger.info("IPhone Photo Rename (reads data from EXIF of jpeg");
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new File(sampleFile));
            for (Directory directory : metadata.getDirectories()) {
                logger.info(directory.getName());
            }
        } catch (ImageProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}