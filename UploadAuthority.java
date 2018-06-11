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

import com.mashape.unirest.http.Headers;


public class UploadAuthority {
	
	AuthorityFileUploadException authorityFileUploadException = null;

	
	public UploadAuthority(){
		authorityFileUploadException = new AuthorityFileUploadException();

	}
	
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
						jsonObject.put("text", data.trim());
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
		jsonObj.put("text", title.trim());
		jsonObj.put("children", jsonArr); 
		
		return jsonObj;
	}
	

	private ArrayList<ArrayList<String>> parseMultikeyword(ArrayList<String> row_data, boolean setNote){
		
		String sep = "\\|";
		ArrayList<ArrayList<String>> new_data = new ArrayList<ArrayList<String>>();
		int indexOfKeyword = row_data.size() - (setNote ? 2 : 1);
		String[] keyowrds = row_data.get(indexOfKeyword).split(sep);

		for(int i = 0; i < keyowrds.length; ++i){
			row_data.set(indexOfKeyword, keyowrds[i]);
			new_data.add(new ArrayList<String>(row_data));
		}
		
		
		return new_data;
	}
	
	private ArrayList<ArrayList<String>> readTextUploadFile(String filepath, String filename, String sep, boolean setNote) throws AuthorityFileUploadException{

		FileInputStream fileInputStream = null;
		Scanner scanner = null;
		ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
		boolean isTxt = sep.equals("\n");
		
		try {
			fileInputStream = new FileInputStream(filepath + filename);
			scanner = new Scanner(fileInputStream, "UTF-8");
			ArrayList<String> header = null;
			int limit = 1, num = 1;
			
			if(!isTxt){ // .txt不需要驗證資料格式
				header = new ArrayList<String>(Arrays.asList(scanner.nextLine().split(sep)));
				this.authorityFileUploadException.setHeader(header);
				validateHeader(header, setNote);
				limit = header.size();
			}
			
			System.out.println("header: " + header);
			while(scanner.hasNextLine()){
				String[] texts = scanner.nextLine().split(sep, limit);
				ArrayList<String> row_data = new ArrayList<String>(Arrays.asList(texts));
				if(isTxt){ // .txt不需要驗證資料格式，也不會有多個關鍵字在同一欄位
					data.add(row_data);
				} else{
					if(validate(row_data, setNote, num)){
						ArrayList<ArrayList<String>> multikeyword = parseMultikeyword(row_data, setNote); // 解析同一欄位多個關鍵字
						for(int i = 0; i < multikeyword.size(); ++i){
							data.add(multikeyword.get(i));
						}
					}
					++num;
				}
			}
			fileInputStream.close();
			scanner.close();
		} catch(IOException e){
			e.printStackTrace();
		}
		
		if(this.authorityFileUploadException.getHasException()){
			throw this.authorityFileUploadException;
		}
		
		return data; 
	}

	public static ArrayList<String> XSSFRowToHeader(XSSFRow row){
		
		ArrayList<String> header = new ArrayList<String>();
		Iterator<Cell> cells = row.cellIterator();
		int n = row.getLastCellNum();
		for(int i = 0; i < n; ++i){
			XSSFCell cell = row.getCell(i);
			header.add(cell==null ? "" : cell.getStringCellValue().trim());
		}
//		System.out.println(row.getLastCellNum());
//		while(cells.hasNext()){
//			
//			XSSFCell cell = (XSSFCell) cells.next();
////			if(cell.getCellType() == XSSFCell.CELL_TYPE_STRING){
//				header.add(cell.getStringCellValue());
////			}
//			System.out.println("cell: " + cell.getStringCellValue());
//		}
		
		// 忽略後面欄位的空字串
		for(int i = header.size()-1; i >= 0; --i){
			if(header.get(i).equals("")){
				header.remove(i);
			} else{
				break;
			}
		}
		// System.out.println(header);
		
		return header;
	}
	
	public static ArrayList<String> XSSFRowToArrayList(XSSFRow row, int limit){
		
		ArrayList<String> list = new ArrayList<String>();
		Iterator<Cell> cells = row.cellIterator();
		
		while(cells.hasNext() && limit > 0){
			XSSFCell cell = (XSSFCell) cells.next();
			list.add(cell.getStringCellValue().trim());
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
		
		HashSet<String> keyword_set = new HashSet<String>(); // 相同路徑重複的關鍵字
		HashMap<String, String> loc = new HashMap<String, String>(); 
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
			if(keyword_set.contains(path + keyword)){
				System.out.println("duplicate: " + row);
			}
			if(!keyword.equals("") && !keyword_set.contains(path + keyword)){
				if(!loc.containsKey(keyword)){
					loc.put(keyword, String.valueOf(loc.size()));
				}
				JSONObject json = new JSONObject();
				json.put("authorityId", authorityId);
				json.put("loc", loc.get(keyword));
				json.put("path", path);
				json.put("text", keyword.trim());
				json.put("note", note);
				json.put("hidden", "false");
				arr.put(json);
				keyword_set.add(path + keyword);
			}
		}

		return arr;
	}
	
	public JSONArray csvToSolrDoc(String filepath, String filename, String title, String authorityId, boolean setNote)throws AuthorityFileUploadException{
		
		String sep = ",";
		ArrayList<ArrayList<String>> data = readTextUploadFile(filepath, filename, sep, setNote);
		
		return toSolrDoc(filepath, filename, title, authorityId, data);
	}
	
	public JSONArray tsvToSolrDoc(String filepath, String filename, String title, String authorityId, boolean setNote) throws AuthorityFileUploadException{
		
		String sep = "\t";
		ArrayList<ArrayList<String>> data = readTextUploadFile(filepath, filename, sep, setNote);
		
		return toSolrDoc(filepath, filename, title, authorityId, data);
	}
	
	public JSONArray txtToSolrDoc(String filepath, String filename, String title, String authorityId, boolean setNote) throws AuthorityFileUploadException{
	
		String sep = "\n";
		ArrayList<ArrayList<String>> data = readTextUploadFile(filepath, filename, sep, setNote);
		return toSolrDoc(filepath, filename, title, authorityId, data);
	}
	
	@SuppressWarnings("deprecation")
	public JSONArray xlsxToSolrDoc(String filepath, String filename, String title, String authorityId, boolean setNote) throws AuthorityFileUploadException{
		
		ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
		
		try {
			InputStream excelToRead = new FileInputStream(filepath + filename);			
			XSSFWorkbook workbook = new XSSFWorkbook(excelToRead);
			XSSFSheet sheet = workbook.getSheetAt(0);			
			Iterator<Row> rows = sheet.rowIterator();
			
			ArrayList<String> header = UploadAuthority.XSSFRowToHeader((XSSFRow) rows.next());
			this.authorityFileUploadException.setHeader(header);
			validateHeader(header, setNote);
			
			int limit = header.size(), num = 1; 
			while(rows.hasNext()){
				XSSFRow row = (XSSFRow) rows.next();
				ArrayList<String> row_data = UploadAuthority.XSSFRowToArrayList(row, limit);
				if(validate(row_data, setNote, num)){
					ArrayList<ArrayList<String>> multikeyword = parseMultikeyword(row_data, setNote); // 解析同一欄位多個關鍵字
					for(int i = 0; i < multikeyword.size(); ++i){
						data.add(multikeyword.get(i));
					}
				}
				++num;
			}
			workbook.close();
		}  catch (IOException | JSONException e) {
			e.printStackTrace();
		}

		if(this.authorityFileUploadException.getHasException()){
			throw this.authorityFileUploadException;
		}
		return toSolrDoc(filepath, filename, title, authorityId, data);
	}
	
	public void validateHeader(ArrayList<String> header, boolean setNote) throws AuthorityFileUploadException{
		
		Map<String, String> header_json = new LinkedHashMap<String, String>();
		int x = setNote ? 2 : 1;
		int i;
		
		for(i = 0; i < header.size() - x; ++i)
			header_json.put("第" + (i+1) + "層", header.get(i).equals("") ? "\" \"" : header.get(i));
		
		header_json.put("名", header.get(i++).equals("") ? "\" \"" : header.get(i-1));
		if(!header.get(i-1).equals("名")){
			this.authorityFileUploadException.appendErrorMessage("標頭缺少「名」欄位");
			this.authorityFileUploadException.appendErrorData(header_json);
			this.authorityFileUploadException.setHasException(true);
			throw this.authorityFileUploadException;
		}
		
		if(setNote){
			if(i < header.size()){
				header_json.put("註解", header.get(i).equals("") ? "\" \"" : header.get(i));
			} else{
				this.authorityFileUploadException.appendErrorMessage("標頭缺少「註解」欄位");
				this.authorityFileUploadException.appendErrorData(header_json);
				this.authorityFileUploadException.setHasException(true);
				throw this.authorityFileUploadException;
			}
		} 
		
		
		
		for(String s : header){
			if(s.equals("")){
				this.authorityFileUploadException.appendErrorMessage("欄位名稱不可空白");
				this.authorityFileUploadException.appendErrorData(header_json);
				this.authorityFileUploadException.setHasException(true);
				throw this.authorityFileUploadException;
			}
		}
	}
	
	public boolean validate(ArrayList<String> row_data, boolean setNote, int index) {
		
		ArrayList<String> header = this.authorityFileUploadException.getHeader();
		boolean isValidate = true;
		int n = row_data.size();
		int m = header.size();
		int x = setNote ? 2 : 1;
		int i = 0;
		Map<String, String> map = new LinkedHashMap<String, String>();
		while(i < n)
			map.put(header.get(i), row_data.get(i++).equals("") ? "\" \"" : row_data.get(i-1));
		while(i < m) // header.size() > row_data.size()
			map.put(header.get(i++), "");
		
		// System.out.println(row_data);
		if(n != m){
			this.authorityFileUploadException.appendErrorMessage("第" + index + "筆資料：欄位數量錯誤");
			this.authorityFileUploadException.appendErrorData(map);
			this.authorityFileUploadException.setHasException(true);
			isValidate = false;
		} else if(row_data.get(n-x).equals("")){ // 有註解，倒數第二個是權威詞。無註解，最後一個是權威詞
			this.authorityFileUploadException.appendErrorMessage("第" + index + "筆資料：權威詞不可空白");
			this.authorityFileUploadException.appendErrorData(map);
			this.authorityFileUploadException.setHasException(true);
			isValidate = false;
		}
		
		x = setNote ? 3 : 2;
		if(n - x > 0){
			boolean hasValue = !row_data.get(n-x).equals("");
			for(int j = n - x - 1; j >= 0; --j){
				if(hasValue && row_data.get(j).equals("")){
					this.authorityFileUploadException.appendErrorMessage("第" + index + "筆資料：階層格式錯誤");
					this.authorityFileUploadException.appendErrorData(map);
					this.authorityFileUploadException.setHasException(true);
					isValidate = false;
				}
			}
		}
		return isValidate;
		
	}

	
	public static void main(String[] args) {
		
//		String filepath = "data/xlsx/";
//		
//		
//		
//		String title = "本草綱目";
//		
////		String filename = "藥名.txt";		
////		String filename = "本草綱目.csv";
////		String filename = "本草綱目-less-header-nonote.tsv";
//		String filename = "本草綱目.xlsx";
//		
//		boolean setNote = true;
//		
//		UploadAuthority uploadAuthority = new UploadAuthority();
//		try {
////			JSONArray arr = uploadAuthority.txtToSolrDoc(filepath, filename, title, "0", setNote);
////			JSONArray arr = uploadAuthority.csvToSolrDoc(filepath, filename, title, "0", setNote);
////			JSONArray arr = uploadAuthority.tsvToSolrDoc(filepath, filename, title, "0", setNote);
//			JSONArray arr = uploadAuthority.xlsxToSolrDoc(filepath, filename, title, "0", setNote);
//			
////			JSONObject obj = uploadAuthority.txtToJstreeJSON(filepath, filename, title);
////			JSONObject obj = uploadAuthority.csvToJstreeJSON(filepath, filename, title);
////			JSONObject obj = uploadAuthority.tsvToJstreeJSON(filepath, filename, title);
////			JSONObject obj = uploadAuthority.xlsxToJstreeJSON(filepath, filename, title);
//			System.out.println(arr);
////			System.out.println(obj);
//		} catch (AuthorityFileUploadException e) {
//			// TODO Auto-generated catch block
////			e.printStackTrace();
//			String message = e.getMessage();
//			ArrayList<Map<String, String>> error_data = e.getErrorData();
//			ArrayList<String> error_msg = e.getErrorMessage();
//			for(int i = 0; i < error_msg.size(); ++i){
//				System.out.println(error_msg.get(i));
//				System.out.println(error_data.get(i));
//				System.out.println("");
//			}
//		}
		
		Date now = new Date();
		System.out.println(new Date().getTime() - now.getTime());
			 
	}

}
