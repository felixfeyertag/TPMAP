/*
 * Copyright (C) 2020 Felix Feyertag <felix.feyertag@ndm.ox.ac.uk>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.chembiohub.tpmap.dstruct.io;

import com.chembiohub.tpmap.dstruct.Protein1D;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ChartExportWizard
 *
 * Exports charts from a 1D TP analysis to a PDF file.
 *
 * @author felixfeyertag
 */
public class ChartExportWizard {

    public ChartExportWizard(List<Protein1D> proteins, ObservableList<String> tempLabels, ObservableList<String> repLabels, final Stage primaryStage) {

        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter fcefpdf = new FileChooser.ExtensionFilter("Portable Document Format (PDF)", "*.pdf");
        fc.getExtensionFilters().addAll(fcefpdf);
        
        File f = fc.showSaveDialog(primaryStage);
        
        if(f!=null) {

            try {

                PDDocument doc = new PDDocument();

                for(Protein1D p : proteins) {

                    if(p.getSelected()) {

                        PDPage page = new PDPage(new PDRectangle(800, 600));
                        doc.addPage(page);

                        BufferedImage image = p.createChart(tempLabels, repLabels).createBufferedImage(640, 480);

                        PDImageXObject imageXObject = LosslessFactory.createFromImage(doc, image);

                        try (PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                            contentStream.drawImage(imageXObject, 80, 60, image.getWidth(), image.getHeight());
                        }

                    }
                }

                doc.save(f);
                doc.close();

            } catch (IOException ex) {

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.initOwner(primaryStage);
                alert.setTitle("File IO Exception");
                alert.setHeaderText("File IO Exception");
                alert.setContentText("Could not write file: " + f.getPath());

                Logger.getLogger(ChartExportWizard.class.getName()).log(Level.SEVERE, null, ex);

                alert.showAndWait();
            }
        }
    }
}

