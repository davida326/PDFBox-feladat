package captionExtractor;

import java.io.File;
import java.io.IOException;
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

public class captionExtractor {

	  public static void main(String[] args)throws IOException {
		  
		  //Bemeneti paraméter ellenörzés
		  String fileName = null;
		  if( args.length > 0)
			  fileName = args[0];
		  else {
			  System.out.println("Szükséges egy PDF fájl, mint bemeneti paraméter");
			  System.exit(0);
		  }
		  

		  //Kapott PDF állományt, betöltöm és elkezdem feldolgozni		  
		  PDDocument doc = PDDocument.load(new File(fileName));
		  
		  //Egy arrayListben tárolom a képaláírásokat, mivel nem tudom pontosan mennyi szerepel a fájlban.
		  List <String> captions = getCaptions(doc);
		  
		  /* Console-ban is listázhatók a képaláírások.(teszteléshez használtam)
		  for(String i :captions)
			  System.out.println(i);
		  */
		  
		  //Kapott dokumentum lezárása.
		  doc.close();
		 
		  //Új PDF állomány készítése
		  String resultFileName = "./result.pdf"; 
		  createDocument(resultFileName);
		  
		  //PDF állomány írása
		  writeDocument(resultFileName,captions);
		 
	  }
	  //Létrehozok egy üres dokumentumot
	  public static void createDocument(String fileName) throws IOException {
		  PDDocument document = new PDDocument();
		  PDPage my_page = new PDPage();
		  //Egy üres oldalt adok hozzá.
		  document.addPage(my_page);
		  //Elmentem a fájlt.
		  document.save(fileName);
		  document.close();
	  }
	  public static void writeDocument(String fileName, List<String> captions) throws IOException {
		  
		  //Az előkészített fájlt megnyitom
		  PDDocument document = PDDocument.load(new File(fileName));    
		  PDPage my_page = document.getPage(0);
		  //Beállítom hozzá a méretet
		  my_page.setMediaBox(PDRectangle.A4);
		  PDRectangle pageSize = my_page.getMediaBox();
		  
		  //Az oldal méretei
		  float height = pageSize.getHeight();
		  float width = pageSize.getWidth();
		  //Az aktuális sor
		  float currentHeight = 792;
		  
		  //contentStream létrehozása
		  PDPageContentStream contentStream = new PDPageContentStream( document, my_page);  
	      contentStream.beginText();   
	      //Betűtípus kiválasztása 
		  contentStream.setFont(PDType1Font.TIMES_ROMAN, 14);  
		  contentStream.setLeading(14.5f);
		  //Sor pozíció beállítása
		  contentStream.newLineAtOffset(20, 792);
		  
		  // Itt kezdem el a hosszú sorok tördelését, ezeket ebbe a lines, ArrayListbe fogom gyűjteni 
		  List<String> lines = new ArrayList<String>();
		  for(int i=0;i<captions.size();i++) {
			  //Szétdarabolom őket whitespace-ek mentén
			  String [] words = captions.get(i).split(" ");
			  String line = "";
			  for(int j=0;j<words.length;j++) {
				  /*
				   * Megnézem előre, a következő szó és az aktuális sorom méretét.
				   * Így eldönthetem, hogy szükséges-e a sortörés.
				   */
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
			  //Az előbb elkészített sorokat elkezdem írni a PDF dokumentumba
			  contentStream.showText(lines.get(i));
			  contentStream.newLine();
			  //Itt mindig kivonom a karakter méretet, az aktuális pozícióból.
			  currentHeight -= 14;
			  //Amennyiben a lap végéhez közeledek és ez a távolság kevesebb mint 50 pt
			  //sortörést végzek
			  if(currentHeight < 50) {
				  //lezárom az jelenlegi contentStream-et
				  contentStream.endText(); 
				  contentStream.close(); 
				  //Új lapot vezetek be, aminek beállítom a méretét
				  my_page = new PDPage();
				  my_page.setMediaBox(PDRectangle.A4);
				  //Ezt hozzáadom a dokumentumhoz
				  document.addPage(my_page);
				  //Új contentStream-et hozok létre, az előbb már látott paraméterekkel
				  contentStream = new PDPageContentStream(document, my_page);
				  contentStream.beginText();   
				  contentStream.setFont(PDType1Font.TIMES_ROMAN, 14);  
				  contentStream.setLeading(14.5f);
				  contentStream.newLineAtOffset(20, 792);
				  currentHeight = 792;
			  }
		  }
		  contentStream.endText(); 
		  contentStream.close(); 
		  
		  System.out.println("Új PDF dokumentum elkészült result.pdf néven!");  
		  //Fájl mentése és lezárása 
		  document.save(fileName);
		  document.close();
	  }
	  
	  /*
	   * A képaláírásokat reguláris kifejezések segítségével kaptam meg.
	   */
	  public static List<String> getCaptions(PDDocument doc) throws IOException{
		  //Egy ArrayList amibe összeszedem a képaláírásokat
		  List <String> captionLst = new ArrayList<String>(); 
		  for(int i=1;i<=doc.getNumberOfPages();i++) {
			  String pageText = getTextFromPage(i,doc);
			  //A dokumentum összes lapjáról, egyenként kinyerem a szöveget
			  //Ezt a szöveget sorokra bontom
			  String lines[] = pageText.split("\\r?\\n");
			  
			  /*
			   * Az oldalszám sorában szerepel egy szövegrész, "© Adobe Systems Incorporated 2008 – All rights reserved"
			   * ami alapján meg tudom különböztetni az oldalszámot a többi számtól.
			   * Igazából én csak a végét használtam fel, mivel úgygondoltam hogy ez is elegendő.
			   */
			  Pattern patternForPageNumber = Pattern.compile("All rights reserved");
			  Matcher matcher;
			  //A pageNumber változóban fogom tárolni a dokumentum által használt oldalszámot
			  String pageNumber = null;
			  for(String line : lines) {
				  matcher = patternForPageNumber.matcher(line);
				  if(matcher.find()) {
					  // Szétszedem szavakra a sorokat, whitespace-ek mentén
					  String [] words = line.split(" ");
					  /*
					   * Az említett keresési mintánál, megfigyelhető hogy páratlan oldalak esetén, 
					   * az oldalszám az utolsó szó. 
					   * Páros oldalaknál viszont, a sorban az első szó tartalmazza az oldalszámot.
					   */
					  if(i % 2 == 0) 
						  pageNumber = words[0];
					  else
						  pageNumber = words[words.length-1];
				  }
			  }
			  /*
			   * Megfigyeltem hogy a szövegben, egy átlagos képaláírásban mik a közösek, és a következőre jutottam:
			   * "Figure"*Bármi*[Egy szám][whitespace][–][whitespace]
			   * Így próbáltam meg kiszedni a szövegből, ezeket a részeket.
			   */
			  Pattern patternHead = Pattern.compile("Figure");
			  Pattern patternTail = Pattern.compile("\\d\s–\s");
			  
			  for(int j=0;j<lines.length;j++) {
				  matcher = patternHead.matcher(lines[j]);
				  //Amikor megtalálta a "Figure" részt, azt a String-et nézi tovább 
				  if(matcher.find()) {
					  matcher = patternTail.matcher(lines[j]);
					  //Amennyiben megtalálta hozzá a [whitespace][–][whitespace] részt is, találatnak minősítem
					  if(matcher.find()) 
						  captionLst.add(lines[j]+" – Page "+pageNumber+"("+i+")");
				  }
			  }
		  }
		  return captionLst; 
	  }
	  /*
	   * A szöveget szerettem volna, oldalanként kinyerni az állományból. 
	   * Erre viszont konkrétan nem találtam megoldást, ezért a következő módszert alkalmaztam. 
	   */
	  public static String getTextFromPage(int pageNumber, PDDocument doc) throws IOException {
		  //Létrehozok egy Stripper-t
		  PDFTextStripper reader = new PDFTextStripper();
		  //Megadom az oldalszámot, ahonnan kezdve szeretném kinyerni a szöveget
		  reader.setStartPage(pageNumber);
		  //Ugyanezt az oldalt adom meg, utolsó oldalnak is
		  reader.setEndPage(pageNumber);
		  //Így egy oldalról kapom meg a szöveget
		  return reader.getText(doc);
	  }
}  

