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

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.chembiohub.tpmap.dstruct.Protein1D;
import com.chembiohub.tpmap.dstruct.Protein2D;
import com.chembiohub.tpmap.dstruct.io.FileExportWizard;
import com.chembiohub.tpmap.ui.TPWebView;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

/**
 * TPStringAnalysis
 *
 * Performs a String PPI and enrichment analysis by uploading selected proteins to the string-db.org server.
 *
 * - Network analysis
 *        Nodes can be highlighted in the network to show stabilised (positive score) and destabilised (negative score)
 *        proteins in the network using the payload mechanism offered by the String server.
 *
 * @author felixfeyertag
 */
public class TPStringAnalysis {

    private static final String speciesPath = "/com/chembiohub/tpmap/analysis/analysispane/species.v10.5.txt";

    public TPStringAnalysis() {

    }

    /**
     *
     * @param proteins List of proteins
     * @param species Species identifier
     * @param primaryStage Parent stage
     * @param tpTabPane Pane on which to open new string tab
     */
    public void stringNetworkImage(ObservableList<? extends Protein> proteins, StringProperty species, Stage primaryStage, TabPane tpTabPane) {

        IntegerProperty selectedProteins = new SimpleIntegerProperty((int)
            proteins.stream().filter(Protein::getSelected).count());

        if(selectedProteins.getValue()==0) {
            Alert noProteinsSelectedAlert = new Alert(Alert.AlertType.ERROR);
            noProteinsSelectedAlert.initOwner(primaryStage);
            noProteinsSelectedAlert.setTitle("No Proteins Selected");
            noProteinsSelectedAlert.setHeaderText("No Proteins Selected. Please select proteins for String analysis using the checkboxes.");
            noProteinsSelectedAlert.showAndWait();
            return;
        }

        if(species.isEmpty().getValue()) {
            species.setValue("Homo sapiens");
        }

        Dialog<Pair<String, Boolean>> speciesSelectorDialog = this.speciesSelector(species.getValue(), selectedProteins.getValue());
        speciesSelectorDialog.initOwner(primaryStage);

        Optional<Pair<String, Boolean>> result = speciesSelectorDialog.showAndWait();

        result.ifPresent(stringAnalysis -> {

            try {

                List<String> idList = new ArrayList<>();
                List<String> colorList = new ArrayList<>();

                proteins.stream().filter(Protein::getSelected).peek((p) -> idList.add(p.getAccession().replaceAll("[-;].*$", ""))).forEachOrdered((p) -> {
                    if(p instanceof Protein1D) {
                        if (((Protein1D) p).getMeanTM() < -0.00) {
                            colorList.add("#FF0000 ");
                        } else if (((Protein1D) p).getMeanTM() > 0.00) {
                            colorList.add("#00FF00 ");
                        } else {
                            colorList.add("#FFFFFF ");
                        }
                    }
                    else {
                        if (p.getScore() < -0.00) {
                            colorList.add("#FF0000 ");
                        } else if (p.getScore() > 0.00) {
                            colorList.add("#00FF00 ");
                        } else {
                            colorList.add("#FFFFFF ");
                        }
                    }
                });

                URL stringInput = new URL("https://string-db.org/api/tsv/get_string_ids?identifiers=".concat(String.join("%0D", idList)));

                Logger.getLogger(TPMAP.class.getName()).log(Level.INFO, stringInput.toString());

                URLConnection connection = stringInput.openConnection();
                List<String> stringIdList;
                List<String> stringColorList;
                String l;
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    stringIdList = new ArrayList<>();
                    stringColorList = new ArrayList<>();
                    boolean header = true;
                    while ((l = in.readLine()) != null) {
                        if (!header) {
                            String[] lineVals = l.split("\t");
                            if (lineVals.length>=3 && lineVals[2].equals(stringAnalysis.getKey())) {
                                stringIdList.add(lineVals[1]);
                                stringColorList.add(colorList.get(Integer.valueOf(lineVals[0])));
                            }
                        } else {
                            header = false;
                        }
                    }
                }


                List<NameValuePair> params = new ArrayList<>();

                String stringIds = String.join(" ", stringIdList);
                String stringColors = String.join(" ", stringColorList);

                HttpPost payloadpost = new HttpPost("https://version11.string-db.org/newstring_cgi/webservices/post_payload.pl");

                params.add(new BasicNameValuePair("species", stringAnalysis.getKey()));
                params.add(new BasicNameValuePair("identifiers", stringIds));

                if(stringAnalysis.getValue()) {
                    params.add(new BasicNameValuePair("colors", stringColors));
                }

                payloadpost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

                HttpClient httpclient = HttpClients.createDefault();

                Logger.getLogger(TPMAP.class.getName()).log(Level.INFO, payloadpost.toString());

                HttpResponse response = httpclient.execute(payloadpost);

                HttpEntity entity = response.getEntity();

                StringBuilder payload_id = new StringBuilder();

                if (entity != null) {
                    try (InputStream is = entity.getContent()) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(is));

                        while ((l = br.readLine()) != null) {
                            payload_id.append(l);
                        }
                    }
                }

                String stringUrl = "";

                if (stringIdList.size()==1) {
                    stringUrl = "https://version11.string-db.org/cgi/network.pl?network_flavor=evidence&species="
                            + stringAnalysis.getKey() + "&internal_payload_id=" + payload_id + "&identifier="
                            + stringIdList.get(0);
                } else {
                    stringUrl = "https://version11.string-db.org/cgi/network.pl?network_flavor=evidence&limit=0&species="
                            + stringAnalysis.getKey() + "&internal_payload_id=" + payload_id + "&identifiers="
                            + String.join("%0D", stringIdList);
                }

                Logger.getLogger(TPMAP.class.getName()).log(Level.INFO, stringUrl);

                TPWebView tpWebView = new TPWebView(stringUrl);
                tpWebView.addToWhitelist("https://version11.string-db.org");
                tpWebView.addToWhitelist("https://www.string-db.org");

                Tab stringTab = new Tab("String Network");
                stringTab.setContent(tpWebView.createBrowserPane());

                tpTabPane.getTabs().add(stringTab);
                tpTabPane.getSelectionModel().select(stringTab);

            } catch (Exception ex) {
                Alert stringAlert = new Alert(Alert.AlertType.ERROR);
                stringAlert.setHeaderText("Unable to load String Network");
                stringAlert.setContentText(ex.getMessage());
                stringAlert.showAndWait();
                Logger.getLogger(TPStringAnalysis.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }

    private Dialog<Pair<String,Boolean>> speciesSelector(String taxonomy, int numProteins) {

        Map<String,String> taxidMap = new HashMap<>();

        StringProperty selectedTaxId = new SimpleStringProperty();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResource(speciesPath).openStream()))) {
            br.lines().forEach((String line) -> {
                if(line.startsWith("#")) {
                    return;
                }
                String[] taxonomyLine = line.split("\t");
                if(!taxonomyLine[1].equals("core")) {
                    return;
                }
                String t = taxonomyLine[3] + " (taxid:" + taxonomyLine[0] + ")";
                taxidMap.put(t, taxonomyLine[0]);
                if(taxonomy!=null && taxonomyLine[3].toLowerCase().equals(taxonomy.toLowerCase())) {
                    selectedTaxId.set(t);
                }
            });
        } catch (IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("No taxonomy list");
            alert.setHeaderText("No taxonomy list");
            alert.showAndWait();
        }

        ObservableList<String> taxidList = FXCollections.observableArrayList(taxidMap.keySet()).sorted();

        Dialog<Pair<String,Boolean>> speciesSelectDialog = new Dialog<>();
        speciesSelectDialog.setTitle("String Analysis");
        speciesSelectDialog.setHeaderText("Uploading " + numProteins + " proteins for String analysis.");

        speciesSelectDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox speciesBox = new VBox();
        speciesBox.setSpacing(10);
        Label uniprotLabel = new Label("Important: Accession IDs should refer to UniProt identifiers for this functionality to work.");
        Label speciesLabel = new Label("Select species:");
        ComboBox<String> taxidComboBox = new ComboBox<>(taxidList);
        taxidComboBox.setValue(selectedTaxId.getValue());
        CheckBox highlightCheckBox = new CheckBox("Highlight stabilized and destabilized proteins");
        highlightCheckBox.setSelected(true);

        speciesBox.getChildren().addAll(speciesLabel,taxidComboBox,highlightCheckBox,uniprotLabel);

        speciesSelectDialog.getDialogPane().setContent(speciesBox);

        Platform.runLater(taxidComboBox::requestFocus);

        speciesSelectDialog.setResultConverter( button -> {
            if(button == ButtonType.OK) {
                return new Pair<>(taxidMap.get(taxidComboBox.getValue()), highlightCheckBox.isSelected());
            }
            return null;
        });

        return speciesSelectDialog;
    }

    public void stringFunctionalEnrichment(ObservableList<? extends Protein> proteins, Stage primaryStage, TabPane tpTabPane) {

        IntegerProperty selectedProteins = new SimpleIntegerProperty((int)
                proteins.stream().filter(Protein::getSelected).count());

        if(selectedProteins.getValue()==0) {
            Alert noProteinsSelectedAlert = new Alert(Alert.AlertType.ERROR);
            noProteinsSelectedAlert.initOwner(primaryStage);
            noProteinsSelectedAlert.setTitle("No Proteins Selected");
            noProteinsSelectedAlert.setHeaderText("No Proteins Selected. Please select proteins for String analysis using the checkboxes.");
            noProteinsSelectedAlert.showAndWait();
            return;
        }

        StringBuilder proteinList = new StringBuilder();
        boolean flag = false;
        for(Protein p : proteins) {
            if(p.getSelected()) {
                if(flag) {
                    proteinList.append("%0d");
                }
                else {
                    flag = true;
                }
                proteinList.append(p.getAccession());
            }
        }

        final String stringUrl = "https://string-db.org/api/tsv/enrichment?species=9606&identifiers=" + proteinList;

        HttpPost stringPost = new HttpPost(stringUrl);

        HttpClient httpclient = HttpClients.createDefault();

        HttpResponse response;

        StringBuilder enrichmentTable = new StringBuilder();
        StringBuilder enrichmentTableHTML =
                new StringBuilder("<!DOCTYPE html>\n" +
                        "<html>\n" +
                        "<head>\n" +
                        "<title>String Enrichment Analysis</title>\n" +
                        "<script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"> </script>\n" +
                        "<script type=\"text/javascript\">\n" +
                        "google.charts.load('current', {'packages':['table']});\n" +
                        "google.charts.setOnLoadCallback(drawTable);\n" +
                        "function drawTable() {\n" +
                        "var data = new google.visualization.DataTable();\n");

        try {
            response = httpclient.execute(stringPost);

            HttpEntity entity = response.getEntity();

            String l;

            if (entity != null) {
                try (InputStream is = entity.getContent()) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));

                    boolean head = true;
                    boolean first = true;
                    List<Integer> numberCols = new Vector<>();

                    while ((l = br.readLine()) != null) {
                        enrichmentTable.append(l).append("\n");
                        if(head) {
                            head = false;
                            String[] header = l.split("\t");
                            for(int i=0; i<header.length; i++) {
                                if(header[i].startsWith("number") || header[i].startsWith("p_value") || header[i].startsWith("fdr")) {
                                    enrichmentTableHTML.append("data.addColumn('number', '").append(header[i]).append("');\n");
                                    numberCols.add(i);
                                }
                                else {
                                    enrichmentTableHTML.append("data.addColumn('string', '").append(header[i]).append("');\n");
                                }
                            }
                            enrichmentTableHTML.append("data.addRows([\n");
                        }
                        else {
                            if(first) { first = false; }
                            else { enrichmentTableHTML.append(",\n"); }
                            String[] columns = l.split("\t");
                            if(numberCols.contains(0)) {
                                enrichmentTableHTML.append("[ ").append(columns[0].replaceAll("'", "").replaceAll("_", " "));
                            }
                            else {
                                enrichmentTableHTML.append("[ '").append(columns[0].replaceAll("'", "")).append("'");
                            }
                            if(columns.length > 0) {
                                for(int i=1; i<columns.length; i++) {
                                    if(numberCols.contains(i)) {
                                        enrichmentTableHTML.append(", ").append(columns[i].replaceAll("'", ""));
                                    }
                                    else {
                                        enrichmentTableHTML.append(", '").append(columns[i].replaceAll("'", "")).append("'");
                                    }
                                }
                            }
                            enrichmentTableHTML.append(" ]");
                        }
                    }
                    enrichmentTableHTML.append("]);\n" + "var table = new google.visualization.Table(document.getElementById('table_div'));\n" + "table.draw(data, {showRowNumber: true, width: '100%', height: '100%'}); }\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        enrichmentTableHTML.append("</script>\n" + "</head>\n" + "<body><div id=\"table_div\"> </div></body>\n" + "</html>\n");

        Tab stringTab = new Tab("Functional Enrichment");

        BorderPane stringTabContent = new BorderPane();
        Button browswerButton = new Button("Open in Web Browser");
        Button saveButton = new Button("Save Table");
        TextField urlField = new TextField(stringUrl);
        HBox container = new HBox(urlField, browswerButton, saveButton);
        HBox.setHgrow(urlField, Priority.ALWAYS);
        stringTabContent.setTop(container);

        WebView webView = new WebView();

        webView.getEngine().loadContent(enrichmentTableHTML.toString());

        stringTabContent.setCenter(webView);

        stringTab.setContent(stringTabContent);
        tpTabPane.getTabs().add(stringTab);
        tpTabPane.getSelectionModel().select(stringTab);

        browswerButton.setOnAction( event -> {
            if (Desktop.isDesktopSupported()) {
                try {
                    try {
                        Desktop.getDesktop().browse(new URI(stringUrl));
                    } catch (IOException ex) {
                        Alert stringAlert = new Alert(Alert.AlertType.ERROR);
                        stringAlert.setHeaderText("Unable to load String Network in web browser");
                        stringAlert.setContentText(ex.getMessage());
                        stringAlert.showAndWait();
                        Logger.getLogger(TPMAP.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } catch (URISyntaxException ex) {
                    Alert stringAlert = new Alert(Alert.AlertType.ERROR);
                    stringAlert.setHeaderText("Unable to load String Network in web browser");
                    stringAlert.setContentText(ex.getMessage());
                    stringAlert.showAndWait();
                    Logger.getLogger(TPMAP.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        final String enrichmentTableExport = enrichmentTable.toString();

        saveButton.setOnAction( event -> {
            FileExportWizard exporter = new FileExportWizard(enrichmentTableExport, primaryStage, false, -1);
        });

    }

}
