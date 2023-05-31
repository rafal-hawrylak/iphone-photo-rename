package org.hawrylak.iphonephotorename;

import java.io.File;

record FileName (File file, String path, String name, java.util.Optional<String> nameWithoutExtension, java.util.Optional<String> extension) {

}
