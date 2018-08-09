package ascdc.dhtext;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import ascdc.sinica.dhtext.tool.solr.AuthorityFileUploadException;
import ascdc.sinica.dhtext.tool.solr.UploadAuthority;;

public class UploadAuthorityTest extends TestCase {
	
	private String username = "chiamin";
	private String authorityId = "0";
	private String title = "單元測試";
	private String xlsxPath = "data/xlsx/";
	private String txtPath = "data/txt/";
	private String csvPath = "data/csv/";
	private String tsvPath = "data/tsv/";
	
	/**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public UploadAuthorityTest( String testName )
    {
        super( testName );
    }
    
    
    /******************** XLSX ********************/
    public void testXlsxWithNote1(){
    	
		String fileName = "太平廣記卷96權威詞-synonym.xlsx";
		boolean setNote = true;
		UploadAuthority uploadAuthority = new UploadAuthority();
		try {
			JSONArray arr = uploadAuthority.xlsxToSolrDoc(this.xlsxPath, fileName, this.title, this.authorityId, setNote);
			JSONObject obj = uploadAuthority.xlsxToJstreeJSON(this.xlsxPath, fileName, this.title, setNote, this.username);
			assertTrue( true );
			
		} catch (AuthorityFileUploadException e) {
			System.out.println("\n\n\ntestXlsxWithNote1");
			e.printStackTrace();
			String message = e.getMessage();
			JSONArray error_data = e.getErrorData();
			
			ArrayList<String> error_msg = e.getErrorMessage();
			for(int i = 0; i < error_msg.size(); ++i){
				System.out.println(error_msg.get(i));
				System.out.println(error_data.get(i));
				System.out.println("");
			}
			assertTrue( true );
		} catch (Exception e) {
			assertTrue( false );
		}
    }
    
    public void testXlsxWithoutNote1() {
    	
    	String fileName = "太平廣記卷96權威詞-synonym.xlsx";
		boolean setNote = false;
		UploadAuthority uploadAuthority = new UploadAuthority();
		try {
			JSONArray arr = uploadAuthority.xlsxToSolrDoc(this.xlsxPath, fileName, this.title, this.authorityId, setNote);
			JSONObject obj = uploadAuthority.xlsxToJstreeJSON(this.xlsxPath, fileName, this.title, setNote, this.username);
			assertTrue( true );
			
		} catch (AuthorityFileUploadException e) {
			System.out.println("\n\n\ntestXlsxWithoutNote1");
			e.printStackTrace();
			String message = e.getMessage();
			JSONArray error_data = e.getErrorData();
			
			ArrayList<String> error_msg = e.getErrorMessage();
			for(int i = 0; i < error_msg.size(); ++i){
				System.out.println(error_msg.get(i));
				System.out.println(error_data.get(i));
				System.out.println("");
			}
			assertTrue( true );
		} catch (Exception e) {
			assertTrue( false );
		}
    }
    
    public void testXlsxWithNote2(){
    	
		String fileName = "太平廣記卷96權威詞.xlsx";
		boolean setNote = true;
		UploadAuthority uploadAuthority = new UploadAuthority();
		try {
			JSONArray arr = uploadAuthority.xlsxToSolrDoc(this.xlsxPath, fileName, this.title, this.authorityId, setNote);
			JSONObject obj = uploadAuthority.xlsxToJstreeJSON(this.xlsxPath, fileName, this.title, setNote, this.username);
			assertTrue( true );
			
		} catch (AuthorityFileUploadException e) {
			System.out.println("\n\n\ntestXlsxWithNote2");
			e.printStackTrace();
			String message = e.getMessage();
			JSONArray error_data = e.getErrorData();
			
			ArrayList<String> error_msg = e.getErrorMessage();
			for(int i = 0; i < error_msg.size(); ++i){
				System.out.println(error_msg.get(i));
				System.out.println(error_data.get(i));
				System.out.println("");
			}
			assertTrue( true );
		} catch (Exception e) {
			assertTrue( false );
		}
    }
    
