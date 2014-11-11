/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tr.edu.metu.ncc.cng.gtsearch.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import tr.edu.metu.ncc.cng.gtsearch.core.MultilingualSearchManager;
import tr.edu.metu.ncc.cng.gtsearch.xmlparser.ks.MohseContext;
import tr.edu.metu.ncc.cng.gtsearch.xmlparser.solr.SolrDocument;

/**
 *
 * @author ugur
 */
@WebServlet(name = "EnrichServlet", urlPatterns = {"/EnrichServlet"})
public class EnrichServlet extends HttpServlet {

    public final static String BROADER = "http://www.w3.org/2004/02/skos/core%23broader";
    public final static String RELATED = "http://www.w3.org/2004/02/skos/core%23related";
    public final static String NARROVER = "http://www.w3.org/2004/02/skos/core%23narrower";

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, Exception {
        
        long first,last;
        Date date = new Date();
        
        first = date.getTime();
        
        response.setContentType("text/html;charset=UTF-8");

        ArrayList<String> replaced = new ArrayList<String>();

        String pageURL = request.getParameter("html");
        String termString = request.getParameter("term");

        System.out.println("EnrichServlet execute.");

        PrintWriter out = response.getWriter();

        // get keywords 
        MultilingualSearchManager msm = new MultilingualSearchManager();

        ArrayList<MohseContext> documentsEnLabel = null;
        ArrayList<MohseContext> documentsTrLabel = null;

        documentsEnLabel = msm.getAllLabels("http://localhost:8080/ks/KnowledgeService", "http://localhost:8080/rdf/" + termString + ".rdf", "en");
        documentsTrLabel = msm.getAllLabels("http://localhost:8080/ks/KnowledgeService", "http://localhost:8080/rdf/" + termString + ".rdf", "tr");

        org.jsoup.nodes.Document doc = Jsoup.parse(new URL(pageURL), 1000);

        System.out.println("document textt");
        System.out.println(doc.text());

        // add utf-8 charset to meta tag
        Elements metas = doc.getElementsByTag("meta");

        // meta deneme
        // Element meta = metas.first();
        // meta.attr("charset", "utf-8");
        // take p tag with jsoup
        Elements p_elements = doc.getElementsByTag("p");

        for (Element element : p_elements) {

            HashMap<String, String> replaceMap = new HashMap<String, String>();

            for (MohseContext context : documentsEnLabel) {

                if (element.text().contains(" " + context.getTerm() + " ") && !isReplaced(replaced, context.getTerm())) {
                    ArrayList<SolrDocument> exactMaches;
                    ArrayList<SolrDocument> broaderMaches;
                    ArrayList<SolrDocument> relatedMaches;
                    ArrayList<SolrDocument> narrowerMaches;
                    
                    List<SolrDocument> bM;
                    List<SolrDocument> rM;
                    List<SolrDocument> nM;

                    exactMaches = msm.getQueryResults("http://localhost:8080/ks/KnowledgeService", "http://localhost:8080/rdf/" + termString + ".rdf", context.getTerm());

                    if (!exactMaches.isEmpty()) {
                        // String add = createTooltip(context.getTerm(), exactMaches);
                        broaderMaches = msm.getRelations("http://localhost:8080/ks/KnowledgeService", "http://localhost:8080/rdf/" + termString + ".rdf", context.getConcept(), BROADER, "tr");
                        relatedMaches = msm.getRelations("http://localhost:8080/ks/KnowledgeService", "http://localhost:8080/rdf/" + termString + ".rdf", context.getConcept(), RELATED, "tr");
                        narrowerMaches = msm.getRelations("http://localhost:8080/ks/KnowledgeService", "http://localhost:8080/rdf/" + termString + ".rdf", context.getConcept(), NARROVER, "tr");

                        if ( context.getTerm().equals("economy")) {
                            System.out.println("eco");
                        }
                        
                        if ( relatedMaches.size() > 5 ) {
                            rM = relatedMaches.subList(0, 4);
                        }
                        else {
                            rM = relatedMaches.subList(0, relatedMaches.size());
                        }
                        
                        if ( broaderMaches.size() > 5 ) {
                            bM = broaderMaches.subList(0, 4);
                        }
                        else {
                            bM = broaderMaches.subList(0, broaderMaches.size());
                        }
                        
                        if ( narrowerMaches.size() > 5 ) {
                            nM = narrowerMaches.subList(0, 4);
                        }
                        else {
                            nM = narrowerMaches.subList(0, narrowerMaches.size());
                        }
                        
//                        String add = createTooltipWithRelationsSpanEncoded(context.getTerm(), exactMaches, 
//                                relatedMaches, 
//                                broaderMaches, 
//                                narrowerMaches);
                        
                        String add = createTooltipWithRelationsSpanEncoded(context.getTerm(), exactMaches, 
                                rM, 
                                bM, 
                                nM);

                        String debug = decodeHtmlCharacter(add);

                        replaceMap.put(" " + context.getTerm() + " ", add);

                        // System.out.println("terms " + context.getTerm());
                        replaced.add(context.getTerm());
                    }

                }
            }

            for (String key : replaceMap.keySet()) {
                // innerHtml = innerHtml.replaceFirst(key, replaceMap.get(key));
                element.text(element.text().replaceFirst(key, replaceMap.get(key)));
            }

        }

        try {

            String doc_str = decodeHtmlCharacter(doc.select("body").first().toString());

            out.println(doc_str);
        } finally {
            out.close();
            date = new Date();
            System.out.println("mili passed " + (date.getTime() - first) );
        }

    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception ex) {
            Logger.getLogger(EnrichServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception ex) {
            Logger.getLogger(EnrichServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

    private String decodeHtmlCharacter(String encoded) {

        encoded = encoded.replaceAll("_small_", "<");
        encoded = encoded.replaceAll("_big_", ">");
        encoded = encoded.replaceAll("_dash_", "\"");

        return encoded;
    }

    private String createAdded(String term) throws Exception {
        return "<div class=\"editor-field\">"
                + "<span class=\"help\">" + term + "</span>"
                + "<div style=\"display: none\" class=\"tooltip\">"
                + "	<div class=\"close\"><a href=\"#\">close</a></div><br class=\"clear\">"
                + "	<div>" + createReplaceStr(term) + "</div>"
                + "</div> "
                + "</div>";
    }

    private String createAdded(String term, String termList) throws Exception {
        return "<div class=\"editor-field\">"
                + "<span class=\"help\">" + term + "</span>"
                + "<div style=\"display: none\" class=\"tooltip\">"
                + "	<div class=\"close\"><a href=\"#\">close</a></div><br class=\"clear\">"
                + "	<div>" + createReplaceStr(term, termList) + "</div>"
                + "</div> "
                + "</div>";
    }

    private String newCreateAdded(String term) throws Exception {
        return "<div class=\"help\" onclick=\"showTooltip(" + term + ", 'Simple Tooltip', ' " + createReplaceStr(term) + " ')\"> " + term + " </div>"
                + "<div style=\"display:none\" id=\"" + term + "\"></div> ";
    }

    private String newCreateAdded(String term, String termList) throws Exception {
        return "<div class=\"help\" onclick=\"showTooltip(" + term + ", 'Simple Tooltip', ' " + createReplaceStr(term, termList) + " ')\"> " + term + " </div>"
                + "<div style=\"display:none\" id=\"" + term + "\"></div> ";
    }

    private String createTooltip(String term, ArrayList<SolrDocument> documents) throws Exception {
        return "<div class=\"help\" onclick=\"showTooltip(" + term + ", 'Simple Tooltip', ' " + createReplaceStr(documents) + " ')\"> " + term + " </div>"
                + "<div style=\"display:none\" id=\"" + term + "\"></div> ";
    }

    private String createTooltipWithRelationsDiv(String term, ArrayList<SolrDocument> exact, ArrayList<SolrDocument> related, ArrayList<SolrDocument> broader, ArrayList<SolrDocument> narrover) throws Exception {
        return "<div class=\"help\" onclick=\"showTooltip(" + term + ", 'Simple Tooltip', ' " + createReplaceStrWithRelations(exact, related, broader, narrover) + " ')\"> " + term + " </div>"
                + "<div style=\"display:none\" id=\"" + term + "\"></div> ";
    }

    private String createTooltipWithRelationsSpan(String term, ArrayList<SolrDocument> exact, ArrayList<SolrDocument> related, ArrayList<SolrDocument> broader, ArrayList<SolrDocument> narrover) throws Exception {
        return "<span class=\"help\" onclick=\"showTooltip(" + term + ", 'Simple Tooltip', ' " + createReplaceStrWithRelations(exact, related, broader, narrover) + " ')\"> " + term + " </span>"
                + "<span style=\"display:none\" id=\"" + term + "\"></span> ";
    }

    private String createTooltipWithRelationsSpanEncoded(String term, ArrayList<SolrDocument> exact, ArrayList<SolrDocument> related, ArrayList<SolrDocument> broader, ArrayList<SolrDocument> narrover) throws Exception {
        return "_small_span class=_dash_help_dash_ onclick=_dash_showTooltip(" + term + ", 'Simple Tooltip', ' " + createReplaceStrWithRelations(exact, related, broader, narrover) + " ')_dash__big_ " + term + " _small_/span_big_"
                + "_small_span style=_dash_display:none_dash_ id=_dash_" + term + "_dash__big__small_/span_big_ ";
    }
    
    private String createTooltipWithRelationsSpanEncoded(String term, List<SolrDocument> exact, List<SolrDocument> related, List<SolrDocument> broader, List<SolrDocument> narrover) throws Exception {
        return "_small_span class=_dash_help_dash_ onclick=_dash_showTooltip(" + term + ", 'Simple Tooltip', ' " + createReplaceStrWithRelations(exact, related, broader, narrover) + " ')_dash__big_ " + term + " _small_/span_big_"
                + "_small_span style=_dash_display:none_dash_ id=_dash_" + term + "_dash__big__small_/span_big_ ";
    }

    private String createReplaceStr(String term) throws Exception {
        MultilingualSearchManager msm = new MultilingualSearchManager();

        ArrayList<SolrDocument> documents = null;

        documents = msm.getQueryResults("http://localhost:8080/ks/KnowledgeService", "http://localhost:8080/rdf/fishery.rdf", term);

        StringBuilder buffer = new StringBuilder();

        for (SolrDocument doc : documents) {
            buffer.append("<p> <a href=");
            buffer.append(doc.getHost());
            buffer.append(">");
            buffer.append(doc.getTitle());
            buffer.append("</a> </p>");
        }

        return buffer.toString();
    }

    private String createReplaceStr(String term, String termList) throws Exception {
        MultilingualSearchManager msm = new MultilingualSearchManager();

        ArrayList<SolrDocument> documents = null;

        documents = msm.getQueryResults("http://localhost:8080/ks/KnowledgeService", "http://localhost:8080/rdf/" + termList + ".rdf", term);

        StringBuilder buffer = new StringBuilder();

        for (SolrDocument doc : documents) {
            buffer.append("<p> <a href=");
            buffer.append(doc.getHost());
            buffer.append(">");
            buffer.append(doc.getTitle());
            buffer.append("</a> </p>");
        }

        return buffer.toString();
    }

    private String createReplaceStr(ArrayList<SolrDocument> documents) throws Exception {

        StringBuilder buffer = new StringBuilder();

        for (SolrDocument doc : documents) {
            buffer.append("<p> <a href=");
            buffer.append(doc.getHost());
            buffer.append(">");
            buffer.append(doc.getTitle());
            buffer.append("</a> </p>");
        }

        return buffer.toString();
    }

    private String createReplaceStrWithRelations(ArrayList<SolrDocument> exact, ArrayList<SolrDocument> related, ArrayList<SolrDocument> broader, ArrayList<SolrDocument> narrover) throws Exception {

        StringBuilder buffer = new StringBuilder();

        buffer.append(convertSolrDoctoHtml(exact, "Exact"));
        buffer.append(convertSolrDoctoHtml(related, "Related"));
        buffer.append(convertSolrDoctoHtml(broader, "Broader"));
        buffer.append(convertSolrDoctoHtml(narrover, "Narrover"));

        return buffer.toString();
    }
    
    private String createReplaceStrWithRelations(List<SolrDocument> exact, List<SolrDocument> related, List<SolrDocument> broader, List<SolrDocument> narrover) throws Exception {

        StringBuilder buffer = new StringBuilder();

        buffer.append(convertSolrDoctoHtml(exact, "Exact"));
        buffer.append(convertSolrDoctoHtml(related, "Related"));
        buffer.append(convertSolrDoctoHtml(broader, "Broader"));
        buffer.append(convertSolrDoctoHtml(narrover, "Narrover"));

        return buffer.toString();
    }

    private String convertSolrDoctoHtml(ArrayList<SolrDocument> documents, String relation) {

        StringBuilder buffer = new StringBuilder();

        if (!documents.isEmpty()) {
            buffer.append("<h4> " + relation + " Matches </h4>");

            for (SolrDocument doc : documents) {

                if (doc.getHost().contains("wikipedia")) {
                    buffer.append("<p> <a href=");
                    buffer.append(doc.getHost());
                    buffer.append(">");
                    buffer.append(doc.getTitle());
                    buffer.append("</a> </p>");
                }

            }
        }

        return buffer.toString();

    }
    
    private String convertSolrDoctoHtml(List<SolrDocument> documents, String relation) {

        StringBuilder buffer = new StringBuilder();

        if (!documents.isEmpty()) {
            buffer.append("<h4> " + relation + " Matches </h4>");

            for (SolrDocument doc : documents) {

                if (doc.getHost().contains("wikipedia")) {
                    buffer.append("<p> <a href=");
                    buffer.append(doc.getHost());
                    buffer.append(">");
                    buffer.append(doc.getTitle());
                    buffer.append("</a> </p>");
                }

            }
        }

        return buffer.toString();

    }

    private boolean isReplaced(ArrayList<String> replaced, String term) {

        for (String str : replaced) {
            if (str.contains(term) || term.contains(str)) {
                return true;
            }
        }

        return false;
    }
}
