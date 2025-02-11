package com.eduva.eduva.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PDFImageExtractorService {

    private static final String IMAGE_FORMAT = "JPEG";
    private static final String IMAGE_EXTENSION = ".jpg";

    /**
     * Extracts images from a PDF file into a temporary directory
     * @param pdfFile MultipartFile containing the PDF
     * @return List of paths to temporary image files
     * @throws IOException if there's an error processing the PDF
     */
    public List<Path> extractImages(MultipartFile pdfFile) throws IOException {
        validateInput(pdfFile);
        Path tempDir = Files.createTempDirectory("pdf-images-"); // Move tempDir here

        try {
            List<Path> extractedImagePaths = new ArrayList<>();
            try (InputStream inputStream = pdfFile.getInputStream();
                 PDDocument document = PDDocument.load(inputStream)) {
                for (int i = 0; i < document.getNumberOfPages(); i++) {
                    extractedImagePaths.addAll(extractImagesFromPage(document.getPage(i), i, tempDir)); // Pass tempDir
                }
                return extractedImagePaths;
            }
        } catch (IOException e) {
            cleanup(tempDir); // Clean up the specific tempDir for the current request
            throw new IOException("Error processing PDF file: " + e.getMessage(), e);
        }
    }

    private List<Path> extractImagesFromPage(PDPage page, int pageNumber, Path tempDir) throws IOException {
        List<Path> pageImagePaths = new ArrayList<>();
        PDResources resources = page.getResources();

        if (resources == null) {
            return pageImagePaths;
        }

        Iterable<COSName> xObjectNames = resources.getXObjectNames();
        for (COSName xObjectName : xObjectNames) {
            PDXObject xObject = resources.getXObject(xObjectName);

            if (xObject instanceof PDImageXObject) {
                Path imagePath = saveImage((PDImageXObject) xObject, pageNumber, xObjectName.getName(), tempDir); // Pass tempDir
                pageImagePaths.add(imagePath);
            }
        }

        return pageImagePaths;
    }

    private Path saveImage(PDImageXObject image, int pageNumber, String objectName, Path tempDir)
            throws IOException {
        BufferedImage bufferedImage = image.getImage();
        String filename = generateImageFilename(pageNumber, objectName);
        Path imagePath = tempDir.resolve(filename);

        ImageIO.write(bufferedImage, IMAGE_FORMAT, imagePath.toFile());
        return imagePath;
    }

    /**
     * Generates a unique filename for the extracted image
     */
    private String generateImageFilename(int pageNumber, String objectName) {
        return String.format("image-%d-%s-%s%s",
                pageNumber,
                objectName,
                UUID.randomUUID().toString().substring(0, 8),
                IMAGE_EXTENSION);
    }

    /**
     * Validates the input parameters
     */
    private void validateInput(MultipartFile pdfFile) throws IOException {
        if (pdfFile == null || pdfFile.isEmpty()) {
            throw new IOException("PDF file cannot be empty");
        }

        String filename = StringUtils.cleanPath(pdfFile.getOriginalFilename());
        if (filename.toLowerCase().endsWith(".pdf")) {
            String contentType = pdfFile.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                throw new IOException("File must be a PDF document");
            }
        } else {
            throw new IOException("File must have .pdf extension");
        }
    }

    /**
     * Cleans up temporary files and directory
     */
    public void cleanup(Path tempDir) {
        if (tempDir != null) {
            try {
                FileSystemUtils.deleteRecursively(tempDir);
            } catch (IOException e) {
                // Log error but don't throw - this is cleanup code
                System.err.println("Error cleaning up temporary files: " + e.getMessage());
            }
        }
    }
}