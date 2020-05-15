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

import com.chembiohub.tpmap.analysis.TPBootstrapAnalysis;
import com.chembiohub.tpmap.dstruct.*;
import com.chembiohub.tpmap.normalisation.TPNormalisation;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;

/**
 * The GenericFileImporter class imports a tab delimited file containing the following columns:
 *
 * - Accession : a unique protein identifier
 * - Description : description of the protein. If this is in UniProtKB format then it will be parsed
 *              to extract gene name, description, and organism:
 *      db|UniqueIdentifier|EntryName ProteinName OS=OrganismName OX=OrganismIdentifier [GN=GeneName ]PE=ProteinExistence SV=SequenceVersion
 * - Columns for abundance headers:
 *
 *    * 2D format Ref_[Temperature]_[Concentration] : where [Temperature] and [Concentration]
 *              are to be replaced with respective temperature and concentration double values
 *
 *    * 1D format Ref_[Temperature]_[V/T Replicate] : where [Temperature]
 *              is to be replaced with respective temperature double values and [V/T Replicate] is to be
 *              replaced with a string referring to Vehicle/Treatment and Replicate number
 *
 *    * PISA format Ref_[Condition] : where [Condition] is to be replaced by:
 *              - V1, V2, and V3: for pooled vehicle temperature replicates 1-3, respectively.
 *              - T1, T2, and T4: for pooled treatment temperature replicates 1-3, respectively.
 *              - Pool: for the pool lane
 *
 *
 * @author felixfeyertag
 */
public class GenericFileImporter extends FileImporter {

    final private StringProperty filePath;
    final private StringProperty filePathDisplay;
    final private Double min;
    final private Double max;
    final private TPNormalisation.Normalisation norm;
    final private Proteome.ExpType tppExpType;

    private final Proteome<Protein> tppExperiment;

    @Override
    public Proteome getTppExperiment() {
        return tppExperiment;
    }

    GenericFileImporter(StringProperty filePath, Stage primaryStage, TabPane tpTabPane, double min, double max, TPNormalisation.Normalisation norm, ProteinParameters params, boolean multithreading, Proteome.ExpType tppExpType) {
        this.filePath = new SimpleStringProperty(filePath.get());
        this.filePathDisplay = filePath;
        this.min = min;
        this.max = max;
        this.norm = norm;
        this.tppExpType = tppExpType;

        if(filePath.getValue().toLowerCase().endsWith("xlsx") || filePath.getValue().toLowerCase().endsWith("xls")) {
            String datFilePath = convertXLS(filePath.getValue());
            this.filePath.setValue(datFilePath);
        }

        tppExperiment = new Proteome<>(primaryStage, tpTabPane);
        tppExperiment.setExpType(tppExpType);
        tppExperiment.setMultithreading(multithreading);
        if(null!=params) {
            if (params instanceof Protein1DParameters) {
                tppExperiment.setCurveFitAttempts(((Protein1DParameters) params).getAttempts());
                tppExperiment.setCurveFitMaxIterations(((Protein1DParameters) params).getMaxIterations());
            }
            if (params instanceof Protein2DParameters) {
                tppExperiment.setBootstrapIterations(((Protein2DParameters) params).getIterations());
            }
        }
    }

    GenericFileImporter(StringProperty filePath, Stage primaryStage, TabPane tpTabPane, StringProperty filePathDisplay, double min, double max, TPNormalisation.Normalisation norm, ProteinParameters params, boolean multithreading, Proteome.ExpType tppExpType) {
        this.filePath = filePath;
        this.filePathDisplay = filePathDisplay;
        this.min = min;
        this.max = max;
        this.norm = norm;
        this.tppExpType = tppExpType;

        if(filePath.getValue().toLowerCase().endsWith("xlsx") || filePath.getValue().toLowerCase().endsWith("xls")) {
            String datFilePath = convertXLS(filePath.getValue());
            this.filePath.setValue(datFilePath);
        }

        tppExperiment = new Proteome(primaryStage, tpTabPane);
        tppExperiment.setExpType(tppExpType);
        tppExperiment.setMultithreading(multithreading);
        switch(tppExpType) {
            case TP1D:
                assert params instanceof Protein1DParameters;
                tppExperiment.setCurveFitAttempts(((Protein1DParameters)params).getAttempts());
                tppExperiment.setCurveFitMaxIterations(((Protein1DParameters)params).getMaxIterations());
                break;
            case TP2D:
                assert params instanceof Protein2DParameters;
                tppExperiment.setBootstrapIterations(((Protein2DParameters)params).getIterations());
                break;
        }
    }

