package ascdc.sinica.dhtext.solr;







import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.*;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.json.*;

import ascdc.sinica.dhtext.util.io.JSONOperate;


public class UploadAuthority {
	
	
	
	@SuppressWarnings("deprecation")
	private static JSONObject listToJstreeJSON(ArrayList<String> list, ArrayList<String> headers, int i){
		
		if(i < list.size()){
			String data = list.get(i);
			if(!data.equals("")){
				try {
					JSONObject jsonObject = new JSONObject();
					JSONArray children = new JSONArray();
					String header = headers.get(i);
					boolean isLeaf = headers.size() == i + 2;
					if(!isLeaf){
						JSONObject child = listToJstreeJSON(list, headers, i+1);
						if(child != null)
							children.put(child);
//						jsonObject.put("cat", header);
						jsonObject.put("type", "subDir");
						jsonObject.put("children", children);
						jsonObject.put("text", data);
						return jsonObject;
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else{
				return listToJstreeJSON(list, headers, i+1);
			}
		}
		return null;
	}
	
	private static JSONObject toJstreeJSON(String filepath, String filename, String title, ArrayList<String> headers, ArrayList<ArrayList<String>> data){
		
		JSONObject jsonObj = new JSONObject();
		JSONArray  jsonArr = new JSONArray();
		
		for(ArrayList<String> row : data){
			JSONObject jsonRow = listToJstreeJSON(row, headers, 0);
			jsonArr = JSONOperate.appendJstree(jsonArr, jsonRow);
		}
		
		jsonObj.put("cat", "目錄");
		jsonObj.put("type", "default");
		jsonObj.put("text", title);
		jsonObj.put("children", jsonArr); 
		
		return jsonObj;
	}
	
	private static JSONArray toSolrDoc(String filepath, String filename, String title, String authorityId, String sep){
		
		FileInputStream fileInputStream = null;
		Scanner scanner = null;
		JSONArray arr = new JSONArray();
		
		HashSet<String> keyword_set = new HashSet<String>(); // 重複的關鍵字
		try {
			fileInputStream = new FileInputStream(filepath + filename);
			scanner = new Scanner(fileInputStream, "UTF-8");
			
			String[] headers = scanner.nextLine().split(sep);
			int limit = headers.length;
			int num = 0;
			while(scanner.hasNextLine()){
				
				String[] keywords = scanner.nextLine().split(sep, limit);
				
				// get path
				String path = title + "/";
				int i;
				for(i = 0; i < headers.length-2; ++i){
					if(!keywords[i].equals(""))
						path += keywords[i] + "/";
				}
				
				// 過濾空字串和重複的關鍵字
				String keyword = keywords[i++];
				if(!keyword.equals("") && !keyword_set.contains(path + keyword)){
					JSONObject json = new JSONObject();
					json.put("id", authorityId + "-" + Integer.toString(num++));
					json.put("authorityId", authorityId);
					json.put("path", path);
					json.put("text", keyword);
					
					if(!sep.equals("\n"))
						json.put("note", keywords[i].equals("") ? "無" : keywords[i]);
					else
						json.put("note", "無");
					arr.put(json);
					keyword_set.add(path + keyword);
				}
			}			
			fileInputStream.close();
			scanner.close();
		
		} catch(IOException e){
			e.printStackTrace();
		} 
		
		return arr;
	}
	
	private static ArrayList<ArrayList<String>> readTextUploadFile(String filepath, String filename, String sep, boolean setNote) throws AuthorityFileUploadException{

		FileInputStream fileInputStream = null;
		Scanner scanner = null;
		ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
		try {
			fileInputStream = new FileInputStream(filepath + filename);
			scanner = new Scanner(fileInputStream, "UTF-8");
			
			String[] headers = scanner.nextLine().split(sep);
			
			int limit = headers.length;
			System.out.println("limit: " + limit);
			while(scanner.hasNextLine()){
				String[] texts = scanner.nextLine().split(sep, limit);
				
				ArrayList<String> row_data = new ArrayList<String>(Arrays.asList(texts));
				if(!sep.equals("\n"))
					validate(row_data, limit, setNote, data.size()+1);
				data.add(row_data);
			}

			fileInputStream.close();
			scanner.close();
		
		} catch(IOException e){
			e.printStackTrace();
		}
		
		return data; 
	}
	
	
	public static ArrayList<String> XSSFRowToHeader(XSSFRow row){
		
		ArrayList<String> header = new ArrayList<String>();
		Iterator<Cell> cells = row.cellIterator();
		
		while(cells.hasNext()){
			XSSFCell cell = (XSSFCell) cells.next();
			if(cell.getCellType() == XSSFCell.CELL_TYPE_STRING){
				header.add(cell.getStringCellValue());
			}
		}
		
		// 忽略後面欄位的空字串
		for(int i = header.size()-1; i >= 0; --i){
			if(header.get(i).equals("")){
				header.remove(i);
			} else{
				break;
			}
		}
		
		return header;
	}
	
	public static ArrayList<String> XSSFRowToArrayList(XSSFRow row, int limit){
		
		ArrayList<String> list = new ArrayList<String>();
		Iterator<Cell> cells = row.cellIterator();
		
		while(cells.hasNext() && limit > 0){
			XSSFCell cell = (XSSFCell) cells.next();
			list.add(cell.getStringCellValue());
			--limit;
		}
		
		return list;
	}

	@SuppressWarnings("deprecation")
	public static JSONObject xlsxToJstreeJSON(String filepath, String filename, String title){
		

		ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
		ArrayList<String> headers = null;
		try {
			InputStream excelToRead = new FileInputStream(filepath + filename);
			XSSFWorkbook workbook = new XSSFWorkbook(excelToRead);
			XSSFSheet sheet = workbook.getSheetAt(0);			
			Iterator<Row> rows = sheet.rowIterator();
			headers = UploadAuthority.XSSFRowToHeader((XSSFRow) rows.next());
			while(rows.hasNext()){
				XSSFRow row = (XSSFRow) rows.next();
				data.add(XSSFRowToArrayList(row, headers.size()));
			}
			
			workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		

		return toJstreeJSON(filepath, filename, title, headers, data);
	}
	
	public static JSONObject txtToJstreeJSON(String filepath, String filename, String title){
	
		return toJstreeJSON(filepath, filename, title, new ArrayList<String>(), new ArrayList<ArrayList<String>>());
	}
	
	public static JSONObject csvToJstreeJSON(String filepath, String filename, String title){
		
		String sep = ",";
		FileInputStream fileInputStream = null;
		Scanner scanner = null;
		ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
		ArrayList<String> headers = null;
		try {
			fileInputStream = new FileInputStream(filepath + filename);
			scanner = new Scanner(fileInputStream, "UTF-8");
			
			headers = new ArrayList<String>(Arrays.asList(scanner.nextLine().split(sep)));
			int col = headers.size();
			while(scanner.hasNextLine()){
				ArrayList<String> list = new ArrayList<String>(Arrays.asList(scanner.nextLine().split(sep, col)));				
				data.add(list);
			}

			fileInputStream.close();
			scanner.close();
		} catch(IOException e){
			e.printStackTrace();
		} 
		
		
		return toJstreeJSON(filepath, filename, title, headers, data);
	}
	
	public static JSONObject tsvToJstreeJSON(String filepath, String filename, String title){
		
		String sep = "\t";
		FileInputStream fileInputStream = null;
		Scanner scanner = null;
		ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
		ArrayList<String> headers = null;
		try {
			fileInputStream = new FileInputStream(filepath + filename);
			scanner = new Scanner(fileInputStream, "UTF-8");
			
			headers = new ArrayList<String>(Arrays.asList(scanner.nextLine().split(sep)));
			int col = headers.size();
			while(scanner.hasNextLine()){
				ArrayList<String> list = new ArrayList<String>(Arrays.asList(scanner.nextLine().split(sep, col)));				
				data.add(list);
			}

			fileInputStream.close();
			scanner.close();
		
		} catch(IOException e){
			e.printStackTrace();
		} 
		return toJstreeJSON(filepath, filename, title, headers, data);
	}
	
	public static JSONArray toSolrDoc(String filepath, String filename, String title, String authorityId, ArrayList<ArrayList<String>> data){
		

		JSONArray arr = new JSONArray();
		
		HashSet<String> keyword_set = new HashSet<String>(); // 重複的關鍵字
	
		int num = 0;
		for(ArrayList<String> row : data){
							
			// get path
			String path = title + "/";
			int i;
			for(i = 0; i < row.size()-2; ++i){
				if(!row.get(i).equals(""))
					path += row.get(i) + "/";
			}
			
			// 過濾空字串和重複的關鍵字
			String keyword = row.get(i++);
			String note = i==row.size() ? "無" : (row.get(i).equals("") ? "無" : row.get(i));
			if(!keyword.equals("") && !keyword_set.contains(path + keyword)){
				JSONObject json = new JSONObject();
				json.put("id", authorityId + "-" + Integer.toString(num++));
				json.put("authorityId", authorityId);
				json.put("path", path);
				json.put("text", keyword);
				json.put("note", note);
				System.out.println(json);
				arr.put(json);
				keyword_set.add(path + keyword);
			}
		}			

		return arr;
	}
	
	public static JSONArray csvToSolrDoc(String filepath, String filename, String title, String authorityId, boolean setNote)throws AuthorityFileUploadException{
		
		String sep = ",";
		ArrayList<ArrayList<String>> data = readTextUploadFile(filepath, filename, sep, setNote);
		
		return toSolrDoc(filepath, filename, title, authorityId, data);
	}
	
	
	public static JSONArray tsvToSolrDoc(String filepath, String filename, String title, String authorityId, boolean setNote) throws AuthorityFileUploadException{
		
		String sep = "\t";
		ArrayList<ArrayList<String>> data = readTextUploadFile(filepath, filename, sep, setNote);
		
		return toSolrDoc(filepath, filename, title, authorityId, data);
	}
	
	
	public static JSONArray txtToSolrDoc(String filepath, String filename, String title, String authorityId, boolean setNote) throws AuthorityFileUploadException{
	
		String sep = "\n";
		ArrayList<ArrayList<String>> data = readTextUploadFile(filepath, filename, sep, setNote);
		return toSolrDoc(filepath, filename, title, authorityId, data);
	}
	
	@SuppressWarnings("deprecation")
	public static JSONArray xlsxToSolrDoc(String filepath, String filename, String title, String authorityId, boolean setNote) throws AuthorityFileUploadException{
		
		ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
		
		try {
			InputStream excelToRead = new FileInputStream(filepath + filename);			
			XSSFWorkbook workbook = new XSSFWorkbook(excelToRead);
			XSSFSheet sheet = workbook.getSheetAt(0);			
			Iterator<Row> rows = sheet.rowIterator();
			
			ArrayList<String> headers = UploadAuthority.XSSFRowToHeader((XSSFRow) rows.next());
			
			
			System.out.println(headers);
			int limit = headers.size(); 
			while(rows.hasNext()){
				XSSFRow row = (XSSFRow) rows.next();
				ArrayList<String> row_data = UploadAuthority.XSSFRowToArrayList(row, limit);
				validate(row_data, limit, setNote, data.size()+1); // throw AuthorityFileUploadException
				data.add(row_data);
				
			}
			workbook.close();
		}  catch (IOException | JSONException e) {
			e.printStackTrace();
		}
		return toSolrDoc(filepath, filename, title, authorityId, data);
	}
	public static void x(ArrayList<String> row_data, int limit, boolean setNote, int index) throws AuthorityFileUploadException{}
	public static void validate(ArrayList<String> row_data, int limit, boolean setNote, int index) throws AuthorityFileUploadException{
		
		// n-1:註解, n-2:權威詞, n-3 ~ 0: 階層
		int n = row_data.size();
		if(n != limit){			
			throw new AuthorityFileUploadException("第" + index + "筆資料欄位數量錯誤");
		} else if(setNote && row_data.get(n-2).equals("")){
			throw new AuthorityFileUploadException("權威詞欄位不能是空的");
		} else if(!setNote && row_data.get(n-1).equals("")){
			throw new AuthorityFileUploadException("權威詞欄位不能是空的");
		}
		
		// 無階層
		if(n - 3 > 0){
			boolean hasValue = !row_data.get(n-3).equals("");
			for(int i = n - 4; i >= 0; --i){
				if(hasValue && row_data.get(i).equals("")){
					throw new AuthorityFileUploadException("第" + index + "筆資料階層格式錯誤");
				}
			}
		}
		
	}

	
	public static void main(String[] args) {
		
		String filepath = "data/";
		
		
		
		String title = "本草綱目";
		
//		String filename = "藥名.txt";		
//		String filename = "本草綱目-note.csv";
//		String filename = "本草綱目.tsv";
		String filename = "本草綱目.xlsx";
		
		boolean setHeaders = true;
		boolean setNote = true;
		

		try {
//			JSONArray arr = txtToSolrDoc(filepath, filename, title, "0", setNote);
//			JSONArray arr = csvToSolrDoc(filepath, filename, title, "0", setNote);
//			JSONArray arr = tsvToSolrDoc(filepath, filename, title, "0", setNote);
			JSONArray arr = xlsxToSolrDoc(filepath, filename, title, "0", setNote);
			
//			JSONObject obj = txtToJstreeJSON(filepath, filename, title);
//			JSONObject obj = csvToJstreeJSON(filepath, filename, title);
//			JSONObject obj = tsvToJstreeJSON(filepath, filename, title);
//			JSONObject obj = xlsxToJstreeJSON(filepath, filename, title);
			
			System.out.println(arr);
//			System.out.println(obj);
		} catch (AuthorityFileUploadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
			
		


		


	    
	}

}
