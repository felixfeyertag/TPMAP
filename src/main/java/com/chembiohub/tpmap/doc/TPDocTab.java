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
package com.chembiohub.tpmap.doc;

import com.chembiohub.tpmap.ui.TPWebView;
import javafx.scene.control.Tab;

/**
 * TPDocTab
 *
 * This is the tab first shown when TPMAP is loaded. Instantiates a WebView displaying the welcome.html file.
 *
 * @author felixfeyertag
 */
public class TPDocTab {

    private final Tab docTab;
    
    public TPDocTab () {

        docTab = new Tab("Welcome");
        
        docTab.setClosable(false);
        TPWebView tpWebView = new TPWebView(this.getClass().getResource("/com/chembiohub/tpmap/doc/welcome.html").toString());

        tpWebView.addToWhitelist(this.getClass().getResource("/com/chembiohub/tpmap/doc/welcome.html").toString());
        tpWebView.addToWhitelist(this.getClass().getResource("/com/chembiohub/tpmap/doc/quickstart.html").toString());
        tpWebView.addToWhitelist("https://www.chembiohub.com");
        tpWebView.addToWhitelist("https://www.gnu.org/licenses/gpl-3.0.en.html");

        docTab.setContent(tpWebView.createWebView());

    }

    public Tab getDocTab() {
        return docTab;
    }

}
