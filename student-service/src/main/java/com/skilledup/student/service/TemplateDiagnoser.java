package com.skilledup.student.service;

import org.apache.poi.xslf.usermodel.*;
import java.io.FileInputStream;
import java.io.File;
import java.io.PrintWriter;

public class TemplateDiagnoser {
    public static void main(String[] args) throws Exception {
        String path = "C:/Users/Admin/Desktop/Intership/Intership/internship-platform-microservices/PPT/OfferLetterTemplate.pptx";
        File outFile = new File("diagnosis.txt");
        try (PrintWriter writer = new PrintWriter(outFile)) {
            writer.println("Inspecting file: " + path);

            try (FileInputStream fis = new FileInputStream(new File(path));
                    XMLSlideShow ppt = new XMLSlideShow(fis)) {

                for (XSLFSlide slide : ppt.getSlides()) {
                    writer.println("--- Slide ---");
                    for (XSLFShape shape : slide.getShapes()) {
                        String name = shape.getShapeName();
                        java.awt.geom.Rectangle2D anchor = shape.getAnchor();
                        String type = shape.getClass().getSimpleName();
                        writer.printf("Shape: %s, Type: %s, X: %.2f, Y: %.2f, W: %.2f, H: %.2f%n", name, type,
                                anchor.getX(), anchor.getY(), anchor.getWidth(), anchor.getHeight());

                        if (shape instanceof XSLFTextShape) {
                            XSLFTextShape textShape = (XSLFTextShape) shape;
                            writer.println("  Text: [" + textShape.getText() + "]");
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(writer);
            }
        }
        System.out.println("Diagnosis written to diagnosis.txt");
    }
}
