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

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.chembiohub.tpmap.dstruct.ProteinParameters;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import com.chembiohub.tpmap.dstruct.Proteome;
import com.chembiohub.tpmap.normalisation.TPNormalisation;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * ProteomeDiscovererFileImporter parses data of the format that Proteome Discoverer produces when exporting proteins.
 *
 * Using Proteome Discoverer output and a configuration file, this class converts the Proteome Discoverer output
 * to native TPMAP format, and calls the GenericFileImporter class to then import the data structures.
 *
 * It reads two files:
 *
 * 1) Proteome Discoverer output, this should be in tab-separated text format and have header columns similar to:
 *
 *    Checked	Protein FDR Confidence: Combined	Master	Accession	Description	Exp. q-value: Combined	Sum PEP Score	Coverage %	# Peptides	# PSMs	# Unique Peptides	# Protein Groups	# AAs	MW kDa	calc. pI	Score Sequest HT: Sequest HT	# Peptides (by Search Engine): Sequest HT	# Razor Peptides	"Abundance Ratio: (F1, 127N) / (F1, 126)"	"Abundance Ratio: (F1, 127C) / (F1, 126)"	"Abundance Ratio: (F1, 128N) / (F1, 126)"	"Abundance Ratio: (F1, 128C) / (F1, 126)"	"Abundance Ratio: (F1, 129N) / (F1, 126)"	"Abundance Ratio: (F1, 129C) / (F1, 126)"	"Abundance Ratio: (F1, 130N) / (F1, 126)"	"Abundance Ratio: (F1, 130C) / (F1, 126)"	"Abundance Ratio: (F1, 131) / (F1, 126)"	"Abundance Ratio: (F2, 127N) / (F2, 126)"	"Abundance Ratio: (F2, 127C) / (F2, 126)"	"Abundance Ratio: (F2, 128N) / (F2, 126)"	"Abundance Ratio: (F2, 128C) / (F2, 126)"	"Abundance Ratio: (F2, 129N) / (F2, 126)"	"Abundance Ratio: (F2, 129C) / (F2, 126)"	"Abundance Ratio: (F2, 130N) / (F2, 126)"	"Abundance Ratio: (F2, 130C) / (F2, 126)"	"Abundance Ratio: (F2, 131) / (F2, 126)"	"Abundance Ratio: (F3, 127N) / (F3, 126)"	"Abundance Ratio: (F3, 127C) / (F3, 126)"	"Abundance Ratio: (F3, 128N) / (F3, 126)"	"Abundance Ratio: (F3, 128C) / (F3, 126)"	"Abundance Ratio: (F3, 129N) / (F3, 126)"	"Abundance Ratio: (F3, 129C) / (F3, 126)"	"Abundance Ratio: (F3, 130N) / (F3, 126)"	"Abundance Ratio: (F3, 130C) / (F3, 126)"	"Abundance Ratio: (F3, 131) / (F3, 126)"	"Abundance Ratio: (F4, 127N) / (F4, 126)"	"Abundance Ratio: (F4, 127C) / (F4, 126)"	"Abundance Ratio: (F4, 128N) / (F4, 126)"	"Abundance Ratio: (F4, 128C) / (F4, 126)"	"Abundance Ratio: (F4, 129N) / (F4, 126)"	"Abundance Ratio: (F4, 129C) / (F4, 126)"	"Abundance Ratio: (F4, 130N) / (F4, 126)"	"Abundance Ratio: (F4, 130C) / (F4, 126)"	"Abundance Ratio: (F4, 131) / (F4, 126)"	"Abundance Ratio: (F5, 127N) / (F5, 126)"	"Abundance Ratio: (F5, 127C) / (F5, 126)"	"Abundance Ratio: (F5, 128N) / (F5, 126)"	"Abundance Ratio: (F5, 128C) / (F5, 126)"	"Abundance Ratio: (F5, 129N) / (F5, 126)"	"Abundance Ratio: (F5, 129C) / (F5, 126)"	"Abundance Ratio: (F5, 130N) / (F5, 126)"	"Abundance Ratio: (F5, 130C) / (F5, 126)"	"Abundance Ratio: (F5, 131) / (F5, 126)"	"Abundance Ratio: (F6, 127N) / (F6, 126)"	"Abundance Ratio: (F6, 127C) / (F6, 126)"	"Abundance Ratio: (F6, 128N) / (F6, 126)"	"Abundance Ratio: (F6, 128C) / (F6, 126)"	"Abundance Ratio: (F6, 129N) / (F6, 126)"	"Abundance Ratio: (F6, 129C) / (F6, 126)"	"Abundance Ratio: (F6, 130N) / (F6, 126)"	"Abundance Ratio: (F6, 130C) / (F6, 126)"	"Abundance Ratio: (F6, 131) / (F6, 126)"	"Abundances (Grouped): F1, 126"	"Abundances (Grouped): F1, 127N"	"Abundances (Grouped): F1, 127C"	"Abundances (Grouped): F1, 128N"	"Abundances (Grouped): F1, 128C"	"Abundances (Grouped): F1, 129N"	"Abundances (Grouped): F1, 129C"	"Abundances (Grouped): F1, 130N"	"Abundances (Grouped): F1, 130C"	"Abundances (Grouped): F1, 131"	"Abundances (Grouped): F2, 126"	"Abundances (Grouped): F2, 127N"	"Abundances (Grouped): F2, 127C"	"Abundances (Grouped): F2, 128N"	"Abundances (Grouped): F2, 128C"	"Abundances (Grouped): F2, 129N"	"Abundances (Grouped): F2, 129C"	"Abundances (Grouped): F2, 130N"	"Abundances (Grouped): F2, 130C"	"Abundances (Grouped): F2, 131"	"Abundances (Grouped): F3, 126"	"Abundances (Grouped): F3, 127N"	"Abundances (Grouped): F3, 127C"	"Abundances (Grouped): F3, 128N"	"Abundances (Grouped): F3, 128C"	"Abundances (Grouped): F3, 129N"	"Abundances (Grouped): F3, 129C"	"Abundances (Grouped): F3, 130N"	"Abundances (Grouped): F3, 130C"	"Abundances (Grouped): F3, 131"	"Abundances (Grouped): F4, 126"	"Abundances (Grouped): F4, 127N"	"Abundances (Grouped): F4, 127C"	"Abundances (Grouped): F4, 128N"	"Abundances (Grouped): F4, 128C"	"Abundances (Grouped): F4, 129N"	"Abundances (Grouped): F4, 129C"	"Abundances (Grouped): F4, 130N"	"Abundances (Grouped): F4, 130C"	"Abundances (Grouped): F4, 131"	"Abundances (Grouped): F5, 126"	"Abundances (Grouped): F5, 127N"	"Abundances (Grouped): F5, 127C"	"Abundances (Grouped): F5, 128N"	"Abundances (Grouped): F5, 128C"	"Abundances (Grouped): F5, 129N"	"Abundances (Grouped): F5, 129C"	"Abundances (Grouped): F5, 130N"	"Abundances (Grouped): F5, 130C"	"Abundances (Grouped): F5, 131"	"Abundances (Grouped): F6, 126"	"Abundances (Grouped): F6, 127N"	"Abundances (Grouped): F6, 127C"	"Abundances (Grouped): F6, 128N"	"Abundances (Grouped): F6, 128C"	"Abundances (Grouped): F6, 129N"	"Abundances (Grouped): F6, 129C"	"Abundances (Grouped): F6, 130N"	"Abundances (Grouped): F6, 130C"	"Abundances (Grouped): F6, 131"	"Found in Sample: S1 F1: 126, Control"	"Found in Sample: S7 F1: 127N, Sample"	"Found in Sample: S8 F1: 127C, Sample"	"Found in Sample: S9 F1: 128N, Sample"	"Found in Sample: S10 F1: 128C, Sample"	"Found in Sample: S11 F1: 129N, Sample"	"Found in Sample: S12 F1: 129C, Sample"	"Found in Sample: S13 F1: 130N, Sample"	"Found in Sample: S14 F1: 130C, Sample"	"Found in Sample: S15 F1: 131, Sample"	"Found in Sample: S2 F2: 126, Control"	"Found in Sample: S16 F2: 127N, Sample"	"Found in Sample: S17 F2: 127C, Sample"	"Found in Sample: S18 F2: 128N, Sample"	"Found in Sample: S19 F2: 128C, Sample"	"Found in Sample: S20 F2: 129N, Sample"	"Found in Sample: S21 F2: 129C, Sample"	"Found in Sample: S22 F2: 130N, Sample"	"Found in Sample: S23 F2: 130C, Sample"	"Found in Sample: S24 F2: 131, Sample"	"Found in Sample: S3 F3: 126, Control"	"Found in Sample: S25 F3: 127N, Sample"	"Found in Sample: S26 F3: 127C, Sample"	"Found in Sample: S27 F3: 128N, Sample"	"Found in Sample: S28 F3: 128C, Sample"	"Found in Sample: S29 F3: 129N, Sample"	"Found in Sample: S30 F3: 129C, Sample"	"Found in Sample: S31 F3: 130N, Sample"	"Found in Sample: S32 F3: 130C, Sample"	"Found in Sample: S33 F3: 131, Sample"	"Found in Sample: S4 F4: 126, Control"	"Found in Sample: S34 F4: 127N, Sample"	"Found in Sample: S35 F4: 127C, Sample"	"Found in Sample: S36 F4: 128N, Sample"	"Found in Sample: S37 F4: 128C, Sample"	"Found in Sample: S38 F4: 129N, Sample"	"Found in Sample: S39 F4: 129C, Sample"	"Found in Sample: S40 F4: 130N, Sample"	"Found in Sample: S41 F4: 130C, Sample"	"Found in Sample: S42 F4: 131, Sample"	"Found in Sample: S5 F5: 126, Control"	"Found in Sample: S43 F5: 127N, Sample"	"Found in Sample: S44 F5: 127C, Sample"	"Found in Sample: S45 F5: 128N, Sample"	"Found in Sample: S46 F5: 128C, Sample"	"Found in Sample: S47 F5: 129N, Sample"	"Found in Sample: S48 F5: 129C, Sample"	"Found in Sample: S49 F5: 130N, Sample"	"Found in Sample: S50 F5: 130C, Sample"	"Found in Sample: S51 F5: 131, Sample"	"Found in Sample: S6 F6: 126, Control"	"Found in Sample: S52 F6: 127N, Sample"	"Found in Sample: S53 F6: 127C, Sample"	"Found in Sample: S54 F6: 128N, Sample"	"Found in Sample: S55 F6: 128C, Sample"	"Found in Sample: S56 F6: 129N, Sample"	"Found in Sample: S57 F6: 129C, Sample"	"Found in Sample: S58 F6: 130N, Sample"	"Found in Sample: S59 F6: 130C, Sample"	"Found in Sample: S60 F6: 131, Sample"	Modifications
 *
 *    Note that the columns required for import are: Accession, Description, as well as the Abundance columns for each
 *    sample and channel specified in the configuration file (see below).
 *
 * 2) A configuration file, this should be a tab-separated text file with four columns
 *      - Sample
 *      - Channel
 *      - Temperature
 *      - Concentration (2d) or Replicate (1d)
 *
 *    For example, for one sample (F1), ten channels (126-131), two temperatures (37 and 73) and 5 concentrations:
 *          F1	126	37	50
 *          F1	127N	37	10
 *          F1	127C	37	1
 *          F1	128N	37	0.2
 *          F1	128C	37	0
 *          F1	129N	73	50
 *          F1	129C	73	10
 *          F1	130N	73	1
 *          F1	130C	73	0.2
 *          F1	131	73	0
 *
 * @author felixfeyertag
 */
