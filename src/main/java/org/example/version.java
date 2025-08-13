package org.example;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.Introspector;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.FileSystemUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class version {

    public static String inputFileList = "ICD_APIs_Corporate_3.5.xlsx";//ICD_APIs_Corporate_3.5.xlsx;ICD_APIs_Acquiring_3.5.xlsx";
    public static String apiRestrictredList = null;//"EnrollCardOnVFC;ManageCardRelationshipOnVFC;";
    public static boolean enableDebugLog = true;
    public static String outputPath = "C:\\Users\\hp\\Downloads\\repo\\";
    public static String inputPath = "C:\\Users\\hp\\Downloads\\repo\\";
    public static String jsonSampleDataPath = inputPath + "//json_sample//";
    public static String pptTemplatePath = inputPath + "//template//template//PPT//API_PPT.pptx";
    public static String staticTemplateDataPath = inputPath + "//template//template//static//";

    public static String dateFormat = "dd-MM-yyyy_HHmmss";
    public static Writer writer;

    public static boolean generateCodeEnabled = false;
    public static boolean generateYamlEnabled = true;
    public static boolean isFirstApi = true;
    public static boolean lastInsertOnModuleDescriptor = false;
    public static String simpleTypes = "c (;c(;n(;n (;date;datetime;num;numeric;double;long;boolean;time;alpha;alphanumeric";
    public static String yamlOutputPath = null;
    public static String apiCodeGenPath = null;
    public static String pptOutputPath = null;
    public static String imgOutputPath = null;
    public static String pwcRelease = "";
    public static String pwcModule = "";
    public static String pwcService = "";
    public static String pwcServiceId = "";
    public static String pwcServiceOverview = "";
    public static String apiName = "";
    public static String apiHttpMethod = "";
    public static String apiVersion = "";
    public static String apiOverview = "";
    public static String apiResources = "";
    public static Boolean alreadyPrinted = false;

    public static Map<String, String> mapApiDBTypes = new HashMap<>();

    public static boolean isComplexType(String type) {
        for (String element : simpleTypes.split(";")) {
            if (type.toLowerCase().startsWith(element) || type.toLowerCase().equalsIgnoreCase("enum")
                    || type.toLowerCase().equalsIgnoreCase("closedenum")) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        try {

            SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
            // JAR
            if (args != null && args.length > 0) {
                inputFileList = args[0];
                if (args.length > 1) {
                    enableDebugLog = (StringUtils.equalsIgnoreCase("true", args[1])) ? true : false;
                }
                if (args.length > 2) {
                    apiRestrictredList = args[2];
                }
                outputPath = "./APIGenerator-Results/" + formatter.format(new Date()) + "/";
                inputPath = "./";
            } else {// IDE Eclipse
                outputPath = outputPath + File.separator + "APIGenerator-Results" + File.separator + formatter.format(new Date()) + File.separator;

                enableDebugLog = true;
            }
            if (StringUtils.isNotEmpty(inputFileList) && !inputFileList.contains(";"))
                inputFileList += ";";

            String[] filesList = inputFileList.split(";");
            if (filesList != null && filesList.length > 0) {

                for (String inputFileName : filesList) {
                    String filePath = inputPath + inputFileName;
                    yamlOutputPath = outputPath + "ws-portal/src/main/react/public/hps/3.5.5/[PWC_SERVICE_ID]/api/schemas/";// [PWC_SERVICE_ID] is replaced by Service ID
                    apiCodeGenPath = outputPath + "code-gen/[PWC_SERVICE_ID]/";
                    pptOutputPath = outputPath + "PPT/[PWC_SERVICE_ID]/";
                    imgOutputPath = outputPath + "ws-portal/src/main/react/public/docs/[PWC_SERVICE_ID]/";
                    new File(yamlOutputPath).mkdirs();
                    new File(pptOutputPath).mkdirs();
                    System.out.println("##################################################");
                    System.out.println("###ICD File   : " + filePath);

                    isFirstApi = true;
                    lastInsertOnModuleDescriptor = false;
                    mapApiDBTypes = new HashMap<>();
                    FileInputStream file = new FileInputStream(new File(filePath));
                    XSSFWorkbook workbook = new XSSFWorkbook(file);
                    for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                        if (i == workbook.getNumberOfSheets() - 1) {
                            lastInsertOnModuleDescriptor = true;
                        }
                        runGeneration(workbook.getSheetAt(i), generateYamlEnabled, generateCodeEnabled);

                        generatePPT(apiName, apiOverview);
                    }
                    workbook.close();
                    // Closing file output streams
                    file.close();
                }
            }

        }

        // Catch block to handle exceptions
        catch (Exception e) {

            // Display the exception along with line number
            // using printStackTrace() method
            e.printStackTrace();
        }

    }


    public static boolean isRowEmpty(Row row) {
        boolean isEmpty = true;
        String data = "";
        for (Cell cell : row) {
            cell.setCellType(CellType.STRING);
            data = data.concat(cell.getStringCellValue());
        }
        if (!data.trim().isEmpty()) {
            isEmpty = false;
        }
        return isEmpty;
    }

    public static void generateCssType(String aggregatName, ArrayList<String> attributList) {
        if (aggregatName.equalsIgnoreCase("REQUESTINFO") || aggregatName.equalsIgnoreCase("RESPONSEINFO"))
            return;

        int counter = 1;
        for (String attribut : attributList) {
            if (counter == 1) {
                printValue("DB", "CREATE OR REPLACE TYPE CSS_"
                        + aggregatName.replace("List", "").replace("V35", "").toUpperCase() + "_V35 FORCE AS OBJECT (");
            }
            String[] row = attribut.split(";");
            printCssTypeRecord(apiName, row[1], row[2], row[3], row[4]);

            counter++;
        }
        printValue("DB", "\t" + StringUtils.rightPad("keyvalues", 30) + "\t\t" + "CSS_TAB_KEYVALUE_V35");
        printValue("DB", ")");
        printValue("DB", "/");
        printValue("DB", "");

    }

    public static void generateBtDesign(String aggregatName, ArrayList<String> attributList) {
        if (aggregatName.equalsIgnoreCase("REQUESTINFO") || aggregatName.equalsIgnoreCase("RESPONSEINFO"))
            return;

        int counter = 1;
        String cssTypeName = "";
        String extendedParent = "";
        for (String attribut : attributList) {
            if (counter == 1) {
                if (aggregatName.endsWith("Rq")) {
                    extendedParent = "com.acp.provider.message.api.AbstractMessageV35Rq";
                } else if (aggregatName.endsWith("Rs")) {
                    extendedParent = "com.acp.provider.message.api.AbstractMessageV35Rs";
                } else {
                    extendedParent = "com.acp.provider.message.api.AbstractAggregateV35";
                }
                printValue("API", "DataTransferObject " + aggregatName + " extends " + extendedParent + " {");
                cssTypeName = "ma.hps.jpub.css_" + aggregatName.replace("List", "").replace("V35", "").toLowerCase()
                        + "_v35";
                mapApiDBTypes.put(aggregatName, cssTypeName);
                printValue("API", "\thint=\"databaseName=" + cssTypeName + "\"");
            }
            String[] row = attribut.split(";");
            if (!(row[1].equalsIgnoreCase("requestinfo") || row[1].equalsIgnoreCase("responseinfo"))) {
                printBtDesignRecord(apiName, row[1], row[2], row[3], row[4]);
            }

            counter++;
        }

        printValue("API", "}");
        printValue("API", "");

    }

    public static void printBtDesignRecord(String apiName, String fieldName, String dataType, String parent,
                                           String occurrence) {
        if (enableDebugLog) {
            System.out.println("-apiName:" + apiName + "	-fieldName:" + fieldName + "	-dataType:" + dataType
                    + "	-parent:" + parent + "	-occurrence:" + occurrence);
        }
        String fieldType = "";
        String fieldLength = "";
        String complexType = "";
        fieldName = Introspector.decapitalize(fieldName);
        if (StringUtils.isNotEmpty(fieldName)) {
            String hintValue = "";
            fieldType = "";
            fieldLength = "";
            complexType = "";
            if (!isComplexType(dataType)) {
                if (occurrence.toLowerCase(Locale.ROOT).startsWith("requir")) {
                    hintValue = "NotEmpty,";
                }
                fieldType = dataType.trim();
                if (dataType.contains("(")) {
                    fieldType = dataType.substring(0, dataType.indexOf("(")).trim();
                    fieldLength = dataType.substring(dataType.indexOf("(") + 1, dataType.indexOf(")"));
                }
                if (fieldType.toLowerCase().equalsIgnoreCase("c")
                        || fieldType.toLowerCase().equalsIgnoreCase("closedenum")
                        || fieldType.toLowerCase().equalsIgnoreCase("enum")) {
                    fieldType = "String";
                    hintValue += "Alphameric,Length=" + fieldLength;
                } else if (fieldType.toLowerCase().startsWith("date")) {
                    hintValue += "CheckDateFormat";
                }
            } else {
                complexType = fieldName.trim().substring(0, 1).toUpperCase() + fieldName.trim().substring(1);
                hintValue += "type=" + complexType + "V35,map=" + mapApiDBTypes.get(complexType) + ",Valid";
            }
            if (enableDebugLog) {
                System.out.println(
                        "-fieldType:" + fieldType + "	-fieldLength:" + fieldLength + "	-hintValue:" + hintValue);
            }
            if (hintValue.toLowerCase().contains(fieldName.toLowerCase())) {
                printValue(parent, "\t" + complexType + "V35 " + fieldName + " hint=\"" + hintValue + "\";");
            } else {
                printValue(parent, "\t" + fieldType + " " + fieldName + " hint=\"" + hintValue + "\";");
            }
        }

    }

    public static void printCssTypeRecord(String apiName, String fieldName, String dataType, String parent,
                                          String occurrence) {
        String fieldType = "";
        String fieldLength = "";
        fieldName = Introspector.decapitalize(fieldName);
        String type = "";
        if (dataType.contains("(")) {
            fieldType = dataType.substring(0, dataType.indexOf("("));
            fieldLength = dataType.substring(dataType.indexOf("(") + 1, dataType.indexOf(")"));
        } else {

            fieldType = dataType;
        }
        if (fieldType.toLowerCase().startsWith("c") && !fieldType.toLowerCase().startsWith("css")
                && !fieldType.toLowerCase().startsWith("complex")) {
            type += "VARCHAR2(" + fieldLength + ")";
        } else if (fieldType.toLowerCase().startsWith("date")) {
            type += "DATE";
        } else if (fieldType.toLowerCase().startsWith("complex")) {
            type = "CSS_" + fieldName + "_V35";
        } else {

            type += fieldType;
        }

        printValue(parent, "\t" + StringUtils.rightPad(fieldName, 30) + "\t\t" + type.toUpperCase() + ",");

    }

    public static void printValue(String aggregatName, String value) {
        try {
            if (aggregatName.equalsIgnoreCase(apiName + "V35Rq")) {
                writer = new PrintWriter(new BufferedWriter(new FileWriter(yamlOutputPath + "request.yaml", true)));
            } else if (aggregatName.equalsIgnoreCase(apiName + "V35Rs")) {
                writer = new PrintWriter(new BufferedWriter(new FileWriter(yamlOutputPath + "response.yaml", true)));
            } else if (aggregatName.equalsIgnoreCase("DB")) {
                writer = new PrintWriter(new BufferedWriter(
                        new FileWriter(apiCodeGenPath + "/" + apiName + "/" + apiName + "_codeSQL.sql", true)));
            } else if (aggregatName.equalsIgnoreCase("API")) {
                writer = new PrintWriter(new BufferedWriter(
                        new FileWriter(apiCodeGenPath + "/" + apiName + "/" + apiName + "_codeJAVA.txt", true)));
            } else {//aggregate
                writer = new PrintWriter(new BufferedWriter(new FileWriter(yamlOutputPath + "aggregate.yaml", true)));
            }
            printValue(value, false);
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void printValue(String value, boolean useTabulation) {
        try {
            String tabulation = useTabulation ? tabulation = "    " : "";
            writer.write(tabulation + value + "\n");
        } catch (IOException ex) {
            // Report
        }
    }

    public static void runGeneration(XSSFSheet sheet, boolean yamlEnabled, boolean cssEnabled) {
        String msgType = null;
        try {
            Map<String, List<FieldNode>> complexTypes = new LinkedHashMap<String, List<FieldNode>>();
            Map<String, String> errorCodeList = new LinkedHashMap<String, String>();

            //Request
            Iterator<Row> rowIterator = sheet.iterator();
            Map<String, List<FieldNode>> nodeMap = new HashMap<>();
            Map<String, String> fieldNameToParentPath = new HashMap<>();

            List<FieldNode> roots = new ArrayList<>();
            int counterMsg = 0;
            int generalRowId = 0;
            Iterator<Row> generalIterator = sheet.iterator();
            while (generalIterator.hasNext()) {
                Row parentRow = generalIterator.next();
                generalRowId = parentRow.getRowNum();
                if (parentRow.getCell(1) != null && parentRow.getCell(1).getStringCellValue() != null) {
                    setApiIdentification(parentRow);
                }
                if (parentRow != null && parentRow.getCell(1) != null
                        && parentRow.getCell(1).getStringCellValue().equalsIgnoreCase("Field Name")) {
                    counterMsg++;
                    if (counterMsg == 1) {
                        msgType = apiName + "V35Rq";
                        break;
                    }
                }
            }
            for (int i = generalRowId + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String fieldName = StringUtils.capitalize(getCellValue(row, 1));
                String dataType = getCellValue(row, 2);
                String parent = StringUtils.capitalize(getCellValue(row, 3));
                String occurrence = getCellValue(row, 4);
                String fieldDescription = getCellValue(row, 5);

                if (row.getCell(1) != null && row.getCell(1).getStringCellValue().equalsIgnoreCase("Field Name"))
                    break;

                FieldNode node = new FieldNode(fieldName, dataType, parent, occurrence, fieldDescription);
                // Get parent's full path from fieldNameToParentPath or use "-" as root
                String parentFullPath = parent == null || parent.equals("-") ? "-" : fieldNameToParentPath.getOrDefault(parent, parent);
                // Build current node full path
                String currentFullPath = buildPath(parentFullPath, fieldName);
                // Save mapping for this field
                fieldNameToParentPath.put(fieldName, currentFullPath);
                // Then put the node into parentPathMap under parentFullPath
                nodeMap.computeIfAbsent(parentFullPath, k -> new ArrayList<>()).add(node);
            }
            FieldNode root = buildTree("-", nodeMap);
            roots.add(root);
            complexTypes.put(msgType, roots);

            //Response
            rowIterator = sheet.iterator();
            nodeMap = new LinkedHashMap<>();
            fieldNameToParentPath = new HashMap<>();
            roots = new ArrayList<>();
            counterMsg = 0;
            generalRowId = 0;
            generalIterator = sheet.iterator();
            while (generalIterator.hasNext()) {
                Row parentRow = generalIterator.next();
                generalRowId = parentRow.getRowNum();
                if (parentRow != null && parentRow.getCell(1) != null
                        && parentRow.getCell(1).getStringCellValue().equalsIgnoreCase("Field Name")) {
                    counterMsg++;
                    if (counterMsg == 2) {
                        msgType = apiName + "V35Rs";
                        break;
                    }
                }
            }
            for (int i = generalRowId + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String fieldName = StringUtils.capitalize(getCellValue(row, 1));
                String dataType = getCellValue(row, 2);
                String parent = StringUtils.capitalize(getCellValue(row, 3));
                String occurrence = getCellValue(row, 4);
                String fieldDescription = getCellValue(row, 5);

                if (row.getCell(1) != null && row.getCell(1).getStringCellValue().equalsIgnoreCase("Field Name"))
                    break;

                FieldNode node = new FieldNode(fieldName, dataType, parent, occurrence, fieldDescription);
                // Get parent's full path from fieldNameToParentPath or use "-" as root
                String parentFullPath = parent == null || parent.equals("-") ? "-" : fieldNameToParentPath.getOrDefault(parent, parent);
                // Build current node full path
                String currentFullPath = buildPath(parentFullPath, fieldName);
                // Save mapping for this field
                fieldNameToParentPath.put(fieldName, currentFullPath);
                // Then put the node into parentPathMap under parentFullPath
                nodeMap.computeIfAbsent(parentFullPath, k -> new ArrayList<>()).add(node);
            }
            root = buildTree("-", nodeMap);
            roots.add(root);
            complexTypes.put(msgType, roots);

            //ErrorCode+ImpactedTables
            generalIterator = sheet.iterator();
            while (generalIterator.hasNext()) {
                Row parentRow = generalIterator.next();
                generalRowId = parentRow.getRowNum();

                if (parentRow != null && parentRow.getCell(4) != null
                        && parentRow.getCell(4).getStringCellValue().equalsIgnoreCase("Error code")) {
                    rowIterator = sheet.iterator();
                    String errorCode = null;
                    String errorDesc = null;

                    while (rowIterator.hasNext()) {
                        Row row = rowIterator.next();
                        int parentRowId = row.getRowNum();
                        if (row != null && !isRowEmpty(row) && row.getCell(4) != null && row.getCell(4).getStringCellValue().equalsIgnoreCase("Impacted Tables"))
                            break;
                        if (row != null && !isRowEmpty(row) && row.getCell(4) != null && generalRowId < parentRowId) {
                            if (row.getCell(5) == null)
                                break;

                            errorCode = row.getCell(4).getStringCellValue();
                            errorDesc = row.getCell(5).getStringCellValue();
                            if (errorCode != null)
                                errorCodeList.put(errorCode, errorDesc);
                        }

                    }
                } else if (parentRow != null && parentRow.getCell(4) != null
                        && parentRow.getCell(4).getStringCellValue().equalsIgnoreCase("Impacted Tables")) {
                    rowIterator = sheet.iterator();
                    String tableName = null;
                    apiResources = "";
                    if (parentRow.getCell(5) != null)
                        tableName = parentRow.getCell(5).getStringCellValue();
                    if (tableName != null)
                        apiResources = tableName + ";";
                    while (rowIterator.hasNext()) {
                        Row row = rowIterator.next();
                        int parentRowId = row.getRowNum();
                        if (row != null && !isRowEmpty(row) && row.getCell(5) != null && generalRowId < parentRowId) {
                            tableName = row.getCell(5).getStringCellValue();
                            if (tableName != null)
                                apiResources += tableName + ";";
                        }

                    }
                }

            }

            if (isApiEnabledToBeGenerated(apiName)) {
                if (yamlEnabled) {
                    // module-descriptor.json
                    writer = new PrintWriter(
                            new BufferedWriter(new FileWriter(yamlOutputPath + "module-descriptor.json", true)));
                    if (isFirstApi) {
                        FileSystemUtils.copyRecursively(new File(staticTemplateDataPath), new File(yamlOutputPath));
                        printValue(getDefaultModuleDescriptor().replace("[API_MODULE]", pwcModule).replace("[PWC_SERVICE_ID]",
                                        pwcService.replaceAll("[\\\r]+", "\r            <br>").replaceAll("[\\\n]+",
                                                "\n            <br>")),
                                false);
                    }

                    printValue("{\r\n" + "          \"name\":\"" + getApiNameToPrint(apiName, pwcServiceId) + "\",\r\n" + "          \"file\": \"" + apiName
                            + ".yaml\",\r\n" + "          \"subApiList\": [\r\n" + "            {\r\n"
                            + "              \"file\": \"" + apiName + ".yaml\",\r\n" + "              \"name\": \"" + getApiNameToPrint(apiName, pwcServiceId)
                            + " " + apiVersion + "\",\r\n" + "              \"HttpVerb\": \"" + apiHttpMethod + "\"\r\n"
                            + "            }\r\n" + "          ]\r\n" + "        }"
                            + ((!lastInsertOnModuleDescriptor) ? "," : ""), false);

                    if (lastInsertOnModuleDescriptor)
                        printValue("]\r\n" + "    }\r\n" + "  ]\r\n" + "}", false);

                    writer.close();

                    // default template request
                    writer = new PrintWriter(new BufferedWriter(new FileWriter(yamlOutputPath + "request.yaml", true)));
                    if (isFirstApi)
                        printValue(getDefaultRequest(), false);
                    writer.close();

                    // default template response
                    writer = new PrintWriter(new BufferedWriter(new FileWriter(yamlOutputPath + "response.yaml", true)));
                    if (isFirstApi)
                        printValue(getDefaultResponse(), false);
                    writer.close();

                    // default template aggregate
                    writer = new PrintWriter(new BufferedWriter(new FileWriter(yamlOutputPath + "aggregate.yaml", true)));
                    if (isFirstApi)
                        printValue(getDefaultAggregate(), false);
                    writer.close();

                    writer = new PrintWriter(new BufferedWriter(new FileWriter(yamlOutputPath + apiName + ".yaml", true)));

                    String errorListForYamlFile = "";
                    for (Map.Entry<String, String> entry : errorCodeList.entrySet()) {
                        if (entry != null && entry.getKey() != null) {
                            errorListForYamlFile += "              <tr><td>" + entry.getKey() + "</td><td>"
                                    + entry.getValue() + "</td></tr>\n";
                        }
                    }
                    printValue(getDefaultHeader().replace("[API_MODULE]", pwcModule)
                            .replace("[PWC_SERVICE_OVERVIEW]",
                                    pwcServiceOverview.replaceAll("[\\\r]+", "\r            <br>").replaceAll("[\\\n]+",
                                            "\n            <br>"))
                            .replace("[PWC_SERVICE_ID]", pwcServiceId).replace("[PWC_RELEASE]", pwcRelease)
                            .replace("[API_PRINTED_NAME]", getApiNameToPrint(apiName, pwcServiceId))
                            .replace("[API_NAME]", apiName)
                            .replace("[API_VERSION]", apiVersion)
                            .replace("[API_HTTP_METHOD_lower]", apiHttpMethod.toLowerCase())
                            .replace("[API_OVERVIEW]",
                                    apiOverview.replaceAll("[\\\r]+", "\r            <br>").replaceAll("[\\\n]+",
                                            "\n            <br>"))
                            .replace("[API_ERROR_LIST]", errorListForYamlFile), false);
                    writer.close();


                    ArrayList<String> printedMainAggregates = new ArrayList<String>();
                    for (Map.Entry<String, List<FieldNode>> entry : complexTypes.entrySet()) {
                        printYamlAggregate(entry.getKey(), entry.getValue(), printedMainAggregates);

                    }
                    generateDynamicJsonSamples(complexTypes);
                }

                isFirstApi = false;
            }
        }

        // Catch block to handle exceptions
        catch (Exception e) {

            // Display the exception along with line number
            // using printStackTrace() method
            e.printStackTrace();
        }
    }

    public static void generateDynamicJsonSamples(Map<String, List<FieldNode>> complexTypes) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            // Générer le JSON de requête
            com.fasterxml.jackson.databind.node.ObjectNode requestJson = buildJsonFromStructure(complexTypes.get(apiName + "V35Rq"));
            mapper.writerWithDefaultPrettyPrinter().writeValue(
                    new File(jsonSampleDataPath + pwcServiceId + "/" + apiName + "_" + apiVersion + "_request.json"),
                    requestJson
            );

            // Générer le JSON de réponse
            ObjectNode responseJson = buildJsonFromStructure(complexTypes.get(apiName + "V35Rs"));
            mapper.writerWithDefaultPrettyPrinter().writeValue(
                    new File(jsonSampleDataPath + pwcServiceId + "/" + apiName + "_" + apiVersion + "_response.json"),
                    responseJson
            );

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static com.fasterxml.jackson.databind.node.ObjectNode buildJsonFromStructure(List<FieldNode> nodes) {
        ObjectMapper mapper = new ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode rootNode = mapper.createObjectNode();

        if (nodes == null || nodes.isEmpty()) {
            return rootNode;
        }

        for (FieldNode node : nodes.get(0).getChildren()) {
            addNodeToJson(rootNode, node);
        }

        return rootNode;
    }

    private static void addNodeToJson(com.fasterxml.jackson.databind.node.ObjectNode parentNode, FieldNode node) {
        String fieldName = node.getJsonFieldName();

        if (node.isArrayType()) {
            com.fasterxml.jackson.databind.node.ArrayNode arrayNode = parentNode.arrayNode();
            parentNode.set(fieldName, arrayNode);

            // Élément de la liste
            com.fasterxml.jackson.databind.node.ObjectNode itemNode = arrayNode.addObject();

            // Si le tableau contient un seul objet complexe, on saute son nom et on ajoute directement ses champs
            if (node.getChildren().size() == 1 && isComplexType(node.getChildren().get(0).getDataType())) {
                for (FieldNode grandChild : node.getChildren().get(0).getChildren()) {
                    addNodeToJson(itemNode, grandChild);
                }
            } else {
                for (FieldNode child : node.getChildren()) {
                    addNodeToJson(itemNode, child);
                }
            }

        } else if (isComplexType(node.getDataType()) || !isComplexType(node.getDataType())  ) {
            com.fasterxml.jackson.databind.node.ObjectNode childNode = parentNode.objectNode();
            parentNode.set(fieldName, childNode);

            for (FieldNode child : node.getChildren()) {
                addNodeToJson(childNode, child);
            }
        } else {
            String exampleValue = generateExampleValue(node);
            if (!exampleValue.isEmpty()) {
                parentNode.put(fieldName, exampleValue);
            }
        }
    }


    private static String generateExampleValue(FieldNode node) {
        // Génère des valeurs d'exemple basées sur le type de données
        String fieldName = node.getFieldName().toLowerCase();
        String dataType = node.getDataType().toLowerCase();

        if (dataType.contains("date")) {
            return " ";
        } else if (dataType.contains("amount") || dataType.contains("numeric")) {
            return " ";
        } else if (dataType.contains("code")) {
            return fieldName.substring(0, 3).toUpperCase() + "001";
        } else if (fieldName.contains("flag") || fieldName.contains("indicator")) {
            return " ";
        } else if (fieldName.contains("id")) {
            return " ";
        } else if (fieldName.contains("name")) {
            return " ";
        } else if (fieldName.contains("email")) {
            return "";
        } else if (fieldName.contains("phone")) {
            return " ";
        }

        return "";
    }

    public static void printYamlAggregate(String aggregatName, List<FieldNode> attributList, List<String> printedMainAggregates) {
        alreadyPrinted=false;
        for(String printedAgg:printedMainAggregates) {
            if(printedAgg.equalsIgnoreCase(aggregatName+"_"+apiName)) {
                alreadyPrinted=true;
                break;
            }
        }
        if(!alreadyPrinted) {
            printValue(aggregatName,"# ---- " + StringUtils.capitalize(aggregatName) + " definition");
            printValue(aggregatName,StringUtils.capitalize(aggregatName) + ":");
            printValue(aggregatName,"      type: object");
            String requiredElements = "";
            boolean requiredElementsExist = false;

            for (FieldNode child : attributList.get(0).children) {
                if (child.occurrence.toLowerCase().contains("required")) {
                    requiredElements = requiredElements + Introspector.decapitalize(child.fieldName) + ";";
                    requiredElementsExist = true;
                }
            }
            if (requiredElementsExist) {
                printValue(aggregatName,"      required:");
                for (String requiredElmt : requiredElements.split(";")) {
                    if (requiredElmt.toLowerCase().startsWith(pwcServiceId.toLowerCase())) {
                        requiredElmt = requiredElmt.replace(pwcServiceId, pwcServiceId.toLowerCase());
                    }
                    printValue(aggregatName,"    - " + requiredElmt);
                }
                requiredElements = "";
                requiredElementsExist = false;
            }
            printValue(aggregatName,"      properties:");
            for (FieldNode node : attributList.get(0).children) {
                recursivePrint(aggregatName, node,printedMainAggregates);
            }
            printedMainAggregates.add(aggregatName+"_"+apiName);
        }

    }

    public static void recursivePrint(String aggregatName, FieldNode node,List<String> printedAggregates) {
        Map<String, String> fieldToFullPath = buildFieldPaths(node.children);
        String fieldName;
        String dataType;
        String parent;
        String occurrence;
        String description;
        if(node!=null && !StringUtils.isEmpty(node.fieldName)) {
            fieldName = Introspector.decapitalize(node.fieldName);
            dataType=node.dataType;
            parent=node.parent;
            occurrence=node.occurrence;
            description=node.fieldDescription;
            if (StringUtils.isNotEmpty(description)) {
                description = description.replaceAll("[\\\r]+", "\r            <br>").replaceAll("[\\\n]+","\n            <br>");
            }

            if (fieldName.toLowerCase().startsWith(pwcServiceId.toLowerCase())) {
                fieldName = fieldName.replace(pwcServiceId, pwcServiceId.toLowerCase());
            }
            if (isComplexType(node.dataType) && !aggregatName.equalsIgnoreCase(apiName+"V35Rq") && !aggregatName.equalsIgnoreCase(apiName+"V35Rs")) {
                aggregatName=node.fieldName +apiName;
            }
            if (isComplexType(dataType) || !isComplexType(dataType)) {
                boolean subFieldAlreadyPrinted=false;
                for(String printedAgg:printedAggregates) {
                    if(printedAgg.equalsIgnoreCase(fieldName+"_"+parent+"_"+apiName)) {
                        subFieldAlreadyPrinted=true;
                        break;
                    }
                }
                if(!subFieldAlreadyPrinted) {
                    boolean skipObjectDeclaration=false;
                    for(String printedAgg:printedAggregates) {
                        if(printedAgg.equalsIgnoreCase(fieldName+"_"+parent+"_"+apiName)) {
                            skipObjectDeclaration=true;
                            break;
                        }
                    }
                    if(node.children!=null && node.children.size()>0) {
                        if(!skipObjectDeclaration) {
                            printedAggregates.add(parent+"_"+apiName);
                            printedAggregates.add(fieldName+"_"+apiName);
//                            printValue(aggregatName,"    " + fieldName + ":");
//                            printValue(aggregatName,"        $ref: 'aggregate.yaml#/" +StringUtils.capitalize(fieldName) + apiName + "'");
                            if(!fieldName.toLowerCase().endsWith("list")) {
//								printValue(aggregatName, fieldName +apiName+ ":");
                            }
                        }

                        if (isComplexType(node.dataType) && !node.fieldName.equalsIgnoreCase(apiName+"V35Rq") && !node.fieldName.equalsIgnoreCase(apiName+"V35Rs")) {
                            aggregatName=node.fieldName +apiName;
                        }
                        subFieldAlreadyPrinted=false;
                        for(String printedAgg:printedAggregates) {
                            if(printedAgg.equalsIgnoreCase(aggregatName+"_"+apiName)) {
                                subFieldAlreadyPrinted=true;
                                break;
                            }
                        }
                        if(!subFieldAlreadyPrinted){

                            printedAggregates.add(aggregatName+"_"+apiName);
                            if(fieldName.toLowerCase().endsWith("list") ) {
                                //printValue(aggregatName,"        $ref: 'aggregate.yaml#/" + StringUtils.capitalize(fieldName).replace("List","").replace("list","") + apiName + "'");
                                printValue(aggregatName,"# ---- " + StringUtils.capitalize(aggregatName) + " definition");
                                printValue(aggregatName,StringUtils.capitalize(aggregatName) + ":");
                                printValue(aggregatName,"      type: array");
                                printValue(aggregatName,"      description: >");
                                printValue(aggregatName,"        " + description);
                                if (occurrence!=null && occurrence.contains("(")) {
                                    String maxItems = occurrence.substring(occurrence.indexOf("(") + 1, occurrence.indexOf(")"));
                                    if(maxItems!=null) {
                                        // Replacing every non-digit character with a space (" ")
                                        maxItems = maxItems.replaceAll("[^\\d]", " ");
                                        // Remove extra spaces from the beginning and the end of the string
                                        maxItems = maxItems.trim();
                                        // Replace all consecutive white spaces with a single space
                                        maxItems = maxItems.replaceAll(" +", " ");
                                        if (!maxItems.equals("")) {
                                            printValue(aggregatName,"      maxItems: "+maxItems);
                                        }
                                    }
                                }
                                String baseFieldName = fieldName;

                                // Cas spécial : "CorporateAddressListCreateCorporate" → "AddressCreateCorporate"
                                    // Sinon, comportement générique : suppression de "list" à la fin
                                    baseFieldName = fieldName.replaceAll("(?i)list$", "").trim();

                                String refName = StringUtils.capitalize(baseFieldName);
                                printValue(aggregatName,"      items: ");
                                printValue(aggregatName,"        $ref: 'aggregate.yaml#/" + refName + apiName + "'");
                                printedAggregates.add(aggregatName + "_" + apiName);


                            }
                            else {
                                printValue(aggregatName,"# ---- " + StringUtils.capitalize(aggregatName) + " definition");
                                printedAggregates.add(aggregatName+"_"+apiName);
                                printValue(aggregatName,StringUtils.capitalize(aggregatName) + ":");
                                printValue(aggregatName,"  type: object");
                                String requiredElements = "";
                                boolean requiredElementsExist = false;
                                for (FieldNode child : node.children) {
                                    if (child.occurrence.toLowerCase().contains("required")) {
                                        requiredElements = requiredElements + Introspector.decapitalize(child.fieldName) + ";";
                                        requiredElementsExist = true;
                                    }
                                }

                                if (requiredElementsExist) {
                                    printValue(aggregatName,"  required:");
                                    for (String requiredElmt : requiredElements.split(";")) {
                                        if (requiredElmt.toLowerCase().startsWith(pwcServiceId.toLowerCase())) {
                                            requiredElmt = requiredElmt.replace(pwcServiceId, pwcServiceId.toLowerCase());
                                        }
                                        printValue(aggregatName,"    - " + requiredElmt);

                                    }
                                    requiredElements = "";
                                    requiredElementsExist = false;
                                }
                                printValue(aggregatName,"  properties:");
                                Map<String,FieldNode> subComplexTypes = new LinkedHashMap<String,FieldNode>();
                                for(FieldNode child:node.children) {
                                    if(isComplexType(child.dataType)){
                                        String refName = StringUtils.capitalize(child.fieldName) + apiName;
//                                    if (child.fieldName.equalsIgnoreCase(child.parent.replace("List", "").replace("list", ""))) {
//                                        continue;
//                                    }
                                        // Écriture du $ref dans la propriété
                                        printValue(aggregatName, "    " + child.fieldName + ":");
                                        printValue(aggregatName, "        $ref: 'aggregate.yaml#/" + refName + "'");
                                    }
                                    subFieldAlreadyPrinted=false;
                                    fieldName = Introspector.decapitalize(child.fieldName);
                                    dataType=child.dataType;
                                    parent=child.parent;
                                    occurrence=child.occurrence;
                                    description=child.fieldDescription;
                                    if (StringUtils.isNotEmpty(description)) {
                                        description = description.replaceAll("[\\\r]+", "\r            <br>").replaceAll("[\\\n]+","\n            <br>");
                                    }

                                    if (fieldName.toLowerCase().startsWith(pwcServiceId.toLowerCase())) {
                                        fieldName = fieldName.replace(pwcServiceId, pwcServiceId.toLowerCase());
                                    }
                                    if (!child.parent.equalsIgnoreCase("-") && !child.fieldName.equalsIgnoreCase(apiName+"V35Rq") && !child.fieldName.equalsIgnoreCase(apiName+"V35Rs")) {
                                        aggregatName=child.fieldName +apiName;
                                    }
                                    for (String elemnt : printedAggregates) {
                                        if(elemnt.equalsIgnoreCase(fieldName+"_"+parent+"_"+apiName)) {
                                            subFieldAlreadyPrinted=true;
                                            break;
                                        }
                                    }
                                    if(!subFieldAlreadyPrinted) {
                                        if (isComplexType(dataType)) {
                                            skipObjectDeclaration = false;
                                            for (String printedAgg : printedAggregates) {
                                                if (printedAgg.equalsIgnoreCase(parent + "_" + apiName)) {
                                                    skipObjectDeclaration = true;
                                                    break;
                                                }
                                            }
                                            if (!skipObjectDeclaration) {
                                                printValue(aggregatName, "    " + fieldName + ":");
                                                printedAggregates.add(parent + "_" + apiName);
                                                if (fieldName.toLowerCase().endsWith("list")) {
                                                    printValue(aggregatName, "      type: array");
                                                    printValue(aggregatName, "      description: >");
                                                    printValue(aggregatName, "        " + description);
                                                    if (occurrence != null && occurrence.contains("(")) {
                                                        String maxItems = occurrence.substring(occurrence.indexOf("(") + 1, occurrence.indexOf(")"));
                                                        if (maxItems != null) {
                                                            // Replacing every non-digit character with a space (" ")
                                                            maxItems = maxItems.replaceAll("[^\\d]", " ");
                                                            // Remove extra spaces from the beginning and the end of the string
                                                            maxItems = maxItems.trim();
                                                            // Replace all consecutive white spaces with a single space
                                                            maxItems = maxItems.replaceAll(" +", " ");
                                                            if (!maxItems.equals("")) {
                                                                printValue(aggregatName, "      maxItems: " + maxItems);
                                                            }
                                                        }
                                                    }
                                                    printValue(aggregatName, "      items: ");
                                                    //  printValue(aggregatName,"        $ref: 'aggregate.yaml#/" + StringUtils.capitalize(fieldName).replace("List","").replace("list","") + apiName + "'");
                                                } else {
                                                    printValue(aggregatName, "      $ref: 'aggregate.yaml#/kenza" + StringUtils.capitalize(fieldName) + apiName + "'");
                                                }
                                            }
                                        } else {
                                            printValue(aggregatName, "    " + fieldName + ":");
                                            printValue(aggregatName, "      type: string");
                                            if (fieldName.toLowerCase().equalsIgnoreCase("resultID")) {
                                                printValue(aggregatName, "      enum: [ProceedWithSuccess, ProceedWithSuccessMC, Error, SystemError]");
                                            } else {
                                                if (dataType.toLowerCase().contains("(")) {
                                                    printValue(aggregatName, "      maxLength: "
                                                            + dataType.substring(dataType.indexOf("(") + 1, dataType.indexOf(")")));
                                                } else if (dataType.toLowerCase().contains("date")) {
                                                    printValue(aggregatName, "      format: date-time");
                                                }
                                            }
                                            printValue(aggregatName, "      description: >");
                                            printValue(aggregatName, "        " + description);

                                            if (!dataType.toLowerCase().contains("date")) {//ne pas générer exmeple data pr les champs de type date pr qu'il prend sysdate par defaut
                                                printValue(aggregatName, "      example: '"
                                                        + getJsonSampleData(apiName, apiVersion, Introspector.decapitalize(parent), child, fieldToFullPath)
                                                        + "'");
                                            }
                                        }}}

                            }

                            String requiredElements = "";
                            boolean requiredElementsExist = false;
                            for (FieldNode child : node.children) {
                                if (child.occurrence.toLowerCase().contains("required")) {
                                    requiredElements = requiredElements + Introspector.decapitalize(child.fieldName) + ";";
                                    requiredElementsExist = true;
                                }
                            }

                            if (requiredElementsExist) {
                               // printValue(aggregatName,"  required:");
                                for (String requiredElmt : requiredElements.split(";")) {
                                    if (requiredElmt.toLowerCase().startsWith(pwcServiceId.toLowerCase())) {
                                        requiredElmt = requiredElmt.replace(pwcServiceId, pwcServiceId.toLowerCase());
                                    }
                                  //  printValue(aggregatName,"    - " + requiredElmt);

                                }
                                requiredElements = "";
                                requiredElementsExist = false;
                            }
                           // printValue(aggregatName,"  properties:");
                            Map<String,FieldNode> subComplexTypes = new LinkedHashMap<String,FieldNode>();
                            for(FieldNode child:node.children) {
                                if(isComplexType(child.dataType)){
                                    String refName = StringUtils.capitalize(child.fieldName) + apiName;
//                                    if (child.fieldName.equalsIgnoreCase(child.parent.replace("List", "").replace("list", ""))) {
//                                        continue;
//                                    }
                                    // Écriture du $ref dans la propriété
//                                    printValue(aggregatName, "    " + child.fieldName + ":");
//                                    printValue(aggregatName, "        $ref: 'aggregate.yaml#/najlaa" + refName + "'");
                                }
                                subFieldAlreadyPrinted=false;
                                fieldName = Introspector.decapitalize(child.fieldName);
                                dataType=child.dataType;
                                parent=child.parent;
                                occurrence=child.occurrence;
                                description=child.fieldDescription;
                                if (StringUtils.isNotEmpty(description)) {
                                    description = description.replaceAll("[\\\r]+", "\r            <br>").replaceAll("[\\\n]+","\n            <br>");
                                }

                                if (fieldName.toLowerCase().startsWith(pwcServiceId.toLowerCase())) {
                                    fieldName = fieldName.replace(pwcServiceId, pwcServiceId.toLowerCase());
                                }
                                if (!child.parent.equalsIgnoreCase("-") && !child.fieldName.equalsIgnoreCase(apiName+"V35Rq") && !child.fieldName.equalsIgnoreCase(apiName+"V35Rs")) {
                                    aggregatName=child.fieldName +apiName;
                                }
                                for (String elemnt : printedAggregates) {
                                    if(elemnt.equalsIgnoreCase(fieldName+"_"+parent+"_"+apiName)) {
                                        subFieldAlreadyPrinted=true;
                                        break;
                                    }
                                }
                                if(!subFieldAlreadyPrinted) {
                                    if (isComplexType(dataType)) {
                                        skipObjectDeclaration=false;
                                        for(String printedAgg:printedAggregates) {
                                            if(printedAgg.equalsIgnoreCase(parent+"_"+apiName)) {
                                                skipObjectDeclaration=true;
                                                break;
                                            }
                                        }
                                        if(!skipObjectDeclaration) {
//                                            printValue(aggregatName,"    " + fieldName + ":");
//                                            printedAggregates.add(parent+"_"+apiName);
                                            if(fieldName.toLowerCase().endsWith("list")) {
//                                                printValue(aggregatName,"      type: array");
//                                                printValue(aggregatName,"      description: >");
//                                                printValue(aggregatName,"        " + description);
                                                if (occurrence!=null && occurrence.contains("(")) {
                                                    String maxItems = occurrence.substring(occurrence.indexOf("(") + 1, occurrence.indexOf(")"));
                                                    if(maxItems!=null) {
                                                        // Replacing every non-digit character with a space (" ")
                                                        maxItems = maxItems.replaceAll("[^\\d]", " ");
                                                        // Remove extra spaces from the beginning and the end of the string
                                                        maxItems = maxItems.trim();
                                                        // Replace all consecutive white spaces with a single space
                                                        maxItems = maxItems.replaceAll(" +", " ");
                                                        if (!maxItems.equals("")) {
                                                            printValue(aggregatName,"      maxItems: "+maxItems);
                                                        }
                                                    }
                                                }
//                                                printValue(aggregatName,"      items: ");
                                                //  printValue(aggregatName,"        $ref: 'aggregate.yaml#/" + StringUtils.capitalize(fieldName).replace("List","").replace("list","") + apiName + "'");
                                            }else {
//                                                printValue(aggregatName,"      $ref: 'aggregate.yaml#/kenza" + StringUtils.capitalize(fieldName) + apiName + "'");
                                            }
                                        }
//                                    }else {
//                                        printValue(aggregatName,"    " + fieldName + ":");
//                                        printValue(aggregatName,"      type: string");
                                        if (fieldName.toLowerCase().equalsIgnoreCase("resultID")) {
//                                            printValue(aggregatName,"      enum: [ProceedWithSuccess, ProceedWithSuccessMC, Error, SystemError]");
                                        } else {
                                            if (dataType.toLowerCase().contains("(")) {
//                                                printValue(aggregatName,"      maxLength: "
//                                                        + dataType.substring(dataType.indexOf("(") + 1, dataType.indexOf(")")));
                                            } else if (dataType.toLowerCase().contains("date")) {
//                                                printValue(aggregatName,"      format: date-time");
                                            }
                                        }
//                                        printValue(aggregatName,"      description: >");
//                                        printValue(aggregatName,"        " + description);

                                        if (!dataType.toLowerCase().contains("date")) {//ne pas générer exmeple data pr les champs de type date pr qu'il prend sysdate par defaut
//                                            printValue(aggregatName,"      example: '"
//                                                    + getJsonSampleData(apiName, apiVersion, Introspector.decapitalize(parent), child,fieldToFullPath)
//                                                    + "'");
                                        }

                                    }
                                    if(child.children!=null && child.children.size()>0)
                                        subComplexTypes.put(child.fieldName, child);
                                }
                            }


                            if(subComplexTypes!=null && subComplexTypes.size()>0) {
                                for (Map.Entry<String, FieldNode> entry : subComplexTypes.entrySet()) {
                                    recursivePrint(entry.getKey(), entry.getValue(),printedAggregates);

                                }
                                subComplexTypes = new LinkedHashMap<String,FieldNode>();
                            }

                        }

                    }else {

                        printValue(aggregatName,"    " + fieldName + ":");

                        if(fieldName.toLowerCase().endsWith("list")) {
                            printValue(aggregatName,"      type: array");
                            printValue(aggregatName,"      description: >");
                            printValue(aggregatName,"        " + description);
                            if (occurrence!=null && occurrence.contains("(")) {
                                String maxItems = occurrence.substring(occurrence.indexOf("(") + 1, occurrence.indexOf(")"));
                                if(maxItems!=null) {
                                    // Replacing every non-digit character with a space (" ")
                                    maxItems = maxItems.replaceAll("[^\\d]", " ");
                                    // Remove extra spaces from the beginning and the end of the string
                                    maxItems = maxItems.trim();
                                    // Replace all consecutive white spaces with a single space
                                    maxItems = maxItems.replaceAll(" +", " ");
                                    if (!maxItems.equals("")) {
                                        printValue(aggregatName,"      maxItems: "+maxItems);
                                    }
                                }
                            }
                            printValue(aggregatName,"      items: ");
                            //printValue(aggregatName,"        $ref: 'aggregate.yaml#/" + StringUtils.capitalize(fieldName).replace("List","").replace("list","") + apiName + "'");
                        }else {
                            printValue(aggregatName,"      $ref: 'aggregate.yaml#/rabia" + StringUtils.capitalize(fieldName) + apiName + "'");
                        }
                    }
                }
            } else {
                printValue(aggregatName,"    " + fieldName + ":");
                printValue(aggregatName,"      type: string");
                if (fieldName.toLowerCase().equalsIgnoreCase("resultID")) {
                    printValue(aggregatName,"      enum: [ProceedWithSuccess, ProceedWithSuccessMC, Error, SystemError]");
                } else {
                    if (dataType.toLowerCase().contains("(")) {
                        printValue(aggregatName,"      maxLength: "
                                + dataType.substring(dataType.indexOf("(") + 1, dataType.indexOf(")")));
                    } else if (dataType.toLowerCase().contains("date")) {
                        printValue(aggregatName,"      format: date-time");
                    }
                }
                printValue(aggregatName,"      description: >");
                printValue(aggregatName,"        " + description);

                if (!dataType.toLowerCase().contains("date")) {//ne pas générer exmeple data pr les champs de type date pr qu'il prend sysdate par defaut
                    printValue(aggregatName,"      example: '"
                            + getJsonSampleData(apiName, apiVersion, Introspector.decapitalize(parent),node,fieldToFullPath)
                            + "'");
                }

            }

        }
    }

    public static String getDefaultModuleDescriptor() {
        return "{\r\n" + "  \"modules\":[\r\n" + "    {\r\n" + "      \"moduleName\":\"Api basics\",\r\n"
                + "      \"expanded\": false,\r\n" + "      \"apis\":[\r\n" + "        {\r\n"
                + "          \"name\":\"Authentication\",\r\n" + "          \"file\": \"tokenAuthentication.yaml\",\r\n"
                + "          \"subApiList\": [\r\n" + "            {\r\n"
                + "              \"file\": \"tokenAuthentication.yaml\",\r\n"
                + "              \"name\": \"Authentication\",\r\n" + "              \"HttpVerb\": \"POST\"\r\n"
                + "            }]\r\n" + "        },\r\n" + "        {\r\n"
                + "          \"name\":\"Sorting and Pagination\",\r\n" + "          \"file\": \"paging.html\",\r\n"
                + "          \"staticContent\": true\r\n" + "        },\r\n" + "        {\r\n"
                + "          \"name\":\"Headers\",\r\n" + "          \"file\": \"headers.html\",\r\n"
                + "          \"staticContent\": true\r\n" + "        },\r\n" + "        {\r\n"
                + "          \"name\":\"Errors\",\r\n" + "          \"file\": \"error.html\",\r\n"
                + "          \"staticContent\": true\r\n" + "        },\r\n" + "        {\r\n"
                + "          \"name\":\"Versioning\",\r\n" + "          \"file\": \"versioning.html\",\r\n"
                + "          \"staticContent\": true\r\n" + "        }\r\n" + "      ]\r\n" + "    },\r\n" + "    {\r\n"
                + "      \"moduleName\": \"[PWC_SERVICE_ID]\",\r\n" + "      \"expanded\": true,\r\n"
                + "      \"apis\": [";
    }

    public static String getDefaultRequest() {
        return "# ---- TokenAuthentication definition\r\n" + "TokenAuthentication:\r\n" + "  type: object\r\n"
                + "  properties:\r\n" + "    providerLogin:\r\n" + "      type: string\r\n"
                + "      example: 'firstUser'\r\n" + "    providerPassword:\r\n" + "      type: string\r\n"
                + "      example: '$2a$10$AnRf8HJwhDOgvM/7PqXkNOnbyebzUPJFiFvN8wLDoLkKaYAK0dS1e'\r\n"
                + "    userLanguage:\r\n" + "      type: string\r\n" + "      example: 'en_US'\r\n"
                + "    requestInfo:\r\n" + "      type: object\r\n" + "      $ref: 'aggregate.yaml#/RequestInfo'\r\n"
                + "\r\n";
    }

    public static String getDefaultResponse() {
        return "# ---- TokenAuthenticationResponse response definition\r\n" + "TokenAuthenticationResponse:\r\n"
                + "  type: object\r\n" + "  properties:\r\n" + "    token:\r\n" + "      type: string\r\n"
                + "      description: <p>The access JWT Token </p>\r\n" + "    responseInfo:\r\n"
                + "      type: object\r\n" + "      $ref: 'aggregate.yaml#/ResponseInfo'\r\n" + "\r\n";
    }

    public static String getDefaultAggregate() {
        return "# ---- KeyValueV35\r\n" + "KeyValueV35:\r\n" + "  type: object\r\n" + "  properties:\r\n"
                + "    key:\r\n" + "      type: string\r\n" + "      example: ''\r\n" + "    data:\r\n"
                + "      type: string\r\n" + "      example: ''\r\n" + "    type:\r\n" + "      type: string\r\n"
                + "      example: ''\r\n" + "ResponseInfo:\r\n" + "  required:\r\n" + "    - responseUID\r\n"
                + "    - resultID\r\n" + "    - errorCode\r\n" + "  type: object\r\n" + "  properties:\r\n"
                + "    responseUID:\r\n" + "      type: string\r\n" + "      example: ''\r\n"
                + "      description: <p>Response Identifier. Echoed back by PowerCARD to the requester. It should contain the same value as the one sent in the request message.\r\n"
                + "        </p>\r\n" + "    resultID:\r\n" + "      type: string\r\n" + "      enum:\r\n"
                + "        - ProceedWithSuccess\r\n" + "        - ProceedWithSuccessMC\r\n" + "        - Error\r\n"
                + "        - SystemError\r\n"
                + "      description: <p>Refers to the result of processing in PowerCARD.\r\n"
                + "        The possible values are:<b>\r\n"
                + "        ProceedWithSuccess:<b> Call was ended successfully.\r\n"
                + "        ProceedWithSuccessMC:<b> Call made successfully, however the request will be inserted in a Maker Checker queue for further analysis.\r\n"
                + "        Error:<b> An error has occurred during the processing.\r\n"
                + "        SystemError:<b> An unknown system error occurred during the process. </p>\r\n"
                + "    errorCode:\r\n" + "      type: string\r\n" + "      example: ''\r\n"
                + "      description: <p>Indicate the error code used by the PowerCARD to tell you that program was experiencing a particular problem during the processing of the request.</p>\r\n"
                + "    errorDescription:\r\n" + "      type: string\r\n" + "      example: ''\r\n"
                + "      description: <p>If present, this field must contain the description of the error encountered</p>\r\n"
                + "RequestInfo:\r\n" + "  required:\r\n" + "    - requestUID\r\n" + "    - requestDate\r\n"
                + "  type: object\r\n" + "  properties:\r\n" + "    requestUID:\r\n" + "      type: string\r\n"
                + "      example: 'firstUser01'\r\n"
                + "      description: <p>Request Identifier. It is sent by the calling system as a universally unique identifier for the message. Used to match response with request messages.\r\n"
                + "        The generation mask should be the following:<b>\r\n"
                + "        XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX\r\n" + "        Example:<b>\r\n"
                + "        6948DF80-14BD-4E04-8842-7668D9C001F5\r\n" + "        </p>\r\n" + "    requestDate:\r\n"
                + "      type: string\r\n" + "      format: date-time\r\n"
                + "      description: <p>Date when the request was submitted</p>\r\n" + "    userID:\r\n"
                + "      type: string\r\n" + "      example: 'firstUser'\r\n"
                + "      description: <p>Represents the user Id who made the request.</p>\r\n";
    }

    public static String getDefaultHeader() {
        return "openapi: \"3.0.3\"\n" + "info:\n" + "  title: [API_MODULE] API documentation\n" + "  description: |\n"
                + "          [PWC_SERVICE_OVERVIEW]\n" + "  version: [PWC_RELEASE]\n" + "servers:\n"
                + "  - url: ${sandbox.backend.connectApiUrl}/rest\n" + "    description: Development server\n"
                + "paths:\n" + "  # --- /[API_NAME]/[API_VERSION]\n" + "  /[API_NAME]/[API_VERSION]:\n"
                + "    [API_HTTP_METHOD_lower]:\n" + "      description: |\r\n" + "        <h3>API Overview</h3>\r\n"
                + "          <p>[API_OVERVIEW]</p>\r\n" + "        \r\n" + "        <h3>Functional Description</h3>\r\n"
                + "        <img src=\"./docs/[PWC_SERVICE_ID]/[API_NAME].png\" />\r\n" + "      summary: |\r\n"
                + "          <p>[API_OVERVIEW]</p>\r\n" + "        \r\n" + "      tags:\n" + "        - [API_PRINTED_NAME]\n"
                + "      requestBody:\n" + "        required: true\n" + "        content:\n"
                + "          application/json:\n" + "            schema:\n"
                + "              $ref: '#/components/schemas/[API_NAME]V35Rq'\n" + "          application/xml:\n"
                + "            schema:\n" + "              $ref: '#/components/schemas/[API_NAME]V35Rq'\n"
                + "      responses:\n" + "        200:\n" + "          description: |\n" + "            Successfull\n"
                + "            Business Error Codes:\n" + "            <table>\n" + "[API_ERROR_LIST]"
                + "            </table>\n" + "          content:\n" + "            application/json:\n"
                + "              schema:\n" + "                  $ref: '#/components/schemas/[API_NAME]V35Rs'\n"
                + "            application/xml:\n" + "              schema:\n"
                + "                  $ref: '#/components/schemas/[API_NAME]V35Rs'\n" + "components:\n"
                + "  securitySchemes:\n" + "    bearerAuth:\n" + "      type: http\n" + "      scheme: bearer\n"
                + "      bearerFormat: JWT\n" + "      description: |\n" + "        <div>\n"
                + "          <h5>Api key authorization</h5>\n"
                + "          <p>JWT authorization header using Bearer scheme. Example: \"Authorization: Bearer {token}\"</p>\n"
                + "          <table>\n" + "            <tr><td>Name:</td><td>Authorization</td></tr>\n"
                + "            <tr><td>In:</td><td>Header</td></tr>\n" + "          </table>\n" + "        </div>\n"
                + "  schemas:\n" + "    # --- Import [API_NAME] request and response\n" + "    [API_NAME]V35Rq:\n"
                + "      $ref: 'request.yaml#/[API_NAME]V35Rq'\n" + "    [API_NAME]V35Rs:\n"
                + "      $ref: 'response.yaml#/[API_NAME]V35Rs'\n" + "security:\n" + "  - bearerAuth: []";
    }

    public static void setApiIdentification(Row parentRow) {
        String titleInfo = parentRow.getCell(1).getStringCellValue();
        switch (titleInfo.toUpperCase()) {
            case "PWC RELEASE":
                pwcRelease = parentRow.getCell(2).getStringCellValue().trim();
                break;
            case "PWC MODULE":
                pwcModule = parentRow.getCell(2).getStringCellValue().trim();
                break;
            case "PWC SERVICE":
                pwcService = parentRow.getCell(2).getStringCellValue().trim();
                break;
            case "PWC SERVICE OVERVIEW":
                pwcServiceOverview = parentRow.getCell(2).getStringCellValue().trim();
                break;
            case "PWC SERVICE ID":
                if(!StringUtils.equalsIgnoreCase(pwcServiceId,parentRow.getCell(2).getStringCellValue().trim()) && parentRow.getCell(2).getStringCellValue()!=null) {
                    pwcServiceId = parentRow.getCell(2).getStringCellValue().trim();
                    apiCodeGenPath = apiCodeGenPath.replace("[PWC_SERVICE_ID]", pwcServiceId);
                    yamlOutputPath = yamlOutputPath.replace("[PWC_SERVICE_ID]", pwcServiceId);
                    pptOutputPath = pptOutputPath.replace("[PWC_SERVICE_ID]", pwcServiceId);
                    imgOutputPath = imgOutputPath.replace("[PWC_SERVICE_ID]", pwcServiceId);
                    System.out.println("######OutPutPath:");
                    System.out.println("########DB/BtDesign:" + apiCodeGenPath);
                    System.out.println("########YAML	   :" + yamlOutputPath);
                    System.out.println("########PPT        :" + pptOutputPath);
                    System.out.println("########IMG        :" + imgOutputPath);
                    try {
                        Files.createDirectories(Paths.get(yamlOutputPath));
                        Files.createDirectories(Paths.get(pptOutputPath));
                        Files.createDirectories(Paths.get(imgOutputPath));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case "API NAME":
                if(!StringUtils.equalsIgnoreCase(apiName,parentRow.getCell(2).getStringCellValue().trim()) && parentRow.getCell(2).getStringCellValue()!=null) {
                    apiName = parentRow.getCell(2).getStringCellValue().trim();
                    System.out.println("##################################################");
                    System.out.println("###GenerateYaml for API:" + apiName);
                }
                break;
            case "API HTTP METHOD":
                apiHttpMethod = getCellValue(parentRow,2);
                break;
            case "API VERSION":
                apiVersion = getCellValue(parentRow,2);
                break;
            case "API OVERVIEW":
            case "OVERVIEW":
                apiOverview = getCellValue(parentRow,2);
                break;
            default:
                break;
        }
    }

    public static String getJsonSampleData(String apiName, String apiVersion, String parentName, FieldNode fieldNode, Map<String, String> fieldToFullPath) {
        try {
            // Load JSON file
            String jsonFilePath = jsonSampleDataPath + "/" + pwcServiceId + "/" + apiName + "_" + apiVersion + ".json";
            File jsonFile = new File(jsonFilePath);
            if (!jsonFile.exists()) {
                Files.createDirectories(Paths.get(jsonSampleDataPath + "/" + pwcServiceId));
                jsonFile.createNewFile();
                return "";
            } else {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(jsonFile);
                if(parentName!=null) {
                    JsonNode parentNode = root.get(parentName);
                    if(parentNode==null) {
                        parentNode=root.get(parentName+"List");
                    }
                    if(parentNode==null) {
                        parentNode=root.get(parentName+"sList");
                    }
                    if(parentNode!=null) {
                        return findFieldUnderParent(parentNode, parentName, fieldNode.fieldName);
                    }
                }
                return findFieldUnderParent(root, parentName, fieldNode.fieldName);

            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }


    public static String findFieldUnderParent(JsonNode node, String parentKey, String fieldKey) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String currentKey = entry.getKey();
                JsonNode value = entry.getValue();
                JsonNode targetFieldValue = null;

                if (currentKey.startsWith(fieldKey) && value.isArray()) {
                    return findFieldUnderParent(value, parentKey, fieldKey);
                } else {
                    if(parentKey!=null && currentKey.startsWith(parentKey)) {
                        if(value.isObject()) {
                            targetFieldValue = value.get(fieldKey);
                            if (targetFieldValue == null) {
                                targetFieldValue= value.get(fieldKey.toLowerCase());
                            }
                            if (targetFieldValue != null) {
                                return StringUtils.isEmpty(targetFieldValue.asText())?"":targetFieldValue.asText();
                            }
                        }else if(value.isArray()) {
                            return findFieldUnderParent(value, parentKey, fieldKey);
                        }

                    }else if ((!currentKey.equals(fieldKey)) && value.isObject()) {
                        if (parentKey != null ) {
                            JsonNode targetParentValue = value.get(parentKey);
                            if(targetParentValue == null) {
                                targetParentValue = value.get(parentKey+"List");
                            }
                            if(targetParentValue == null) {
                                targetParentValue = value.get(parentKey+"sList");
                            }
                            if(targetParentValue!=null) {
                                if(targetParentValue.isObject()) {
                                    targetFieldValue = targetParentValue.get(fieldKey);
                                    if (targetFieldValue == null) {
                                        targetFieldValue= targetParentValue.get(fieldKey.toLowerCase());
                                    }
                                    if (targetFieldValue != null)
                                        return StringUtils.isEmpty(targetFieldValue.asText())?"":targetFieldValue.asText();
                                }else if(targetParentValue.isArray()) {
                                    return findFieldUnderParent(targetParentValue, parentKey, fieldKey);
                                }

                            }
                        }
                    } else if (currentKey.equalsIgnoreCase(fieldKey) && !value.isObject()) {
                        return StringUtils.isEmpty(value.asText())?"":value.asText();
                    }else if(value.isObject()) {
                        return findFieldUnderParent(value, parentKey, fieldKey);
                    }else if (value.isArray()) {
                        return findFieldUnderParent(value, parentKey, fieldKey);
                    }
                }
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                return findFieldUnderParent(item, parentKey, fieldKey);
            }
        }
        return "";
    }

    public static boolean isApiEnabledToBeGenerated(String apiName) {
        if (apiRestrictredList != null) {
            String[] apiToGenerateList = apiRestrictredList.split(";");
            for (String api : apiToGenerateList) {
                if (api.equalsIgnoreCase(apiName)) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    public static void generatePPT(String apiName, String apiOverview) throws IOException {

        try (FileInputStream fis = new FileInputStream(new File(pptTemplatePath));
             XMLSlideShow ppt = new XMLSlideShow(fis)) {

            for (XSLFSlide slide : ppt.getSlides()) {
                XSLFGroupShape groupShape = (XSLFGroupShape) slide.getShapes().get(0);
                recursivePptReplacement(groupShape);
            }
            // Save the updated file
            try (FileOutputStream out = new FileOutputStream(pptOutputPath + "/" + apiName + ".pptx")) {
                ppt.write(out);
            }
            System.out.println("PPT saved to: " + pptOutputPath + "/" + apiName + ".pptx");

        }
        //generate screenshot used in yaml files
        generatePPTScreenShoot(pptOutputPath + "/" + apiName + ".pptx");
    }

    public static void recursivePptReplacement(XSLFGroupShape groupShape) {
        for (XSLFShape shape : groupShape.getShapes()) {
            if (shape instanceof XSLFGroupShape) {
                recursivePptReplacement((XSLFGroupShape) shape);
            } else if (shape instanceof XSLFAutoShape) {
                XSLFAutoShape autoShape = (XSLFAutoShape) shape;

                for (XSLFTextParagraph paragraph : autoShape.getTextParagraphs()) {
                    for (XSLFTextRun run : paragraph.getTextRuns()) {
                        String text = run.getRawText();
                        if (text != null) {
                            // Calcul de l'abréviation apiOverview
                            String apiAbrev = apiOverview;
                            if (apiOverview.contains("."))
                                apiAbrev = apiOverview.substring(0, apiOverview.indexOf("."));

                            // Remplacement des placeholders dans l'ordre et mise à jour du style
                            if (text.contains("[API_NAME]")) {
                                run.setText(text.replace("[API_NAME]", apiName));
                                text = run.getRawText(); // mise à jour du texte courant
                            }

                            if (text.contains("[API_OVERVIEW]")) {
                                run.setText(text.replace("[API_OVERVIEW]", apiAbrev));
                                text = run.getRawText();
                            }

                            if (text.contains("[API_RESOURCES]")) {
                                String replacedText = text.replace("[API_RESOURCES]", apiResources.replace(";", "\n"));
                                run.setText(replacedText);

                                // Mise à jour du style selon le contenu de apiResources
                                run.setFontSize(10.0);
                                run.setItalic(false);
                                run.setFontColor(Color.DARK_GRAY);




                            }
                        }
                    }
                }
            }
        }
    }


    public static void generatePPTScreenShoot(String pptxFile) {

        // Load the PPTX file
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(pptxFile);

            XMLSlideShow ppt = new XMLSlideShow(fis);
            fis.close();

            // Create output directory if it doesn't exist
            new File(imgOutputPath).mkdirs();

            Dimension pgsize = ppt.getPageSize();
            double scale = 3.0;  // Ajuste ici la qualité (2.0, 3.0, etc.)

            int width = (int) (pgsize.width * scale);
            int height = (int) (pgsize.height * scale);
            int slideNumber = 1;

            for (XSLFSlide slide : ppt.getSlides()) {
                BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = img.createGraphics();
                graphics.scale(scale, scale);

                // Optional: set rendering hints for better quality
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                graphics.setPaint(Color.WHITE);

                // Clear background
                //graphics.setPaint(Color.white);
                graphics.fill(new Rectangle(pgsize));

                // Render slide
                slide.draw(graphics);
                graphics.dispose();

                // Save image
                File imageFile = new File(imgOutputPath + "/" + apiName + ".png");
                ImageIO.write(img, "png", imageFile);
                System.out.println("PPT screenshot Saved: " + imageFile.getAbsolutePath());

                slideNumber++;
            }

            ppt.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static String getApiNameToPrint(String apiConcatenatedName,String serviceID) {
        StringBuffer buffer = new StringBuffer();
        Pattern p = Pattern.compile("([A-Z][a-z]*)");
        Matcher m = p.matcher(serviceID);
        while ( m.find() )
            buffer.append(m.group() + " ");
        String serviceIDSplited= buffer.toString().trim();

        buffer = new StringBuffer();
        m = p.matcher(apiConcatenatedName);
        while ( m.find() )
            buffer.append(m.group() + " ");
        return buffer.toString().trim().replace(serviceIDSplited, serviceID);
    }


    @SuppressWarnings("deprecation")
    public static String getCellValue(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_STRING:
                return cell.getStringCellValue().trim();
            case Cell.CELL_TYPE_NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case Cell.CELL_TYPE_BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case Cell.CELL_TYPE_FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        return "";
                    }
                }
            case Cell.CELL_TYPE_ERROR:
            case Cell.CELL_TYPE_BLANK:
            default:
                return "";
        }
    }


    public static FieldNode buildTree(String parentPath, Map<String, List<FieldNode>> map) {
        List<FieldNode> children = map.get(parentPath);
        if (children == null) return null;


        // Since root may be multiple fields, return a dummy root node
        FieldNode dummyRoot = new FieldNode(parentPath, "Complex","Root", "Required", "Root node");
        for (FieldNode child : children) {
            String childPath = buildPath(parentPath, child.getFieldName());
            if (isComplexType(child.getDataType())) {
                FieldNode subtree = buildTree(childPath, map);
                if (subtree != null) {
                    child.getChildren().addAll(subtree.getChildren());
                }
            }
            dummyRoot.addChild(child);
        }

        return dummyRoot;
    }

    public static String buildPath(String parentPath, String fieldName) {
        if (parentPath == null || parentPath.isEmpty() || parentPath.equals("-")) {
            return fieldName;
        } else {
            return parentPath + "." + fieldName;
        }
    }

    public static Map<String, String> buildFieldPaths(List<FieldNode> fields) {
        Map<String, String> fieldToParent = new HashMap<>();
        for (FieldNode f : fields) {
            fieldToParent.put(StringUtils.uncapitalize(f.fieldName), StringUtils.uncapitalize(f.parent));
        }

        Map<String, String> fieldToPath = new HashMap<>();
        for (FieldNode f : fields) {
            StringBuilder path = new StringBuilder(StringUtils.uncapitalize(f.fieldName));
            String parent = StringUtils.uncapitalize(f.parent);
            while (parent != null && !parent.equals("-")) {
                path.insert(0, parent + ".");
                parent = fieldToParent.get(parent);
            }
            fieldToPath.put(StringUtils.uncapitalize(f.fieldName), path.toString());
        }
        return fieldToPath;
    }



}
