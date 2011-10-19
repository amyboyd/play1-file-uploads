package com.alienmegacorp.fileuploads;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.persistence.*;
import org.apache.commons.lang.time.DateFormatUtils;
import play.data.validation.Match;
import play.data.validation.MaxSize;
import play.data.validation.Min;
import play.data.validation.MinSize;
import play.data.validation.Required;
import play.db.jpa.Model;
import play.libs.Files;
import play.libs.IO;
import static com.alienmegacorp.fileuploads.Helper.*;

/**
 * File uploads.
 *
 * <p>application.conf must set the properties "aws.accessKey", "aws.secretKey",
 * and "application.amazonS3Bucket" must also be set.
 * If amazonS3Bucket is "false", the local file system will be used instead of S3.
 * If not "false", the value will be the name of the S3 bucket.
 *
 * @author Michael Boyd <michael@alienmegacorp.com>
 * @version r1
 */
@MappedSuperclass
abstract public class AbstractUploadedFile extends Model {
    /**
     * Name of the uploaded file. May be null.
     */
    public String originalFilename;

    /**
     * Location of the copied file, if {@link #setURLtoCopy(java.lang.String)}
     * or {@link #setURLtoCopy(java.net.URL)} was used.
     */
    @Column(name = "original_url")
    public String originalURL;

    @Required
    @Column(name = "file_path")
    public String path;

    /**
     * The file's extension without the leading dot, and not null.
     */
    @Required
    @Match(value = "^[a-zA-Z0-9]+$", message = "File extension is invalid")
    @MinSize(2)
    @MaxSize(4)
    public String extension;

    /**
     * File size in bytes.
     */
    @Required
    @Min(0)
    public long fileSize;

    @Temporal(value = TemporalType.TIMESTAMP)
    public Date createdAt;

    protected abstract String getFolder();

    public File getLocalFile() {
        return new File(DIR_ROOT.getPath() + "/" + path);
    }

    public String getURL() {
        return Helper.getBaseURL() + path;
    }

    public final void setURLtoCopy(final String url) {
        try {
            setURLtoCopy(new URL(url));
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Malformed URL: " + url, ex);
        }
    }

    public final void setURLtoCopy(final URL url) {
        try {
            // Copy the URL file to a local temporary file, and continue with that file.
            final File tmpFile = File.createTempFile("copy-file-", null);
            IO.copy(url.openStream(), new FileOutputStream(tmpFile));

            this.originalURL = url.toString();
            this.fileSize = tmpFile.length();
            this.extension = url.toString().substring(url.toString().lastIndexOf('.') + 1);

            putFileInCorrectPlace(tmpFile);

            tmpFile.delete();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void setFixtureFile(final String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Fixture filename must not be null");
        }

        final File file = play.Play.getFile("public/fixtures/" + filename);
        if (!file.exists()) {
            throw new IllegalArgumentException("Fixture file doesn't exist: " + filename);
        }

        this.originalFilename = filename;
        this.fileSize = file.length();
        this.extension = filename.substring(filename.lastIndexOf('.') + 1);

        putFileInCorrectPlace(file);
    }

    public void setFileToCopy(File file) {
        if (file == null) {
            throw new IllegalArgumentException("File must not be null");
        } else if (!file.exists()) {
            throw new IllegalArgumentException("File doesn't exist: " + file);
        }

        this.originalFilename = file.getName();
        this.fileSize = file.length();
        this.extension = this.originalFilename.substring(this.originalFilename.lastIndexOf('.') + 1);

        putFileInCorrectPlace(file);
    }

    private void putFileInCorrectPlace(File file) {
        if (file == null) {
            throw new IllegalArgumentException("File must not be null");
        } else if (!file.exists()) {
            throw new IllegalArgumentException("File doesn't exist: " + file);
        }

        if (this.path != null) {
            throw new IllegalStateException("Using an AbstractUploadedFile that already has a path (create a new object instead of reusing");
        }

        // Generate a path name.
        this.path = getFolder()
                + "/" + DateFormatUtils.format(Calendar.getInstance(), "MMMy")
                + "/" + UUID.randomUUID().toString()
                + "." + extension;

        // Copy the file to its target path, and put it on S3 if enabled.
        final File target = getLocalFile();
        target.getParentFile().mkdirs();
        Files.copy(file, target);

        if (USE_S3) {
            final PutObjectRequest request = new PutObjectRequest(AWS_BUCKET_NAME, path, target);
            request.setCannedAcl(CannedAccessControlList.PublicRead);
            S3_CLIENT.putObject(request);
        }
    }

    @PrePersist
    protected void prePersist() {
        if (createdAt == null) {
            createdAt = new Date();
        }
    }
}
