package in.at.maven;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;

public class CaptionExtractor {
  public static void main(String[] args)throws IOException {
    PrintStream oldErr = System.err;
    PrintStream newErr = new PrintStream(new ByteArrayOutputStream());
    System.setErr(newErr);
    
    String fileName = null;
    if( args.length > 0)
      fileName = args[0];
    else {
      System.out.println("A PDF file is required as an input parameter");
      System.exit(0);
    }

    PDDocument doc = PDDocument.load(new File(fileName));    
    List <String> captions = getCaptions(doc);
    doc.close();
   
    String resultFileName = "./result.pdf"; 
    createDocument(resultFileName);
    writeDocument(resultFileName,captions);
    System.setErr(oldErr);

  }
  
  /**
   * Returns an ArrayList of String objects, that will be our captions.
   * The doc argument must specify the document object, where the text will be extracted from.
   * <p>
   * The method extracts the text from every page, and breaks it down into paragraphs.
   * Then it searches each paragraphs to find the one, that contains the page number
   * and then it will do the same with the captions.
   * To achive this, it uses patterns and symbols, "Figure" and the "–" for the caption 
   * and "All rights reserved" for the pagenumber.
   * This and the main method will throw an IOExecption, if the document is non-existent.
   * 
   * @param doc the document object, where from text will be extracted
   * @return captions from the images
   * @throws IOException If an input or output exception occurred
   */
  public static List<String> getCaptions(PDDocument doc) throws IOException{
    PDFTextStripper stripper = new PDFTextStripper();
    List <String> captions = new ArrayList<String>(); 

    for(int i=1;i<=doc.getNumberOfPages();i++) {

      stripper.setParagraphStart("pBegin");
      stripper.setStartPage(i);
      stripper.setEndPage(i);

      String [] lines = stripper.getText(doc).split(stripper.getParagraphStart());

      Pattern patternForPageNumber = Pattern.compile("All rights reserved");
      Matcher matcher;
      String pageNumber = null;

      //For even pages, the first and for odd pages the last word will be the page number.
      for(String line : lines) {
        matcher = patternForPageNumber.matcher(line);
        if(matcher.find()) {
          String [] words = line.split(" ");
          if(i % 2 == 0) 
            pageNumber = words[0];
          else
            pageNumber = words[words.length-1];
        }
      }
      
      Pattern patternHead = Pattern.compile("Figure");
      Pattern patternTail = Pattern.compile("–");
      
      for(int j=0;j<lines.length;j++) {
        matcher = patternHead.matcher(lines[j]);
        if(matcher.find()) {
          matcher = patternTail.matcher(lines[j]);
          if(matcher.find()) {
            String line = lines[j]+" – Page "+pageNumber+"("+i+")";
            line = line.replace("\n", "").replace("\r", "");
            captions.add(line);
          }   
        }
      }
    }

    return captions; 
  }

  /**
   * Creates a new empty document
   * The fileName argument will specify the name of the new document.
   * This method has no return type, because i will open & write this document with an another method.
   * 
   * @param fileName the name of the new document
   */
  public static void createDocument(String fileName) throws IOException {
    PDDocument document = new PDDocument();
    PDPage myPage = new PDPage();
    document.addPage(myPage);
    document.save(fileName);
    document.close();
  }

  /**
   * This method writes the extracted captions to a new document.
   * Has no return type.
   * <p>
   * It opens the document and sets the page size, fonts and starting position.
   * Then it breaks the lines into shorter ones, that will fit the page's width.
   * It will start to write the lines to the page, when its near at the end,
   * itt will drop the page and create an another one (Set the pagesize, fonts, etc... again)
   * When its done, it will close and save the document as "results.pdf".
   * 
   * @param fileName the name of the created document
   * @param captions the image captions, that i extracted from the given document
   * @throws IOException If an input or output exception occurred
   */
  public static void writeDocument(String fileName, List<String> captions) throws IOException {
  
    PDDocument doc = PDDocument.load(new File(fileName));    
    PDPage myPage = doc.getPage(0);
    myPage.setMediaBox(PDRectangle.A4);
    float currentHeight = 792;
    
    PDPageContentStream contentStream = new PDPageContentStream( doc, myPage);  
    contentStream.beginText();   
    contentStream.setFont(PDType1Font.TIMES_ROMAN, 14);  
    contentStream.setLeading(14.5f);
    contentStream.newLineAtOffset(20, 792);
    
    List<String> lines = new ArrayList<String>();
    for(int i=0;i<captions.size();i++) {
      String [] words = captions.get(i).split(" ");
      String line = "";
      for(int j=0;j<words.length;j++) {
         if((line.length()+words[j].length())>90) {
           lines.add(line);
           line = "";
         }
         line += words[j];
         line += " ";
      }
      lines.add(line);
    }
    
    for(int i=0;i<lines.size();i++) {
      contentStream.showText(lines.get(i));
      contentStream.newLine();
      currentHeight -= 14;

      if(currentHeight < 50) {
        contentStream.endText(); 
        contentStream.close(); 
        myPage = new PDPage();
        myPage.setMediaBox(PDRectangle.A4);
        doc.addPage(myPage);
        contentStream = new PDPageContentStream(doc, myPage);
        contentStream.beginText();   
        contentStream.setFont(PDType1Font.TIMES_ROMAN, 14);  
        contentStream.setLeading(14.5f);
        contentStream.newLineAtOffset(20, 792);
        currentHeight = 792;
      }
    }
    contentStream.endText(); 
    contentStream.close(); 
    
    System.out.println("A new PDF document has been created called result.pdf!");  
    
    doc.save(fileName);
    doc.close();
  }
}