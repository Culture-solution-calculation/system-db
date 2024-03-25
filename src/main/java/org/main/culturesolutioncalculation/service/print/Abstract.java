package org.main.culturesolutioncalculation.service.print;

import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorker;
import com.itextpdf.tool.xml.XMLWorkerFontProvider;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.itextpdf.tool.xml.html.CssAppliers;
import com.itextpdf.tool.xml.html.CssAppliersImpl;
import com.itextpdf.tool.xml.html.Tags;
import com.itextpdf.tool.xml.parser.XMLParser;
import com.itextpdf.tool.xml.pipeline.css.CSSResolver;
import com.itextpdf.tool.xml.pipeline.css.CssResolverPipeline;
import com.itextpdf.tool.xml.pipeline.end.PdfWriterPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipelineContext;
import org.main.culturesolutioncalculation.service.calculator.FinalCal;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import com.itextpdf.text.*;
import com.itextpdf.tool.xml.css.CssFile;
import com.itextpdf.tool.xml.css.StyleAttrCSSResolver;
import org.main.culturesolutioncalculation.service.database.DatabaseConnector;

public class Abstract {

    private DatabaseConnector conn;
    private Map<String, FinalCal> MacroMolecularMass = new LinkedHashMap<>();
    private Map<String, FinalCal> MicroMolecularMass = new LinkedHashMap<>();
    /*
    분석 기록에 들어가야 할 정보들 :
    사용자 이름, 분석 날짜, 재배 작물, 배양액 종류(네덜란드, 야마자키:이건 프론트에서 받아오기로)
     */
    private String userName;
    private int userId;
    private LocalDateTime requestDate;
    private String cropName;

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public void setCropName(String cropName) {
        this.cropName = cropName;
    }

    //데이터베이스에서 꺼내와야함
    public void setMacroMolecularMass() {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime requestDate = LocalDateTime.parse("2024-03-25 16:48:44", formatter);

        // DateTimeFormatter를 사용하여 LocalDateTime을 적절한 문자열로 변환
        String formattedDate = requestDate.format(formatter);

        String query = "SELECT um.* FROM users_macro_calculatedMass um " +
                "JOIN users u ON um.user_id = u.id " +
                "WHERE u.id = ? AND u.request_date = ?";

        try (Connection connection = conn.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(query)) {

            // 파라미터 바인딩
            pstmt.setInt(1, userId);
            pstmt.setString(2, formattedDate);

            try (ResultSet resultSet = pstmt.executeQuery()) {
                // 결과 처리
                while (resultSet.next()) {
                    String macro = resultSet.getString("macro"); //질산칼슘4수염, 질산칼륨, 질산암모늄 등등
                    String solution = resultSet.getString("solution"); //양액 타입 (A,B, C)
                    double mass = resultSet.getDouble("mass");//화합물 질량

                    MacroMolecularMass.put(macro, new FinalCal(solution, mass)); //100배액 계산을 위해 화합물과 그 질량 저장
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public void setMicroMolecularMass() {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime requestDate = LocalDateTime.parse("2024-03-25 16:48:44", formatter);

        // DateTimeFormatter를 사용하여 LocalDateTime을 적절한 문자열로 변환
        String formattedDate = requestDate.format(formatter);

        String query = "SELECT um.* FROM users_micro_calculatedMass um " +
                "JOIN users u ON um.user_id = u.id " +
                "WHERE u.id = ? AND u.request_date = ?";

        try (Connection connection = conn.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(query)) {

            // 파라미터 바인딩
            pstmt.setInt(1, userId);
            pstmt.setString(2, formattedDate);

            try (ResultSet resultSet = pstmt.executeQuery()) {
                // 결과 처리
                while (resultSet.next()) {
                    String micro = resultSet.getString("micro"); //질산칼슘4수염, 질산칼륨, 질산암모늄 등등
                    String solution = resultSet.getString("solution"); //양액 타입 (A,B, C)
                    double mass = resultSet.getDouble("mass");//화합물 질량

                    MicroMolecularMass.put(micro, new FinalCal(solution, mass)); //100배액 계산을 위해 화합물과 그 질량 저장
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    private String pdfName;

    public void setPdfName() {
        this.pdfName = userName+"_"+requestDate+"_"+cropName+".pdf";
    }

    public String getPdfName() {
        return pdfName;
    }
    public void setUp(){
        setMacroMolecularMass();
        setMicroMolecularMass();
        setPdfName();
    }

    public void getPDF(){

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

            //HTML과 폰트 준비
            XMLWorkerFontProvider fontProvider = new XMLWorkerFontProvider(XMLWorkerFontProvider.DONTLOOKFORFONTS);
            fontProvider.register("MALGUN.ttf","MalgunGothic");
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

    private String getAllHtmlStr() {
        String htmlStr = "<html><head><body style='font-family: MalgunGothic;'>"+
                "<table>"+
                "<tr>";

        htmlStr += getSolution("A");
        htmlStr += getSolution("B");
        htmlStr += getSolution("C");


        return htmlStr;
    }

    private String getSolution(String solution) {
        String unit = "Kg";
        String Html =
                "<th class=\"category\">"+solution+"액</th>" +
                "<th colspan=\"2\">100배액 기준</th>" +
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