public class ProteomeDiscovererFileImporter extends FileImporter {

    private final StringProperty filePath;
    private final StringProperty filePathDisplay;
    private final StringProperty configFilePath;
    private final double min;
    private final double max;
    private final TPNormalisation.Normalisation norm;
    private Proteome tppExperiment;
    private final ProteinParameters params;
    private final Proteome.ExpType tppExpType;
    private final boolean multithreading;
    private final TabPane tpTabPane;
    private final Stage primaryStage;

    public ProteomeDiscovererFileImporter(StringProperty filePath, StringProperty configFilePath, Stage primaryStage, TabPane tpTabPane, double min, double max, TPNormalisation.Normalisation norm, ProteinParameters params, boolean multithreading, Proteome.ExpType tppExpType) {

        this.filePath = filePath;
        this.filePathDisplay = new SimpleStringProperty(filePath.get());
        this.configFilePath = configFilePath;
        this.primaryStage = primaryStage;
        this.tpTabPane = tpTabPane;
        this.min = min;
        this.max = max;
        this.norm = norm;
        this.params = params;
        this.tppExpType = tppExpType;
        this.multithreading = multithreading;

        if(filePath.toString().toLowerCase().endsWith("xlsx") || filePath.toString().toLowerCase().endsWith("xls")) {
            this.filePath.setValue(GenericFileImporter.convertXLS(filePath.toString()));
        }
        if(configFilePath.toString().toLowerCase().endsWith("xlsx") || configFilePath.toString().toLowerCase().endsWith("xls")) {
            this.configFilePath.setValue(GenericFileImporter.convertXLS(filePath.toString()));
        }


    }
    
