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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
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
    private String userId;
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
    public void setMacroMolecularMass(String userId, String userName, LocalDateTime requestDate) {
        String query = "select * from "





    }

    public void setMicroMolecularMass(Map<String, FinalCal> microMolecularMass) {
        MicroMolecularMass = microMolecularMass;
    }

    private String pdfName;

    public void setPdfName() {
        this.pdfName = userName+"_"+requestDate+"_"+cropName;
    }

    public String getPdfName() {
        return pdfName;
    }

    public void getPDF(){
        setMacroMolecularMass(userId, userName, requestDate);
        //setMicroMolecularMass(userId, userName, requestDate);
        setPdfName();
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
        String AHtml =
                "<th class=\"category\">A액</th>" +
                "<th colspan=\"2\">100배액 기준</th>" +
                "</tr>";

        for (String macro : MacroMolecularMass.keySet()) {
            if(MacroMolecularMass.get(macro).getSolution().equals(solution)){
                AHtml += "<td class=\"name\">"+macro+"</td>" +
                        "<td>"+String.format("%.2f",solution.getHundredfoldDilutionStandard())+"</td>" +
                        "<td class=\"unit\">"+solution.getHundredfoldUnit()+"</td>" +
                        "</tr>";

            }
        }

    }


}
