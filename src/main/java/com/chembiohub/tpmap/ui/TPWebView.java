/*
 * Copyright (C) 2020 Felix Feyertag <felix.feyertag@ndm.ox.ac.uk>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.chembiohub.tpmap.ui;

import com.chembiohub.tpmap.TPMAP;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Worker;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.apache.http.client.utils.URIBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
//import org.w3c.dom.html.HTMLButtonElement;
import org.w3c.dom.html.HTMLButtonElement;

import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TPWebView
 *
 * The TPWebView class is used to create an HTML browser in TPMAP. The web browser can be used to access local and
 * remote HTML documents, a whitelist is used to restrict access to trusted HTTP domains.
 */
public class TPWebView {

    private final String url;
    private final List<String> whitelist;

    /**
     * Instantiate a TPWebView to load a URL
     *
     * @param url URL to be loaded
     */
    public TPWebView(String url) {
        this.url = url;
        this.whitelist = new LinkedList<>();
    }

    /**
     * After instantiating a TPWebView, trusted URL domains can be added to the whitelist. If the user tries to access
     * a domain that is not whitelisted, an error page will be displayed with the option of loading it in the system
     * default web browser.
     *
     * @param url url to be added
     */
    public void addToWhitelist(String url) {
        try {
            whitelist.add(new URIBuilder(url).toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a JavaFX WebView instance that loads a URL
     *
     * @return webView window
     */
    public WebView createWebView() {

        WebView webView = new WebView();

        WebEngine engine = webView.getEngine();

        final EventListener externalLinkListener = new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                EventTarget target = evt.getCurrentTarget();
                HTMLButtonElement element = (HTMLButtonElement) target;
                String href = element.getName();
                if(href.toLowerCase().startsWith("http")) {
                    try {
                        Desktop.getDesktop().browse(new URI(href));
                    } catch (IOException | URISyntaxException e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setHeaderText("Unable to launch browser");
                        alert.setContentText(e.toString());
                        alert.showAndWait();
                        Logger.getLogger(TPMAP.class.getName()).log(Level.SEVERE, null, e);
                    }
                }
                evt.preventDefault();
            }
        };

        Platform.runLater(() -> {
            engine.getLoadWorker().stateProperty().addListener((obs, oldStage, newState) -> {
                if(newState.equals(Worker.State.SUCCEEDED)) {
                    Document document;
                    document = engine.getDocument();
                    NodeList nodeList = document.getElementsByTagName("button");
                    if(nodeList.getLength()==1) {
                        Node node = nodeList.item(0);
                        EventTarget et = (EventTarget) node;
                        et.addEventListener("click", externalLinkListener, false);
                    }
                }
            });
        });

        engine.locationProperty().addListener((obs, oldValue, newValue) -> {

            Logger.getLogger(TPMAP.class.getName()).log(Level.INFO, newValue);

            try {
                final URIBuilder errorURL;

                if(!newValue.replaceFirst("^file://","").startsWith(this.getClass().getResource("/com/chembiohub/tpmap/doc/error.html").toString().replaceFirst("^file:", ""))) {
                    errorURL = new URIBuilder(this.getClass().getResource("/com/chembiohub/tpmap/doc/error.html").toString());
                    errorURL.addParameter("site", newValue);
                } else {
                    errorURL = new URIBuilder(newValue);
                }

                whitelist.add(errorURL.toString());

                final BooleanProperty invalidSite = new SimpleBooleanProperty(true);

                whitelist.forEach( s -> {
                    if(newValue.endsWith(s.replaceFirst("^file:", ""))) {
                        invalidSite.setValue(false);
                    } else if(s.startsWith("https://") && newValue.startsWith(s)) {
                        invalidSite.setValue(false);
                    } else if(s.startsWith("http://") && newValue.startsWith(s)) {
                        invalidSite.setValue(false);
                    }
                });

                if(invalidSite.getValue()) {
                    engine.load(errorURL.toString());
                }

            } catch(URISyntaxException e) {
                Logger.getLogger(TPMAP.class.getName()).log(Level.SEVERE, null, e);
            }

        });

        engine.load(url);

        return webView;

    }

    /**
     * Creates a container displaying web contents along with a navigation bar above.
     *
     * @return browser pane
     */
    public Pane createBrowserPane() {

        BorderPane browserPane = new BorderPane();

        WebView webView = this.createWebView();

        Button browserButton = new Button("Open in Web Browser");
        TextField urlField = new TextField();
        urlField.textProperty().bind(webView.getEngine().locationProperty());

        HBox container = new HBox(urlField, browserButton);
        HBox.setHgrow(urlField, Priority.ALWAYS);

        browserPane.setTop(container);
        browserPane.setCenter(webView);

        browserButton.setOnAction(event -> {
            if(Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().browse(new URI(urlField.textProperty().getValue()));
                }
                catch (IOException | URISyntaxException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setHeaderText("Unable to launch browser");
                    alert.setContentText(e.toString());
                    alert.showAndWait();
                    Logger.getLogger(TPMAP.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        });

        return browserPane;

    }

}