    public void testXlsxWithoutNote2() {
    	
    	String fileName = "太平廣記卷96權威詞.xlsx";
		boolean setNote = false;
		UploadAuthority uploadAuthority = new UploadAuthority();
		try {
			JSONArray arr = uploadAuthority.xlsxToSolrDoc(this.xlsxPath, fileName, this.title, this.authorityId, setNote);
			JSONObject obj = uploadAuthority.xlsxToJstreeJSON(this.xlsxPath, fileName, this.title, setNote, this.username);
			assertTrue( true );
			
		} catch (AuthorityFileUploadException e) {
			System.out.println("\n\n\ntestXlsxWithoutNote2");
			e.printStackTrace();
			String message = e.getMessage();
			JSONArray error_data = e.getErrorData();
			
			ArrayList<String> error_msg = e.getErrorMessage();
			for(int i = 0; i < error_msg.size(); ++i){
				System.out.println(error_msg.get(i));
				System.out.println(error_data.get(i));
				System.out.println("");
			}
			assertTrue( true );
		} catch (Exception e) {
			assertTrue( false );
		}
    }
    
    public void testXlsxWithNote3(){
    	
		String fileName = "本草綱目.xlsx";
		boolean setNote = true;
		UploadAuthority uploadAuthority = new UploadAuthority();
		try {
			JSONArray arr = uploadAuthority.xlsxToSolrDoc(this.xlsxPath, fileName, this.title, this.authorityId, setNote);
			JSONObject obj = uploadAuthority.xlsxToJstreeJSON(this.xlsxPath, fileName, this.title, setNote, this.username);
			assertTrue( true );
			
		} catch (AuthorityFileUploadException e) {
			System.out.println("\n\n\ntestXlsxWithNote3");
			
			e.printStackTrace();
			String message = e.getMessage();
			JSONArray error_data = e.getErrorData();
			
			ArrayList<String> error_msg = e.getErrorMessage();
			for(int i = 0; i < error_msg.size(); ++i){
				System.out.println(error_msg.get(i));
				System.out.println(error_data.get(i));
				System.out.println("");
			}
			assertTrue( true );
		} catch (Exception e) {
			assertTrue( false );
		}
    }
    
    public void testXlsxWithoutNote3() {
    	
    	String fileName = "本草綱目.xlsx";
		boolean setNote = false;
		UploadAuthority uploadAuthority = new UploadAuthority();
		try {
			JSONArray arr = uploadAuthority.xlsxToSolrDoc(this.xlsxPath, fileName, this.title, this.authorityId, setNote);
			JSONObject obj = uploadAuthority.xlsxToJstreeJSON(this.xlsxPath, fileName, this.title, setNote, this.username);
			assertTrue( true );
			
		} catch (AuthorityFileUploadException e) {
			System.out.println("\n\n\ntestXlsxWithoutNote3");
			e.printStackTrace();
			String message = e.getMessage();
			JSONArray error_data = e.getErrorData();
			
			ArrayList<String> error_msg = e.getErrorMessage();
			for(int i = 0; i < error_msg.size(); ++i){
				System.out.println(error_msg.get(i));
				System.out.println(error_data.get(i));
				System.out.println("");
			}
			assertTrue( true );
		} catch (Exception e) {
			assertTrue( false );
		}
    }
    
    public void testXlsxWithNote4(){
    	
		String fileName = "法鼓山地名2.xlsx";
		boolean setNote = true;
		UploadAuthority uploadAuthority = new UploadAuthority();
		try {
			JSONArray arr = uploadAuthority.xlsxToSolrDoc(this.xlsxPath, fileName, this.title, this.authorityId, setNote);
			JSONObject obj = uploadAuthority.xlsxToJstreeJSON(this.xlsxPath, fileName, this.title, setNote, this.username);
			assertTrue( true );
			
		} catch (AuthorityFileUploadException e) {
			System.out.println("\n\n\ntestXlsxWithNote4");
			e.printStackTrace();
			String message = e.getMessage();
			JSONArray error_data = e.getErrorData();
			
			ArrayList<String> error_msg = e.getErrorMessage();
			for(int i = 0; i < error_msg.size(); ++i){
				System.out.println(error_msg.get(i));
				System.out.println(error_data.get(i));
				System.out.println("");
			}
			assertTrue( true );
		} catch (Exception e) {
			assertTrue( false );
		}
    }
    
    public void testXlsxWithoutNote4() {
    	
    	String fileName = "法鼓山地名.xlsx";
		boolean setNote = true;
		UploadAuthority uploadAuthority = new UploadAuthority();
		try {
			JSONArray arr = uploadAuthority.xlsxToSolrDoc(this.xlsxPath, fileName, this.title, this.authorityId, setNote);
			JSONObject obj = uploadAuthority.xlsxToJstreeJSON(this.xlsxPath, fileName, this.title, setNote, this.username);
			assertTrue( true );
			
		} catch (AuthorityFileUploadException e) {
			System.out.println("\n\n\ntestXlsxWithoutNote4");
			e.printStackTrace();
			String message = e.getMessage();
			JSONArray error_data = e.getErrorData();
			
			ArrayList<String> error_msg = e.getErrorMessage();
			for(int i = 0; i < error_msg.size(); ++i){
				System.out.println(error_msg.get(i));
				System.out.println(error_data.get(i));
				System.out.println("");
			}
			assertTrue( true );
		} catch (Exception e) {
			assertTrue( false );
		}
    }
    
