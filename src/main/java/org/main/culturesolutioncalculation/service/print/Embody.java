package org.main.culturesolutioncalculation.service.print;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorker;
import com.itextpdf.tool.xml.XMLWorkerFontProvider;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.itextpdf.tool.xml.css.CssFile;
import com.itextpdf.tool.xml.css.StyleAttrCSSResolver;
import com.itextpdf.tool.xml.html.CssAppliers;
import com.itextpdf.tool.xml.html.CssAppliersImpl;
import com.itextpdf.tool.xml.html.Tags;
import com.itextpdf.tool.xml.parser.XMLParser;
import com.itextpdf.tool.xml.pipeline.css.CSSResolver;
import com.itextpdf.tool.xml.pipeline.css.CssResolverPipeline;
import com.itextpdf.tool.xml.pipeline.end.PdfWriterPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipelineContext;
import com.opencsv.CSVReader;
import org.main.culturesolutioncalculation.domain.Users;
import org.main.culturesolutioncalculation.model.CropNutrientStandard;
import org.main.culturesolutioncalculation.model.NutrientSolution;
import org.main.culturesolutioncalculation.service.CSVDataReader;
import org.main.culturesolutioncalculation.service.calculator.FinalCal;
import org.main.culturesolutioncalculation.service.database.DatabaseConnector;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Embody implements Print{

    private DatabaseConnector conn;
    private CSVDataReader csvDataReader;
    private String pdfName;
    private Map<String, FinalCal> MacroMolecularMass = new LinkedHashMap<>();
    private Map<String, FinalCal> MicroMolecularMass = new LinkedHashMap<>();
    private Map<String, Map<String, Double>> MacroCompoundsRatio = new LinkedHashMap<>(); // ex; {NH4NO3 , {NH4N=1.0, NO3N=1.0}}
    private Map<String, Map<String, Double>> MicroCompoundsRatio = new LinkedHashMap<>(); // ex; {NH4NO3 , {NH4N=1.0, NO3N=1.0}}
    /*
    �м� ��Ͽ� ���� �� ������ :
    ����� �̸�, �м� ��¥, ��� �۹�, ���� ����(�״�����, �߸���Ű:�̰� ����Ʈ���� �޾ƿ����)
     */
    private Users users;

    public void setUsers(Users users) {
        this.users = users;
    }

    @Override
    public void setMacroMolecularMass() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime requestDate = LocalDateTime.parse("2024-03-25 16:48:44", formatter);

        // DateTimeFormatter�� ����Ͽ� LocalDateTime�� ������ ���ڿ��� ��ȯ
        String formattedDate = requestDate.format(formatter);

        String query = "SELECT um.* FROM users_macro_calculatedMass um " +
                "JOIN users u ON um.user_id = u.id " +
                "WHERE u.id = ? AND u.request_date = ?";

        try (Connection connection = conn.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(query)) {

            // �Ķ���� ���ε�
            pstmt.setInt(1, users.getId());
            pstmt.setString(2, formattedDate);

            try (ResultSet resultSet = pstmt.executeQuery()) {
                // ��� ó��
                while (resultSet.next()) {
                    String macro = resultSet.getString("macro"); //����Į��4����, ����Į��, ����ϸ� ���
                    String solution = resultSet.getString("solution"); //��� Ÿ�� (A,B, C)
                    double mass = resultSet.getDouble("mass");//ȭ�չ� ����

                    MacroMolecularMass.put(macro, new FinalCal(solution, mass)); //100��� ����� ���� ȭ�չ��� �� ���� ����
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setMicroMolecularMass() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime requestDate = LocalDateTime.parse("2024-03-25 16:48:44", formatter);

        // DateTimeFormatter�� ����Ͽ� LocalDateTime�� ������ ���ڿ��� ��ȯ
        String formattedDate = requestDate.format(formatter);

        String query = "SELECT um.* FROM users_micro_calculatedMass um " +
                "JOIN users u ON um.user_id = u.id " +
                "WHERE u.id = ? AND u.request_date = ?";

        try (Connection connection = conn.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(query)) {

            // �Ķ���� ���ε�
            pstmt.setInt(1, users.getId());
            pstmt.setString(2, formattedDate);

            try (ResultSet resultSet = pstmt.executeQuery()) {
                // ��� ó��
                while (resultSet.next()) {
                    String micro = resultSet.getString("micro"); //����Į��4����, ����Į��, ����ϸ� ���
                    String solution = resultSet.getString("solution"); //��� Ÿ�� (A,B, C)
                    double mass = resultSet.getDouble("mass");//ȭ�չ� ����

                    MicroMolecularMass.put(micro, new FinalCal(solution, mass)); //100��� ����� ���� ȭ�չ��� �� ���� ����
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void setMacroFertilization(){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime requestDate = LocalDateTime.parse("2024-03-25 16:48:44", formatter);

        // DateTimeFormatter�� ����Ͽ� LocalDateTime�� ������ ���ڿ��� ��ȯ
        String formattedDate = requestDate.format(formatter);

        String query = "select um.* from users_macro_fertilization um " +
                "join users u on u.id = um.users_id " +
                "where u.request_date = ? and u.id = ?";

        try(Connection connection = conn.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(query)){

            pstmt.setString(1, formattedDate);
            pstmt.setInt(2, users.getId());

            try(ResultSet resultSet = pstmt.executeQuery()){
                while(resultSet.next()){
                    String macro = resultSet.getString("macro");;
                    Map<String, Double> compoundRatio = new LinkedHashMap<>();
                    String subQuery = "select * from ";//users_macro_fertilization���� ���� �ִ� ������ ��� ���
                }
            }

        }catch (SQLException e){

        }


    }

    @Override
    public String getUserInfo() {
        return
                "<p>�Ƿ��� ����: "+users.getName()+"</p>" +
                        "<p>�Ƿ� �Ͻ�: "+users.getRequestDate()+"</p>" +
                        "<p>��� �۹�: "+users.getCropName()+"</p>" +
                        "<p>���� ����: "+users.getMediumType()+"</p>" +
                        "<hr>";
    }
    public void setPdfName() {
        this.pdfName = users.getName()+"_"+users.getRequestDate()+"_"+users.getCropName()+".pdf";
    }

    public String getPdfName() {
        return pdfName;
    }
    public void setUp(){
        setMacroFertilization();
        //setMicroFertilizaton();
        setMacroMolecularMass();
        setMicroMolecularMass();
        setPdfName();
    }

    @Override
    public void getPDF() {

        setUp();

        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        try{
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(getPdfName()));
            writer.setInitialLeading(12.5f);

            document.open();
            XMLWorkerHelper helper = XMLWorkerHelper.getInstance();

            CSSResolver cssResolver = new StyleAttrCSSResolver();
            CssFile cssFile = null;
            try{
                cssFile = helper.getCSS(new FileInputStream("pdf.css"));
            }catch (FileNotFoundException e){
                e.printStackTrace();
            }
            cssResolver.addCss(cssFile);

            //HTML�� ��Ʈ �غ�
            XMLWorkerFontProvider fontProvider = new XMLWorkerFontProvider(XMLWorkerFontProvider.DONTLOOKFORFONTS);
            fontProvider.register("css/MALGUN.ttf","MalgunGothic");
            CssAppliers cssAppliers = new CssAppliersImpl(fontProvider);

            HtmlPipelineContext htmlContext = new HtmlPipelineContext(cssAppliers);
            htmlContext.setTagFactory(Tags.getHtmlTagProcessorFactory());

            //Pipelines
            PdfWriterPipeline pdf = new PdfWriterPipeline(document, writer);
            HtmlPipeline html = new HtmlPipeline(htmlContext, pdf);
            CssResolverPipeline css = new CssResolverPipeline(cssResolver, html);

            XMLWorker worker = new XMLWorker(css, true);
            XMLParser xmlParser = new XMLParser(worker, Charset.forName("UTF-8"));

            String htmlStr = getAllHtmlStr();

            StringReader stringReader = new StringReader(htmlStr);
            xmlParser.parse(stringReader);
            document.close();
            writer.close();

        }catch (DocumentException e){
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getAllHtmlStr() {

        String htmlStr = "<html><head><body style='font-family: MalgunGothic;'>";

        htmlStr += getUserInfo();
        htmlStr += getTable(htmlStr);

        return htmlStr;
    }

    private String getTable(String htmlStr) {
        htmlStr += "<table>" + "<tr>";

        CropNutrientStandard cropNutrients = getCropNutrients();
        htmlStr += getMacro(cropNutrients);
        //htmlStr += getMicro();

        htmlStr += getSolution("A");
        htmlStr += getSolution("B");
        htmlStr += getSolution("C");

        return htmlStr;
    }

    private CropNutrientStandard getCropNutrients (){ //�ش� ���� ������ �ش��ϴ� ��� �۹��� ���� ���ط� ����

        csvDataReader = new CSVDataReader();
        NutrientSolution nutrientSolution = csvDataReader.readFile(users.getMediumType()); //�״�����, �߸���Ű ��
        ArrayList<CropNutrientStandard> cropList = nutrientSolution.getCropList();
        Optional<CropNutrientStandard> cropNutrients = cropList.stream().filter(c -> c.getCropName().equals(users.getCropName()))
                .findFirst();

        return cropNutrients.get();
    }


    private String getMacro(CropNutrientStandard cropNutrientStandard){



        String Html =
                "<th class=\"category\">�ٷ�����</th>" +
                        "<th colspan=\"2\">Ca</th>" +
                        "<th colspan=\"2\">NO3N</th>" +
                        "<th colspan=\"2\">NH4N</th>" +
                        "<th colspan=\"2\">K</th>" +
                        "<th colspan=\"2\">H2PO4</th>" +
                        "<th colspan=\"2\">SO4</th>" +
                        "<th colspan=\"2\">Mg</th>" +
                        "</tr>";


        return Html;
    }
    private String getSolution(String solution) {
        String unit = "Kg";
        String Html =
                "<th class=\"category\">"+solution+"��</th>" +
                        "<th colspan=\"2\">100��� ����</th>" +
                        "</tr>";

        for (String macro : MacroMolecularMass.keySet()) {
            if(MacroMolecularMass.get(macro).getSolution().equals(solution)){
                Html += "<td class=\"name\">"+macro+"</td>" +
                        "<td>"+String.format("%.2f",MacroMolecularMass.get(macro).getMass())+"</td>" +
                        "<td class=\"unit\">"+unit+"</td>" +
                        "</tr>";
            }
        }
        for (String micro : MicroMolecularMass.keySet()) {
            if(MicroMolecularMass.get(micro).getSolution().equals(solution)){
                Html += "<td class=\"name\">"+micro+"</td>" +
                        "<td>"+String.format("%.2f",MicroMolecularMass.get(micro).getMass())+"</td>" +
                        "<td class=\"unit\">"+unit+"</td>" +
                        "</tr>";
            }
        }

        return Html;

    }
}