    /**
     *
     */
    @Override
    public Task<Void> initImport() {

        return new Task<Void>() {
            private void changed(Number newProgress) {
                updateProgress(newProgress.doubleValue(), 1);
            }

            @Override
            public Void call() throws IOException,InvalidHeaderException {

                IntegerProperty counter = new SimpleIntegerProperty(0);
                    long numProteins = Files.lines(Paths.get(filePath.get())).count()-1;

                    HashMap<String,Integer> index;
                    ObservableList<String> headers = FXCollections.observableArrayList();
                    ObservableList<Double> tempVals = FXCollections.observableArrayList();
                    ObservableList<String> concVals = FXCollections.observableArrayList();

                    BufferedReader br = new BufferedReader(new FileReader(filePath.get()));
                    String[] header = br.readLine().split("\t");

                    index = new HashMap<>();


                    for(int i=0;i<header.length;i++) {
                        header[i] = header[i].replaceFirst("^\"","");
                        header[i] = header[i].replaceFirst("\"$", "");
                        header[i] = header[i].replaceAll("[ \\t\\n\\x0b\\r\\f]+", "");
                        header[i] = header[i].toLowerCase();

                        if(header[i].equals("accession")) {
                            index.put("accession",i);
                        }
                        else if(header[i].equals("description")) {
                            index.put("description",i);
                        }
                        else if(header[i].startsWith("ref_")) {
                            if(tppExpType.equals(Proteome.ExpType.PISA)) {
                                if(header[i].toLowerCase().equals("ref_v1")) {

                                }
                            }

                            if(tppExpType == Proteome.ExpType.PISA) {

                                String[] hVal = header[i].split("_");
                                if (hVal.length != 2) {
                                    throw new InvalidHeaderException("Invalid reference header: " + header[i]);
                                }
                                if (!hVal[1].matches("v1|v2|v3|t1|t2|t3|c1|c2|c3|pool")) {
                                    throw new InvalidHeaderException("Invalid PISA header: " + header[i]);
                                }

                                index.put(hVal[1], i);
                                concVals.add(hVal[1]);

                            } else {

                                String[] refVal = header[i].split("_");
                                if(refVal.length!=3) {
                                    throw new InvalidHeaderException("Invalid reference header: " + header[i]);
                                }
                                Double tempVal = Double.NaN;
                                try {
                                    tempVal = Double.parseDouble(refVal[1]);
                                } catch (NumberFormatException e) {
                                    throw new InvalidHeaderException("Invalid temperature value: " + tempVal);
                                } catch (NullPointerException e) {
                                    throw new InvalidHeaderException("No temperature specified: " + header[i]);
                                }
                                String concVal = refVal[2];
                                if(concVal.isEmpty()) {
                                    throw new InvalidHeaderException("No concentration specified: " + header[i]);
                                }

                                if(headers.contains(tempVal + ":" + concVal)) {
                                    throw new InvalidHeaderException("Too many columns for temperature " + tempVal + " and concentration " + concVal);
                                }

                                headers.add(tempVal + ":" + concVal);
                                index.put(tempVal + ":" + concVal, i);


                                if(!tempVals.contains(tempVal)) {
                                    tempVals.add(tempVal);
                                }
                                if(!concVals.contains(concVal)) {
                                    concVals.add(concVal);
                                }
                            }
                        }
                    }

                    if(tempVals.isEmpty()) {
                        throw new InvalidHeaderException("Parsing error: Could not identify temperature values");
                    }
                    if(concVals.isEmpty()) {
                        throw new InvalidHeaderException("Parsing error: Could not identify concentration values");
                    }

                    class ConcComparator implements Comparator<String> {
                        public int compare(String o1, String o2){
                            try {
                                return new BigDecimal(o1).compareTo(new BigDecimal(o2));
                            } catch (NumberFormatException e) {
                                return o1.compareTo(o2);
                            }
                        }
                    }

                    tempVals.sort(Comparator.naturalOrder());
                    concVals.sort(new ConcComparator());

                    tppExperiment.setFileName(Paths.get(filePathDisplay.get()).getFileName().toString());

                    int accessionCoordinate = index.get("accession");
                    int descriptionCoordinate = index.get("description");

                    int[][] abundanceCoordinates = new int[concVals.size()][tempVals.size()];


                    for(int i=0;i<abundanceCoordinates.length;i++) {
                        for (int j=0;j<abundanceCoordinates[0].length;j++) {
                            try {
                                abundanceCoordinates[i][j] = index.get(tempVals.get(j).toString() + ":" + concVals.get(i));
                            } catch (NullPointerException ex) {
                                throw new InvalidHeaderException("Unable to load column for temperature " + j + " and concentration " + i + "\n");
                            }
                        }
                    }


                    int[] referenceCoordinates = new int[tempVals.size()];
                    for(int i=0;i<referenceCoordinates.length;i++) {
                        referenceCoordinates[i] = index.get(tempVals.get(i).toString() + ":" + concVals.get(0));
                    }

                    tppExperiment.setConcLabels(concVals);
                    tppExperiment.setTempLabels(tempVals);

                    String line;

                    while((line = br.readLine()) != null) {
                        String[] lineVals = line.split("\t");

                        if(lineVals.length<accessionCoordinate) {
                            continue;
                        }

                        String accession = lineVals[accessionCoordinate];

                        if(accession==null) {
                            continue;
                        }
                        String organismName = "";
                        String organismIdentifier = "";
                        String geneName = "";
                        String proteinExistence = "";
                        String sequenceVersion = "";

                        String description="";
                        String[] descriptionVals = new String[] { description };
                        try {
                            lineVals[descriptionCoordinate] = lineVals[descriptionCoordinate].replaceAll("^\"", "");
                            lineVals[descriptionCoordinate] = lineVals[descriptionCoordinate].replaceAll("\"$", "");
                            descriptionVals = lineVals[descriptionCoordinate].split("=");
                            description = descriptionVals[0];
                        } catch(ArrayIndexOutOfBoundsException e) {
                            description = "";
                        }
                        description = description.replaceAll(" OS$", "");
                        description = description.replaceAll(" OX$", "");
                        description = description.replaceAll(" GN$", "");
                        description = description.replaceAll(" PE$", "");
                        description = description.replaceAll(" SV$", "");
                        for(int j=1;j<descriptionVals.length;j++) {
                            if(descriptionVals[j-1].endsWith(" OS")) {
                                organismName = descriptionVals[j];
                                organismName = organismName.replaceAll(" OX$", "");
                                organismName = organismName.replaceAll(" GN$", "");
                                organismName = organismName.replaceAll(" PE$", "");
                                organismName = organismName.replaceAll(" SV$", "");
                            }
                            else if (descriptionVals[j-1].endsWith(" OX")) {
                                organismIdentifier = descriptionVals[j];
                                organismIdentifier = organismIdentifier.replaceAll(" OS$", "");
                                organismIdentifier = organismIdentifier.replaceAll(" GN$", "");
                                organismIdentifier = organismIdentifier.replaceAll(" PE$", "");
                                organismIdentifier = organismIdentifier.replaceAll(" SV$", "");
                            }
                            else if (descriptionVals[j-1].endsWith(" GN")) {
                                geneName = descriptionVals[j];
                                geneName = geneName.replaceAll(" OS$", "");
                                geneName = geneName.replaceAll(" OX$", "");
                                geneName = geneName.replaceAll(" PE$", "");
                                geneName = geneName.replaceAll(" SV$", "");
                            }
                            else if (descriptionVals[j-1].endsWith(" PE")) {
                                proteinExistence = descriptionVals[j];
                                proteinExistence = proteinExistence.replaceAll(" OS$", "");
                                proteinExistence = proteinExistence.replaceAll(" OX$", "");
                                proteinExistence = proteinExistence.replaceAll(" GN$", "");
                                proteinExistence = proteinExistence.replaceAll(" SV$", "");
                            }
                            else if (descriptionVals[j-1].endsWith(" SV")) {
                                sequenceVersion = descriptionVals[j];
                                sequenceVersion = sequenceVersion.replaceAll(" OS$", "");
                                sequenceVersion = sequenceVersion.replaceAll(" OX$", "");
                                sequenceVersion = sequenceVersion.replaceAll(" GN$", "");
                                sequenceVersion = sequenceVersion.replaceAll(" PE$", "");
                            }
                        }


                        Double[][] abundances = new Double[abundanceCoordinates[0].length][abundanceCoordinates.length];

                        Double[] concReference = new Double[abundanceCoordinates[0].length];
                        Double[] tempReference = new Double[abundanceCoordinates.length];

                        for(int i=0;i<concReference.length;i++) {
                            concReference[i] = Double.NaN;
                            try {
                                concReference[i] = Double.parseDouble(lineVals[referenceCoordinates[i]]);
                            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                                concReference[i] = Double.NaN;
                            }
                        }
                        for(int i=0;i<abundances.length;i++) {
                            for(int j=0;j<abundances[0].length;j++) {
                                abundances[i][j] = Double.NaN;
                                try {
                                    abundances[i][j] = Double.parseDouble(lineVals[index.get(tempVals.get(i).toString() + ":" + concVals.get(j))]);
                                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                                    abundances[i][j] = Double.NaN;
                                }
                                tempReference[j] = abundances[0][j];
                            }
                        }

                        switch(tppExpType) {
                            case TP1D:
                                Protein1D protein1d = new Protein1D();
                                protein1d.setAccession(accession);
                                protein1d.setDescription(description);
                                protein1d.setOrganismName(organismName);
                                protein1d.setOrganismIdentifier(organismIdentifier);
                                protein1d.setGeneName(geneName);
                                protein1d.setProteinExistence(proteinExistence);
                                protein1d.setSequenceVersion(sequenceVersion);
                                protein1d.setAttempts(tppExperiment.getCurveFitAttempts());
                                protein1d.setMaxIterations(tppExperiment.getCurveFitMaxIterations());
                                //protein1d.setConcReference(concReference);
                                protein1d.setTempReference(tempReference);
                                protein1d.setAbundances(abundances, tppExperiment.getTempLabels(), tppExperiment.getConcLabels());
                                protein1d.calculateRatios(tppExperiment.getTempLabels(),tppExperiment.getConcLabels());
                                protein1d.setNormalisationMethod(norm);
                                tppExperiment.addProtein(protein1d);
                                break;
                            case TP2D:
                                Protein2D protein2d = new Protein2D();
                                protein2d.setAccession(accession);
                                protein2d.setDescription(description);
                                protein2d.setOrganismName(organismName);
                                protein2d.setOrganismIdentifier(organismIdentifier);
                                protein2d.setGeneName(geneName);
                                protein2d.setProteinExistence(proteinExistence);
                                protein2d.setSequenceVersion(sequenceVersion);
                                protein2d.setConcReference(concReference);
                                protein2d.setTempReference(tempReference);
                                protein2d.setAbundances(abundances, tppExperiment.getTempLabels(), tppExperiment.getConcLabels());
                                protein2d.setNormalisationMethod(norm);
                                //protein2d.calculateRatios(min, max, tppExperiment.getTempLabels(), tppExperiment.getConcLabels());
                                tppExperiment.addProtein(protein2d);
                                break;
                            case PISA:
                                /*ProteinPISA proteinPisa = new ProteinPISA();
                                proteinPisa.setAccession(accession);
                                proteinPisa.setDescription(description);
                                proteinPisa.setOrganismName(organismName);
                                proteinPisa.setOrganismIdentifier(organismIdentifier);
                                proteinPisa.setGeneName(geneName);
                                proteinPisa.setProteinExistence(geneName);
                                proteinPisa.setSequenceVersion(sequenceVersion);
                                proteinPisa.setTempReference(tempReference);
                                proteinPisa.setAbundances(abundances, tppExperiment.getTempLabels(), tppExperiment.getConcLabels());
                                proteinPisa.setNormalisationMethod(norm, tppExperiment.getTempLabels(), tppExperiment.getConcLabels());
                                //proteinPisa.calculateRatios(tppExperiment.getTempLabels(), tppExperiment.getConcLabels());
                                tppExperiment.addProtein(proteinPisa);*/
                                break;
                        }

                        counter.set(counter.get()+1);

                        updateProgress(counter.get(),numProteins);

                        updateMessage("Importing... " + counter.get() + "/" + numProteins);

                        if(isCancelled()) {
                            br.close();
                            return null;
                        }
                    }

                    br.close();

                    updateMessage("Processing data...");

                    tppExperiment.progressProperty().addListener((obs, oldProgress, newProgress) -> changed(newProgress));
                    tppExperiment.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                        if(isCancelled()) {
                            tppExperiment.getIsCancelled().set(true);
                        }
                    });

                    tppExperiment.updateProteinCount();
                    tppExperiment.setPercentileThresholds(min, max);
                    tppExperiment.setNormalisation(norm);


                    if(tppExpType==Proteome.ExpType.TP2D) {
                        if (tppExperiment.getBootstrapIterations() > 0) {

                            updateMessage("Running bootstrap...");

                            TPBootstrapAnalysis bsAnalysis = new TPBootstrapAnalysis(tppExperiment.getBootstrapIterations());

                            bsAnalysis.runBootstrapAnalysis(tppExperiment);
                            bsAnalysis.setBootstrapPVals(tppExperiment);

                            tppExperiment.set2dBootstrapAnalysis(bsAnalysis);

                        }
                    }

                return null;
            }
        };
    }

    protected static String convertXLS(String file) {

        File tempFile = null;
        String tmpDir = System.getProperty("java.io.tmpdir");

        try {
            tempFile = File.createTempFile("tpmap_", ".dat", new File(tmpDir));
            Workbook wb = new XSSFWorkbook(new File(file));
            DataFormatter formatter = new DataFormatter();
            PrintStream out = new PrintStream(new FileOutputStream(tempFile),true, "UTF8");
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                boolean first = true;
                for (Cell cell : row) {
                    if (!first) out.print('\t');
                    String contents = formatter.formatCellValue(cell);
                    contents.replaceAll("\t", "");
                    out.print(contents);
                    first = false;
                }
                out.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidFormatException e) {
            e.printStackTrace();
        }

        return tempFile.getAbsolutePath();
    }
}
