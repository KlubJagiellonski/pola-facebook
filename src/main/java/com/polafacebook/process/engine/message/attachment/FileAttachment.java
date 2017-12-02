package com.polafacebook.process.engine.message.attachment;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Piotr on 24.09.2017.
 */
public class FileAttachment extends Attachment {

    private final String file;

    public FileAttachment(String file, Attachment.Type type) {
        super(type);
        this.file = file;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(this.file);
    }

    @Override
    public String toString() {
        return "FileAttachment{" +
                "file='" + file + '\'' +
                ", type=" + type +
                '}';
    }
}