    public void testXlsxWithNote5(){
    	
		String fileName = "法鼓山地名.xlsx";
		boolean setNote = false;
		UploadAuthority uploadAuthority = new UploadAuthority();
		try {
			JSONArray arr = uploadAuthority.xlsxToSolrDoc(this.xlsxPath, fileName, this.title, this.authorityId, setNote);
			JSONObject obj = uploadAuthority.xlsxToJstreeJSON(this.xlsxPath, fileName, this.title, setNote, this.username);
			assertTrue( true );
			
		} catch (AuthorityFileUploadException e) {
			System.out.println("\n\n\ntestXlsxWithNote5");
			assertTrue( true );
		} catch (Exception e) {
			assertTrue( false );
		}
    }
    
    public void testXlsxWithoutNote5() {
    	
    	String fileName = "法鼓山地名2.xlsx";
		boolean setNote = false;
		UploadAuthority uploadAuthority = new UploadAuthority();
		try {
			JSONArray arr = uploadAuthority.xlsxToSolrDoc(this.xlsxPath, fileName, this.title, this.authorityId, setNote);
			JSONObject obj = uploadAuthority.xlsxToJstreeJSON(this.xlsxPath, fileName, this.title, setNote, this.username);
			assertTrue( true );
			
		} catch (AuthorityFileUploadException e) {
			System.out.println("\n\n\ntestXlsxWithoutNote5");
			e.printStackTrace();
			String message = e.getMessage();
			JSONArray error_data = e.getErrorData();
			
			ArrayList<String> error_msg = e.getErrorMessage();
			for(int i = 0; i < error_msg.size(); ++i){
				System.out.println(error_msg.get(i));
				System.out.println(error_data.get(i));
				System.out.println("");
			}
			assertTrue( true );
		} catch (Exception e) {
			assertTrue( false );
		}
    }
    
    public void testXlsxWithNote6(){
    	
		String fileName = "太平廣記卷96權威詞-no-note.xlsx";
		boolean setNote = true;
		UploadAuthority uploadAuthority = new UploadAuthority();
		try {
			JSONArray arr = uploadAuthority.xlsxToSolrDoc(this.xlsxPath, fileName, this.title, this.authorityId, setNote);
			JSONObject obj = uploadAuthority.xlsxToJstreeJSON(this.xlsxPath, fileName, this.title, setNote, this.username);
			assertTrue( true );
			
		} catch (AuthorityFileUploadException e) {
			System.out.println("\n\n\ntestXlsxWithNote6");
			e.printStackTrace();
			String message = e.getMessage();
			JSONArray error_data = e.getErrorData();
			
			ArrayList<String> error_msg = e.getErrorMessage();
			for(int i = 0; i < error_msg.size(); ++i){
				System.out.println(error_msg.get(i));
				System.out.println(error_data.get(i));
				System.out.println("");
			}
			assertTrue( true );
		} catch (Exception e) {
			assertTrue( false );
		}
    }
    
