package mara.mybox.controller;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import mara.mybox.db.data.VisitHistory;
import mara.mybox.dev.MyBoxLog;
import mara.mybox.fxml.NodeStyleTools;
import static mara.mybox.fxml.NodeStyleTools.badStyle;
import mara.mybox.tools.FileNameTools;
import mara.mybox.tools.FileTools;
import mara.mybox.tools.TextFileTools;
import mara.mybox.value.AppVariables;
import static mara.mybox.value.Languages.message;
import mara.mybox.value.Languages;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * @Author Mara
 * @CreateDate 2020-12-14
 * @License Apache License Version 2.0
 */
public class DataFileCSVConvertController extends BaseDataConvertController {

    @FXML
    protected ControlCsvOptions csvReadController;

    public DataFileCSVConvertController() {
        baseTitle = Languages.message("CsvConvert");
    }

    @Override
    public void setFileType() {
        setFileType(VisitHistory.FileType.CSV, VisitHistory.FileType.All);
    }

    @Override
    public void initOptionsSection() {
        try {
            super.initOptionsSection();
            csvReadController.setControls(baseName + "Read");
            convertController.setControls(this, pdfOptionsController);

        } catch (Exception e) {
            MyBoxLog.error(e.toString());
        }
    }

    @Override
    public boolean makeMoreParameters() {
        if (csvReadController.delimiterInput.getStyle().equals(NodeStyleTools.badStyle)
                || (!csvReadController.autoDetermine && csvReadController.charset == null)) {
            return false;
        }
        return super.makeMoreParameters();
    }

    @Override
    public String handleFile(File srcFile, File targetPath) {
        if (csvReadController.withNamesCheck.isSelected()) {
            return withHeader(srcFile, targetPath);
        } else {
            return withoutHeader(srcFile, targetPath);
        }
    }

    public String withHeader(File srcFile, File targetPath) {
        String result;
        CSVFormat csvFormat = CSVFormat.DEFAULT
                .withFirstRecordAsHeader().withDelimiter(csvReadController.delimiter)
                .withIgnoreEmptyLines().withTrim().withNullString("");
        Charset fileCharset;
        if (csvReadController.autoDetermine) {
            fileCharset = TextFileTools.charset(srcFile);
        } else {
            fileCharset = csvReadController.charset;
        }
        try ( CSVParser parser = CSVParser.parse(srcFile, fileCharset, csvFormat)) {
            List<String> names = parser.getHeaderNames();
            convertController.names = names;
            String filePrefix = FileNameTools.getFilePrefix(srcFile.getName());
            convertController.openWriters(filePrefix);
            for (CSVRecord record : parser) {
                List<String> rowData = new ArrayList<>();
                for (String name : names) {
                    rowData.add(record.get(name));
                }
                convertController.writeRow(rowData);
            }
            convertController.closeWriters();
            result = Languages.message("Handled");
        } catch (Exception e) {
            result = e.toString();
        }
        return result;
    }

    public String withoutHeader(File srcFile, File targetPath) {
        String result;
        CSVFormat csvFormat = CSVFormat.DEFAULT
                .withDelimiter(csvReadController.delimiter)
                .withIgnoreEmptyLines().withTrim().withNullString("");
        Charset fileCharset;
        if (csvReadController.autoDetermine) {
            fileCharset = TextFileTools.charset(srcFile);
        } else {
            fileCharset = csvReadController.charset;
        }
        try ( CSVParser parser = CSVParser.parse(srcFile, fileCharset, csvFormat)) {
            List<String> names = null;
            for (CSVRecord record : parser) {
                if (names == null) {
                    names = new ArrayList<>();
                    for (int i = 1; i <= record.size(); i++) {
                        names.add(Languages.message("col") + i);
                    }
                    convertController.names = names;
                    String filePrefix = FileNameTools.getFilePrefix(srcFile.getName());
                    convertController.openWriters(filePrefix);
                }
                List<String> rowData = new ArrayList<>();
                for (int i = 0; i < record.size(); i++) {
                    rowData.add(record.get(i));
                }
                convertController.writeRow(rowData);
            }
            convertController.closeWriters();
            result = Languages.message("Handled");
        } catch (Exception e) {
            result = e.toString();
        }
        return result;
    }

}
