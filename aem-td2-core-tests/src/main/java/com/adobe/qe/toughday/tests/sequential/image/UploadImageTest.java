/*
Copyright 2015 Adobe. All rights reserved.
This file is licensed to you under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License. You may obtain a copy
of the License at http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under
the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
OF ANY KIND, either express or implied. See the License for the specific language
governing permissions and limitations under the License.
*/
package com.adobe.qe.toughday.tests.sequential.image;

import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.annotations.Description;
import com.adobe.qe.toughday.api.annotations.Tag;
import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.api.annotations.After;
import com.adobe.qe.toughday.api.annotations.Before;
import com.adobe.qe.toughday.tests.composite.AuthoringTest;
import com.adobe.qe.toughday.tests.sequential.AEMTestBase;
import com.adobe.qe.toughday.tests.samplecontent.SampleContent;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.Constants;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Tag(tags = { "author" })
@Description(desc = "Test for uploading assets under the same path."  +
        " Due to OAK limitations, performance will decrease over time." +
        " If you are not looking for this specific scenario, please consider using CreateAssetTreeTest.")
public class UploadImageTest extends AEMTestBase {

    private String fileName = AuthoringTest.DEFAULT_ASSET_NAME;
    private String resourcePath = AuthoringTest.DEFAULT_RESOURCE_PATH;
    private String mimeType = AuthoringTest.DEFAULT_MIME_TYPE; //TODO do we really need this?
    private String parentPath = SampleContent.TOUGHDAY_DAM_FOLDER;

    public static ThreadLocal<File> lastCreated = new ThreadLocal<>();
    public static Random rnd = new Random();
    public static final AtomicInteger nextNumber = new AtomicInteger(0);

    private BufferedImage img;
    private InputStream imageStream;

    public UploadImageTest() {}

    private UploadImageTest(String fileName, String resourcePath, String mimeType, String parentPath) {
        this.resourcePath = resourcePath;
        this.mimeType = mimeType;
        this.parentPath = parentPath;
        this.fileName = fileName;
    }

    @Before
    private void before() throws ClientException, IOException {
        String nextFileName = fileName + nextNumber.getAndIncrement() + ".png";

        // image processing: read, add noise and save to file
        imageStream = UploadImageTest.getImage(this.resourcePath);
        img = ImageIO.read(imageStream);
        addNoise(img);
        File last = new File(workspace, nextFileName);
        ImageIO.write(img, "png", last);
        lastCreated.set(last);
    }

    @Override
    public void test() throws Throwable {
        MultipartEntity multiPartEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        try {
            multiPartEntity.addPart("file", new FileBody(lastCreated.get()));

            multiPartEntity.addPart(Constants.PARAMETER_CHARSET, new StringBody(Constants.CHARSET_UTF8));
            multiPartEntity.addPart("fileName", new StringBody(lastCreated.get().getName(),
                            Charset.forName(Constants.CHARSET_UTF8)));
        } catch (UnsupportedEncodingException e) {
            throw new ClientException("Could not create Multipart Post!", e);
        }

        String currentParentPath = StringUtils.stripEnd(getCommunication("parentPath", parentPath), "/");

        try {
            logger().debug("{}: Trying to upload image={}{}", Thread.currentThread().getName(), currentParentPath, lastCreated.get().getName());

            benchmark().measure(this, "UploadImage", getDefaultClient()).doPost(currentParentPath + ".createasset.html", multiPartEntity, HttpStatus.SC_OK);
        } catch (Throwable e) {
            logger().warn("{}: Failed to upload image={}{}", Thread.currentThread().getName(), currentParentPath, lastCreated.get().getName());
            logger().debug(Thread.currentThread().getName() + ": ERROR: ", e);

            throw e;
        }

        logger().debug("{}: Successfully uploaded image={}{}", Thread.currentThread().getName(), currentParentPath, lastCreated.get().getName());
    }

    @After
    private void after() {
        if (!lastCreated.get().delete()) {
            throw new RuntimeException("Cannot delete file " + lastCreated.get().getName());
        }
    }


    @Override
    public AbstractTest newInstance() {
        return new UploadImageTest(fileName, resourcePath, mimeType, parentPath);
    }


    @ConfigArgSet(required = false, defaultValue = AuthoringTest.DEFAULT_ASSET_NAME, desc = "The name of the file to be created")
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @ConfigArgGet
    public String getFileName() {
        return this.fileName;
    }

    @ConfigArgSet(required = false, defaultValue = AuthoringTest.DEFAULT_RESOURCE_PATH,
            desc = "The image resource path either in the classpath or the filesystem")
    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @ConfigArgGet
    public String getResourcePath() {
        return this.resourcePath;
    }

    @ConfigArgSet(required = false, defaultValue = AuthoringTest.DEFAULT_MIME_TYPE, desc = "The mime type of the uploaded image")
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @ConfigArgGet
    public String getMimeType() {
        return this.mimeType;
    }

    @ConfigArgSet(required = false, defaultValue = SampleContent.TOUGHDAY_DAM_FOLDER, desc = "The path where the image is uploaded")
    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    @ConfigArgGet
    public String getParentPath() {
        return this.parentPath;
    }

    /**
     * Get an InputStream of an image, either from the filesystem or from the resources.
     * @param filename
     * @return
     * @throws ClientException if filename is not found either on the filesystem or in the resources
     */
    public static InputStream getImage(String filename) throws ClientException {
        InputStream in;
        try {
            in = new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            // try the classpath
            in = UploadImageTest.class.getClassLoader().getResourceAsStream(filename);
            if (null == in) {
                throw new ClientException("Could not find " + filename + " in classpath or in path");
            }
        }
        return in;
    }

    /**
     * Add noise to a {@see BufferedImage}
     * @param img
     */
    public static void addNoise(BufferedImage img) {
        for (int i = 0; i < 200; i++) {
            int x = rnd.nextInt(img.getWidth());
            int y = rnd.nextInt(img.getHeight());
            img.setRGB(x, y, Color.CYAN.getRGB());
        }
    }
}