    @Override
    public Task<Void> initImport() throws IOException, InvalidHeaderException {

        Map<String,String> tmtMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(configFilePath.get()))) {

            String line;

            while((line = br.readLine())!=null) {

                String[] lineVals = line.split("\t");

                if(lineVals.length>=4) {
                    if(tmtMap.containsKey("abundance: " + lineVals[0].toLowerCase() + ": " + lineVals[1].toLowerCase())) {
                        throw new InvalidHeaderException("Multiple definitions of " + lineVals[0] + ": " + lineVals[1]);
                    }
                    tmtMap.put("abundances: "           + lineVals[0].toLowerCase() + ", " + lineVals[1].toLowerCase(), "Ref_" + lineVals[2] + "_" + lineVals[3]);
                    tmtMap.put("abundances (scaled): "  + lineVals[0].toLowerCase() + ": " + lineVals[1].toLowerCase(), "Ref_" + lineVals[2] + "_" + lineVals[3]);
                    tmtMap.put("abundances (grouped): " + lineVals[0].toLowerCase() + ", " + lineVals[1].toLowerCase(), "Ref_" + lineVals[2] + "_" + lineVals[3]);
                }
            }
        }

        String tmpDir = System.getProperty("java.io.tmpdir");
        File tempFile;
        tempFile = File.createTempFile("tpmap_", ".dat", new File(tmpDir));

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
                BufferedReader br = new BufferedReader(new FileReader(filePath.get()))) {

            String line;
            line = br.readLine();
            String[] lineVals = line.split("\t");
            int[] positions = new int[tmtMap.size()+2];
            int pos = 0;
            boolean first = true;

            for(int i=0;i<lineVals.length;i++) {

                lineVals[i] = lineVals[i].replaceAll("^\"", "");
                lineVals[i] = lineVals[i].replaceAll("\"$", "");

                switch (lineVals[i].toLowerCase()) {

                    case "accession":
                        if(!first) { bw.write("\t"); }
                        if(first) { first = false; }
                        bw.write("accession");
                        positions[pos++] = i;
                        break;

                    case "description":
                        if(!first) { bw.write("\t"); }
                        if(first) { first = false; }
                        bw.write("description");
                        positions[pos++] = i;
                        break;

                    default:
                        for (String key : tmtMap.keySet()) {
                            if(lineVals[i].toLowerCase().startsWith(key)) {
                                if(!first) bw.write("\t");
                                if(first) first = false;
                                bw.write(tmtMap.get(key));
                                positions[pos++] = i;
                                break;
                            }
                        }
                        break;
                }
            }

            bw.write("\n");

            while((line = br.readLine())!=null) {
                lineVals = line.split("\t");
                first = true;

                for (int i : positions) {
                    if(!first) bw.write("\t");
                    if(first) first = false;
                    if(i<lineVals.length) {
                        bw.write(lineVals[i]);
                    }
                }

                bw.write("\n");
            }
        }

        GenericFileImporter gfi = new GenericFileImporter(new SimpleStringProperty(tempFile.getAbsolutePath()), primaryStage, tpTabPane, filePathDisplay, min, max, norm, params, multithreading, tppExpType);
        tppExperiment = gfi.getTppExperiment();
        return gfi.initImport();
    }

    @Override
    public Proteome getTppExperiment() {
        return tppExperiment;
    }


}

