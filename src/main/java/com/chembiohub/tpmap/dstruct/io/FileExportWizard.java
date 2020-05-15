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
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * FileExportWizard
 *
 * This class will print a string containing contents of a table to a tab-delimited text file or an XLSX file.
 * For 2D proteome tables, abundance fold change can optionally be coloured.
 *
 * @author felixfeyertag
 */
public class FileExportWizard {

    public FileExportWizard(String content, final Stage primaryStage, boolean conditionalFormatting2d, int concVals2d) {
        
        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter fceftxt = new FileChooser.ExtensionFilter("Tab Delimited Text File (*.txt)", "*.txt");
        FileChooser.ExtensionFilter fcefxls = new FileChooser.ExtensionFilter("Excel 2007 OOXML File (*.xlsx)", "*.xlsx");
        fc.getExtensionFilters().addAll(fcefxls, fceftxt);
        
        File f = fc.showSaveDialog(primaryStage);
        
        if(f!=null) {

            try {
                if (f.getName().toLowerCase().endsWith("xls") || f.getName().toLowerCase().endsWith("xlsx")) {
                    XSSFWorkbook wb = new XSSFWorkbook();
                    XSSFSheet sheet = wb.createSheet();
                    String[] lines = content.split("\n");

                    Font headerFont = wb.createFont();
                    headerFont.setBold(true);

                    CellStyle numberStyle = wb.createCellStyle();
                    numberStyle.setDataFormat(wb.createDataFormat().getFormat("0.0000"));

                    CellStyle headerStyle = wb.createCellStyle();
                    headerStyle.setBorderRight(BorderStyle.THIN);
                    headerStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
                    headerStyle.setBorderTop(BorderStyle.THIN);
                    headerStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
                    headerStyle.setBorderLeft(BorderStyle.THIN);
                    headerStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
                    headerStyle.setBorderBottom(BorderStyle.THIN);
                    headerStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());

                    headerStyle.setAlignment(HorizontalAlignment.CENTER);
                    headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
                    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    headerStyle.setFont(headerFont);


                    for (int i = 0; i < lines.length; i++) {
                        XSSFRow row = sheet.createRow(i);
                        String[] cells = lines[i].split("\t");
                        for (int j = 0; j < cells.length; j++) {
                            if (i == 0) {
                                Cell headerCell = row.createCell(j);
                                headerCell.setCellValue(cells[j]);
                                headerCell.setCellStyle(headerStyle);
                            } else if (j == 0) {
                                row.createCell(j).setCellValue(cells[j]);
                            } else {
                                try {
                                    if (!Double.isFinite(Double.parseDouble(cells[j]))) {
                                        throw new NumberFormatException();
                                    }
                                    row.createCell(j).setCellValue(Double.parseDouble(cells[j]));
                                    row.getCell(j).setCellStyle(numberStyle);
                                } catch (NumberFormatException | NullPointerException e) {
                                    row.createCell(j).setCellValue(cells[j]);
                                }
                            }
                        }
                    }

                    if (conditionalFormatting2d && concVals2d>0) {

                        SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();
                        ConditionalFormattingRule rule1 = sheetCF.createConditionalFormattingColorScaleRule();
                        ColorScaleFormatting clrFmt = rule1.getColorScaleFormatting();
                        clrFmt.getThresholds()[0].setRangeType(ConditionalFormattingThreshold.RangeType.PERCENTILE);
                        clrFmt.getThresholds()[0].setValue(1d);
                        clrFmt.getThresholds()[1].setRangeType(ConditionalFormattingThreshold.RangeType.NUMBER);
                        clrFmt.getThresholds()[1].setValue(1d);
                        clrFmt.getThresholds()[2].setRangeType(ConditionalFormattingThreshold.RangeType.PERCENTILE);
                        clrFmt.getThresholds()[2].setValue(99d);
                        ((ExtendedColor) clrFmt.getColors()[0]).setARGBHex("FFF8696B");
                        ((ExtendedColor) clrFmt.getColors()[1]).setARGBHex("FFFFEB84");
                        ((ExtendedColor) clrFmt.getColors()[2]).setARGBHex("FF63BE7B");

                        String xlsColumnStart = CellReference.convertNumToColString(lines[0].split("\t").length-concVals2d);
                        String xlsColumnEnd = CellReference.convertNumToColString(lines[0].split("\t").length);

                        CellRangeAddress[] regions = {CellRangeAddress.valueOf(xlsColumnStart + "2:" + xlsColumnEnd + lines.length)};
                        sheetCF.addConditionalFormatting(regions, rule1);

                    }

                    FileOutputStream fos = new FileOutputStream(f.getAbsoluteFile());
                    wb.write(fos);
                    fos.close();

                }
                else {

                    PrintWriter writer;
                    writer = new PrintWriter(f);
                    writer.println(content);
                    writer.close();

                }
            } catch (IOException ex) {

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.initOwner(primaryStage);
                alert.setTitle("File IO Exception");
                alert.setHeaderText("File IO Exception");
                alert.setContentText("Could not write file: " + f.getPath());

                Logger.getLogger(FileExportWizard.class.getName()).log(Level.SEVERE, null, ex);

                alert.showAndWait();
            } finally {
                content = null;
                System.gc();
            }
        }
    }
    
}