    public void testXlsxWithoutNote6() {
    	
    	String fileName = "太平廣記卷96權威詞-no-note.xlsx";
		boolean setNote = false;
		UploadAuthority uploadAuthority = new UploadAuthority();
		try {
			JSONArray arr = uploadAuthority.xlsxToSolrDoc(this.xlsxPath, fileName, this.title, this.authorityId, setNote);
			JSONObject obj = uploadAuthority.xlsxToJstreeJSON(this.xlsxPath, fileName, this.title, setNote, this.username);
			assertTrue( true );
			
		} catch (AuthorityFileUploadException e) {
			System.out.println("\n\n\ntestXlsxWithoutNote6");
			e.printStackTrace();
			String message = e.getMessage();
			JSONArray error_data = e.getErrorData();
			
			ArrayList<String> error_msg = e.getErrorMessage();
			for(int i = 0; i < error_msg.size(); ++i){
				System.out.println(error_msg.get(i));
				System.out.println(error_data.get(i));
				System.out.println("");
			}
			assertTrue( true );
		} catch (Exception e) {
			assertTrue( false );
		}
    }
    
    
    public void testXlsxWithNote7(){
    	/** 標頭缺少「註解」註解*/
		String fileName = "本草綱目-header.xlsx";
		boolean setNote = true;
		UploadAuthority uploadAuthority = new UploadAuthority();
		try {
			JSONArray arr = uploadAuthority.xlsxToSolrDoc(this.xlsxPath, fileName, this.title, this.authorityId, setNote);
			JSONObject obj = uploadAuthority.xlsxToJstreeJSON(this.xlsxPath, fileName, this.title, setNote, this.username);
			assertTrue( true );
			
		} catch (AuthorityFileUploadException e) {
			System.out.println("\n\n\ntestXlsxWithNote7");
			e.printStackTrace();
			String message = e.getMessage();
			JSONArray error_data = e.getErrorData();
			
			ArrayList<String> error_msg = e.getErrorMessage();
			for(int i = 0; i < error_msg.size(); ++i){
				System.out.println(error_msg.get(i));
				System.out.println(error_data.get(i));
				System.out.println("");
			}
			assertTrue( true );
		} catch (Exception e) {
			assertTrue( false );
		}
    }
    
    public void testXlsxWithoutNote7() {
    	/** 標頭缺少「註解」註解*/
    	String fileName = "本草綱目-header.xlsx";
		boolean setNote = false;
		UploadAuthority uploadAuthority = new UploadAuthority();
		try {
			JSONArray arr = uploadAuthority.xlsxToSolrDoc(this.xlsxPath, fileName, this.title, this.authorityId, setNote);
			JSONObject obj = uploadAuthority.xlsxToJstreeJSON(this.xlsxPath, fileName, this.title, setNote, this.username);
			assertTrue( true );
			
		} catch (AuthorityFileUploadException e) {
			System.out.println("\n\n\ntestXlsxWithoutNote7");
			e.printStackTrace();
			String message = e.getMessage();
			JSONArray error_data = e.getErrorData();
			
			ArrayList<String> error_msg = e.getErrorMessage();
			for(int i = 0; i < error_msg.size(); ++i){
				System.out.println(error_msg.get(i));
				System.out.println(error_data.get(i));
				System.out.println("");
			}
			assertTrue( true );
		} catch (Exception e) {
			assertTrue( false );
		}
    }
    
    /******************** END XLSX ********************/
    
    /******************** CSV ********************/
    public void testCsvWithNote1(){
    	String fileName = "本草綱目.csv";
		boolean setNote = true;
		UploadAuthority uploadAuthority = new UploadAuthority();
		try {
			JSONArray arr = uploadAuthority.csvToSolrDoc(this.csvPath, fileName, this.title, this.authorityId, setNote);
			JSONObject obj = uploadAuthority.csvToJstreeJSON(this.csvPath, fileName, this.title, setNote, this.username);
			assertTrue( true );
			
		} catch (AuthorityFileUploadException e) {
			System.out.println("\n\n\ntestCsvWithNote1");
			e.printStackTrace();
			String message = e.getMessage();
			JSONArray error_data = e.getErrorData();
			
			ArrayList<String> error_msg = e.getErrorMessage();
			for(int i = 0; i < error_msg.size(); ++i){
				System.out.println(error_msg.get(i));
				System.out.println(error_data.get(i));
				System.out.println("");
			}
			assertTrue( true );
		} catch (Exception e) {
			assertTrue( false );
		}
    }
    
    public void testCsvWithoutNote1(){
    	String fileName = "本草綱目.csv";
		boolean setNote = false;
		UploadAuthority uploadAuthority = new UploadAuthority();
		try {
			JSONArray arr = uploadAuthority.csvToSolrDoc(this.csvPath, fileName, this.title, this.authorityId, setNote);
			JSONObject obj = uploadAuthority.csvToJstreeJSON(this.csvPath, fileName, this.title, setNote, this.username);
			assertTrue( true );
			
		} catch (AuthorityFileUploadException e) {
			System.out.println("\n\n\ntestCsvWithoutNote1");
			e.printStackTrace();
			String message = e.getMessage();
			JSONArray error_data = e.getErrorData();
			
			ArrayList<String> error_msg = e.getErrorMessage();
			for(int i = 0; i < error_msg.size(); ++i){
				System.out.println(error_msg.get(i));
				System.out.println(error_data.get(i));
				System.out.println("");
			}
			assertTrue( true );
		} catch (Exception e) {
			assertTrue( false );
		}
    }
    /******************** END CSV ********************/
    
