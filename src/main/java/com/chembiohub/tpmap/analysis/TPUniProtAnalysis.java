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
package com.chembiohub.tpmap.analysis;

import com.chembiohub.tpmap.TPMAP;
import com.chembiohub.tpmap.dstruct.Protein;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.chembiohub.tpmap.ui.TPWebView;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

/**
 * TPUniProtAnalysis
 *
 * Open UniProt pages for selected proteins
 *
 * @author felixfeyertag
 */
public class TPUniProtAnalysis {

    /**
     *
     * @param proteins List of proteins
     * @param tpTabPane Pane on which new tab will be loaded
     * @param primaryStage Parent stage
     */
    public static void uniprotEntry(ObservableList<? extends Protein> proteins, TabPane tpTabPane, Stage primaryStage) {

        Alert alert = new Alert(AlertType.INFORMATION, "UniProt pages for " + proteins.size() +
                " selected proteins will be loaded. Do you wish to proceed?", ButtonType.YES, ButtonType.NO);
        alert.initOwner(primaryStage);
        alert.showAndWait();
        if(alert.getResult()==ButtonType.NO) {
            return;
        }

        proteins.forEach( (p) -> {

            final String uniprotURL = "https://www.uniprot.org/uniprot/" + p.getAccession();

            TPWebView tpWebView = new TPWebView(uniprotURL);
            tpWebView.addToWhitelist("https://www.uniprot.org");


            Tab uniprotTab = new Tab("UniProt " + p.getAccession());

            uniprotTab.setContent(tpWebView.createBrowserPane());

            tpTabPane.getTabs().add(uniprotTab);
            tpTabPane.getSelectionModel().select(uniprotTab);

        });

    }
}