    /******************** TXT ********************/
    public void testTxtWithNote1(){
    	String fileName = "藥名.txt";
		boolean setNote = true;
		UploadAuthority uploadAuthority = new UploadAuthority();
		try {
			JSONArray arr = uploadAuthority.txtToSolrDoc(this.txtPath, fileName, this.title, this.authorityId, setNote);
			JSONObject obj = uploadAuthority.txtToJstreeJSON(this.title, setNote, this.username);
			assertTrue( true );
			
		} catch (AuthorityFileUploadException e) {
			System.out.println("\n\n\ntestTxtWithNote1");
			e.printStackTrace();
			String message = e.getMessage();
			JSONArray error_data = e.getErrorData();
			
			ArrayList<String> error_msg = e.getErrorMessage();
			for(int i = 0; i < error_msg.size(); ++i){
				System.out.println(error_msg.get(i));
				System.out.println(error_data.get(i));
				System.out.println("");
			}
			assertTrue( true );
		} catch (Exception e) {
			assertTrue( false );
		}
    }
    
    public void testTxtWithoutNote1(){
    	String fileName = "藥名.txt";
		boolean setNote = false;
		UploadAuthority uploadAuthority = new UploadAuthority();
		try {
			JSONArray arr = uploadAuthority.txtToSolrDoc(this.txtPath, fileName, this.title, this.authorityId, setNote);
			JSONObject obj = uploadAuthority.txtToJstreeJSON(this.title, setNote, this.username);
			assertTrue( true );
			
		} catch (AuthorityFileUploadException e) {
			System.out.println("\n\n\ntestTxtWithoutNote1");
			e.printStackTrace();
			String message = e.getMessage();
			JSONArray error_data = e.getErrorData();
			
			ArrayList<String> error_msg = e.getErrorMessage();
			for(int i = 0; i < error_msg.size(); ++i){
				System.out.println(error_msg.get(i));
				System.out.println(error_data.get(i));
				System.out.println("");
			}
			assertTrue( true );
		} catch (Exception e) {
			assertTrue( false );
		}
    }
    
    /******************** END TXT ********************/
    
    
    /******************** TSV ********************/
    public void testTsvWithNote1(){
    	String fileName = "本草綱目.tsv";
		boolean setNote = true;
		UploadAuthority uploadAuthority = new UploadAuthority();
		try {
			JSONArray arr = uploadAuthority.tsvToSolrDoc(this.tsvPath, fileName, this.title, this.authorityId, setNote);
			JSONObject obj = uploadAuthority.tsvToJstreeJSON(this.tsvPath, fileName, this.title, setNote, this.username);
			assertTrue( true );
			
		} catch (AuthorityFileUploadException e) {
			System.out.println("\n\n\ntestTsvWithNote1");
			e.printStackTrace();
			String message = e.getMessage();
			JSONArray error_data = e.getErrorData();
			
			ArrayList<String> error_msg = e.getErrorMessage();
			for(int i = 0; i < error_msg.size(); ++i){
				System.out.println(error_msg.get(i));
				System.out.println(error_data.get(i));
				System.out.println("");
			}
			assertTrue( true );
		} catch (Exception e) {
			assertTrue( false );
		}
    }
    
    public void testTsvWithoutNote1(){
    	String fileName = "本草綱目.tsv";
		boolean setNote = false;
		UploadAuthority uploadAuthority = new UploadAuthority();
		try {
			JSONArray arr = uploadAuthority.tsvToSolrDoc(this.tsvPath, fileName, this.title, this.authorityId, setNote);
			JSONObject obj = uploadAuthority.tsvToJstreeJSON(this.tsvPath, fileName, this.title, setNote, this.username);
			assertTrue( true );
			
		} catch (AuthorityFileUploadException e) {
			System.out.println("\n\n\ntestTsvWithoutNote1");
			e.printStackTrace();
			String message = e.getMessage();
			JSONArray error_data = e.getErrorData();
			
			ArrayList<String> error_msg = e.getErrorMessage();
			for(int i = 0; i < error_msg.size(); ++i){
				System.out.println(error_msg.get(i));
				System.out.println(error_data.get(i));
				System.out.println("");
			}
			assertTrue( true );
		} catch (Exception e) {
			assertTrue( false );
		}
    }
    /******************** END TSV ********************/
    
    
    
}
