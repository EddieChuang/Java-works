package ascdc.sinica.dhtext.tool.solr;


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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.json.*;

import com.mashape.unirest.http.Headers;

import ascdc.sinica.dhtext.tool.tree.JstreeNode;
import ascdc.sinica.dhtext.util.io.JSONOperate;
import ascdc.sinica.dhtext.util.sort.KeywordLengthSort;


public class UploadAuthority {
	
	private String solrSpecialChar = "+-&|!{}[]^\"~*?:/\\]"; 
	private String synonymSeparatar = "=";
	
	//上傳檔案格式錯誤時，throw authorityFileUploadException
	AuthorityFileUploadException authorityFileUploadException = null;
	
	public UploadAuthority(){
		authorityFileUploadException = new AuthorityFileUploadException();
	}
	
	/**
	  * ex: ['水部', '天水類', '雨水'] -> {"type":"subDir", "text":"水部", "children":[ {"type":"subDir", "text":"天水類", "children":[...]} ]}
	  * @param   
	  */
//	@SuppressWarnings("deprecation")
//	private static JSONObject listToJstreeJSON(ArrayList<String> list, ArrayList<String> headers, int i, boolean setNote){
//		
////		int x = setNote ? 2 : 1;
//		if(i < list.size()){
//			String data = list.get(i);
//			if(!data.equals("")){
//				try {
//					JSONObject jsonObject = new JSONObject();
//					JSONArray children = new JSONArray();
//					JSONObject child = listToJstreeJSON(list, headers, i + 1, setNote);
//					if(child != null)
//						children.put(child);
//					jsonObject.put("type", "subDir");
//					jsonObject.put("children", children);
//					jsonObject.put("text", data.trim());
//					return jsonObject;
////					}
//				} catch (JSONException e) {
//					e.printStackTrace();
//				}
//			} else{
//				return listToJstreeJSON(list, headers, i+1, setNote);
//			}
//		}
//		return null;
//	}
	
	public static JSONObject toJstreeJSON(String title, ArrayList<String> header, ArrayList<JSONObject> data, boolean setNote){
		
//		JSONObject jsonObj = new JSONObject();
//		JSONArray  jsonArr = new JSONArray();
		
		JSONObject init_data   = new JSONObject();
		init_data.put("remove", false);
		init_data.put("rename", false);
		
		JstreeNode jstree = new JstreeNode(title, "default");  // chiamin add
		jstree.setData(init_data); // 根節點不能刪除和更名
		
		//default 自訂標記
		if(header.size()==0)
			header.add("自訂標記");
		ArrayList<String> rowDefault = new ArrayList<String>();
		rowDefault.add("自訂標記");
		if(header.size()>0){
			for (int i = 1; i < header.size(); i++) {
				rowDefault.add("");
			}
		}
		
		JstreeNode defaultNode = JstreeNode.toJstreeNode(rowDefault, 0);
		defaultNode.setData(init_data); // 自訂標記不能刪除和更名
		jstree.append(defaultNode);
		
		for(JSONObject node : data){
			jstree.append(node); // 新增節點
		}
		
		
//		JSONObject jsonRowDefault = listToJstreeJSON(rowDefault, headers, 0,setNote);
//		jsonArr = JSONOperate.appendJstree(jsonArr, jsonRowDefault);
		
		//end default
		
		/*
		 row = ['水部', '天水類', '雨水']
		 jstree 每個節點的格式 {"type":"subDir", "text":"水部", "children":[]} 
	    */	
		
		
//		for(ArrayList<String> row : data){
//			JSONObject jsonRow = listToJstreeJSON(row, headers, 0, setNote);
//			jsonArr = JSONOperate.appendJstree(jsonArr, jsonRow);			
//		}
		
//		jsonObj.put("cat", "目錄");
//		jsonObj.put("type", "default");
//		jsonObj.put("text", title.trim());
//		jsonObj.put("children", jsonArr); 
		
//		System.out.println(jstree.toJSON());
		return jstree.toJSON();
	}
	
	/**
	  * 解析同一個欄位的多個權威詞
	  * @param   
	  */
//	private ArrayList<ArrayList<String>> parseMultikeyword(ArrayList<String> row_data, boolean setNote){
//		
//		String sep = this.multikeywordSeparatar;
//		ArrayList<ArrayList<String>> new_data = new ArrayList<ArrayList<String>>();
//		int indexOfKeyword = row_data.size() - (setNote ? 2 : 1); // keyword的位置。如果setNote==true，權威詞在倒數第二個位置。setNote==false，則是在最後一個位置
//		String[] keyowrds = row_data.get(indexOfKeyword).split(sep);
//
//		for(int i = 0; i < keyowrds.length; ++i){
//			row_data.set(indexOfKeyword, keyowrds[i]);
//			new_data.add(new ArrayList<String>(row_data));
//		}
//		return new_data;
//	}
	
	/**
	 * 讀 text file，包含 txt, csv, tsv
	 * @param filePath 權威檔路徑   ex: "data/csv/"
	 * @param fileName 權威檔名        ex: "本草綱目.csv"
	 * @param sep      txt->"\n"  csv->","  tsv->"\t"
	 */
	private ArrayList<JSONObject> readTextUploadFile(String filePath, String fileName, String sep, boolean setNote) throws AuthorityFileUploadException{

		FileInputStream fileInputStream = null;
		Scanner scanner = null;
		ArrayList<JSONObject> data = new ArrayList<JSONObject>();
		boolean isTxt  = sep.equals("\n");
		
		try {
			fileInputStream = new FileInputStream(filePath + fileName);
			scanner = new Scanner(fileInputStream, "UTF-8");
			ArrayList<String> header = null;
			int limit = 1, num = 1; // 第 num 行
			
			if(!isTxt){ // .txt不需要驗證資料格式
				header = new ArrayList<String>(Arrays.asList(scanner.nextLine().split(sep)));
				this.authorityFileUploadException.setHeader(header);
				validateHeader(header, setNote); // 驗證 header 格式
				limit = header.size(); // 以 header 的欄位數為基準
			}
			
			System.out.println("header: " + header);
			// 逐行讀取資料
			while(scanner.hasNextLine()){
				String[] texts = scanner.nextLine().split(sep, limit);
				JSONObject json = UploadAuthority.convertToJSONObject(new ArrayList<String>(Arrays.asList(texts)), header);
				if(isTxt){ // .txt不需要驗證資料格式，也不會有多個關鍵字在同一欄位
					data.add(json);
				} else{
					if(validate2(json, num)){
						ArrayList<JSONObject> synonym = parseSynonym(json, header, setNote);
//						System.out.println("synonym: " + synonym);
						data.addAll(synonym);
					}
					++num;
				}
			}
			fileInputStream.close();
			scanner.close();
		} catch(IOException e){
			e.printStackTrace();
		}
		
		// 如果有不符合格式的資料，throw exception
		if(this.authorityFileUploadException.getHasException()){
			throw this.authorityFileUploadException;
		}
		
		return data; 
	}

	public static ArrayList<String> XSSFRowToHeader(XSSFRow row){
		
		ArrayList<String> header = new ArrayList<String>();
		int ncol = row.getLastCellNum();
		for(int i = 0; i < ncol; ++i){
			XSSFCell cell = row.getCell(i); // 取得Excel儲存格
			header.add(cell == null ? "" : cell.getStringCellValue().trim()); // 如果儲存格是null，則放空字串
		}

		// 忽略後面欄位的空字串
		for(int i = header.size()-1; i >= 0; --i){
			if(header.get(i).equals("")){
				header.remove(i);
			} else{
				break;
			}
		}
		System.out.println("header: " + header);
		
		return header;
	}
	
	/**
	 * 將 XSSFRow 轉換成 ArrayList<String>，長度為 limit (header.size())，太長截斷，太短補空字串
	 * @param row 
	 * @param limit 
	 * */
	public static ArrayList<String> XSSFRowToArrayList(XSSFRow row, int limit){
		
		ArrayList<String> list = new ArrayList<String>();
//		Iterator<Cell> cells = row.cellIterator();
		
		for(int i = 0; i < limit; ++i){
			XSSFCell cell = row.getCell(i);
			String value = "";
			if(cell != null){
				value = cell.getStringCellValue();
			} 
			list.add(value.trim());
		}
		
		
//		while(cells.hasNext() && limit > 0){
//			XSSFCell cell = (XSSFCell) cells.next();
//			list.add(cell.getStringCellValue().trim());
//			--limit;
//		}
		
		return list;
	}

	/**
	 * 逐行讀取權威檔資料成 ArrayList<ArrayList<String>>，再轉換成 jstree的樹狀目錄，不包含權威詞，只有目錄結構
	 * @param filePath 權威檔路徑   ex: "data/xlsx/"
	 * @param fileName 權威檔名        ex: "本草綱目.xlsx"
	 * @param title    檔威檔標題   ex: "本草綱目"
	 * */
	@SuppressWarnings("deprecation")
	public static JSONObject xlsxToJstreeJSON(String filePath, String fileName, String title, boolean setNote){
		
		ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
		ArrayList<String> header = null;
		try {
			// Excel -> XSSFWorkbook -> XSSFSheet -> XSSFRow -> XSSFCell
			InputStream excelToRead = new FileInputStream(filePath + fileName); // 讀Excel
			XSSFWorkbook workbook = new XSSFWorkbook(excelToRead); // poi 用 XSSFWorkbook 操作 Excel
			XSSFSheet sheet = workbook.getSheetAt(0); // 取得第一個試算表
			Iterator<Row> rows = sheet.rowIterator(); // 取得試算表的 row iterator
			header = UploadAuthority.XSSFRowToHeader((XSSFRow) rows.next()); // 第一行當作 header
			
			HashSet<String> set = new HashSet<String>();
			
			// 讀每一行的資料
			while(rows.hasNext()){
				XSSFRow row = (XSSFRow) rows.next();
				ArrayList<String> list = XSSFRowToArrayList(row, header.size());
				
				// 建立 jstree 只需要 path，所以將權威詞刪掉，如果有註解的話也刪掉‧
				list.remove(list.size() - 1); 
				if(setNote){
					list.remove(list.size() - 1);
				}
				
				// 過濾重複的路徑
				if(!set.contains(list.toString())){
					set.add(list.toString());
					data.add(list);
				}
			}
			
			// 按照 path asc 排序，以符合取出 solr document 的順序
			Collections.sort(data, pathComparator);
			workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// 轉換成 jstree json 的格式，讓前端jstree library可以直接讀取
		return toJstreeJSON(title, header, data, setNote);
	}
	
	/**
	 * txt的權威檔沒有階層
	 * @param title    檔威檔標題   ex: "本草綱目"
	 * */
	public static JSONObject txtToJstreeJSON(String title, boolean setNote){
	
		return toJstreeJSON(title, new ArrayList<String>(),new ArrayList<ArrayList<String>>() , setNote);
	}
	
	/**
	 * 逐行讀取權威檔資料成 ArrayList<ArrayList<String>>，再轉換成 jstree的樹狀目錄，不包含權威詞，只有目錄結構
	 * @param filePath 權威檔路徑   ex: "data/csv/"
	 * @param fileName 權威檔名        ex: "本草綱目.csv"
	 * @param title    檔威檔標題   ex: "本草綱目"
	 * */
	public JSONObject csvToJstreeJSON(String filePath, String fileName, String title, boolean setNote){
		
		String sep = ",";
		FileInputStream fileInputStream = null;
		Scanner scanner = null;
		ArrayList<JSONObject> data = new ArrayList<JSONObject>();
		ArrayList<String> header = null;
		try {
			fileInputStream = new FileInputStream(filePath + fileName);
			scanner = new Scanner(fileInputStream, "UTF-8");
			
			// 第一行當作 header，以 header 的欄位數為基準
			header = new ArrayList<String>(Arrays.asList(scanner.nextLine().split(sep)));
			int col = header.size();
			while(scanner.hasNextLine()){
				
				// "a,b,c,d".split(sep, col)
				// 如果該筆資料的欄位數小於 col不影響結果。col=5, ["a", "b", "c", "d"]
				// 如果該筆資料的欄位數大於 col會截斷字串。col=3, ["a", "b", "c,d"]
				ArrayList<String> list = new ArrayList<String>(Arrays.asList(scanner.nextLine().split(sep, col)));
				list.remove(list.size()-1);
				if(setNote){
					list.remove(list.size()-1);
				}
				ArrayList<JSONObject> synonym = parseSynonym(convertToJSONObject(list, header), header, setNote);
				System.out.println("synonym: " + synonym);
				data.addAll(synonym);
//				data.add(list);
			}
			Collections.sort(data, pathComparator);
			fileInputStream.close();
			scanner.close();
		} catch(IOException e){
			e.printStackTrace();
		} 
		
		
		return toJstreeJSON(title, header, data, setNote);
	}
	
	public static Comparator<ArrayList<String>> pathComparator = new Comparator<ArrayList<String>>() {

		public int compare(ArrayList<String> arr1, ArrayList<String> arr2) {
		   String path1 = arr1.toString().replaceAll("[\\[\\] ]", "").replace(",", "/");
		   String path2 = arr2.toString().replaceAll("[\\[\\] ]", "").replace(",", "/");

		   //ascending order
		   return path1.compareTo(path2);
	    }
	};
	
	/**
	 * 逐行讀取權威檔資料成 ArrayList<ArrayList<String>>，再轉換成 jstree的樹狀目錄，不包含權威詞，只有目錄結構
	 * @param filePath 權威檔路徑   ex: "data/tsv/"
	 * @param fileName 權威檔名        ex: "本草綱目.tsv"
	 * @param title    檔威檔標題   ex: "本草綱目"
	 * */
	public JSONObject tsvToJstreeJSON(String filePath, String fileName, String title, boolean setNote){
		
		String sep = "\t";
		FileInputStream fileInputStream = null;
		Scanner scanner = null;
		ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
		ArrayList<String> headers = null;
		try {
			fileInputStream = new FileInputStream(filePath + fileName);
			scanner = new Scanner(fileInputStream, "UTF-8");
			
			// 第一行當作 header，以 header 的欄位數為基準
			headers = new ArrayList<String>(Arrays.asList(scanner.nextLine().split(sep)));
			int col = headers.size();
			while(scanner.hasNextLine()){
				
				// "a,b,c,d".split(sep, col)
				// 如果該筆資料的欄位數小於 col不影響結果。col=5, ["a", "b", "c", "d"]
				// 如果該筆資料的欄位數大於 col會截斷字串。col=3, ["a", "b", "c,d"]
				ArrayList<String> list = new ArrayList<String>(Arrays.asList(scanner.nextLine().split(sep, col)));		
				list.remove(list.size()-1);
				if(setNote){
					list.remove(list.size()-1);
				}
				data.add(list);
			}
			
			Collections.sort(data, pathComparator);
			fileInputStream.close();
			scanner.close();
		
		} catch(IOException e){
			e.printStackTrace();
		} 
		return toJstreeJSON(title, headers, data, setNote);
	}
	
	
	public static JSONArray toSolrDoc(String filePath, String fileName, String title, String authorityId, ArrayList<ArrayList<String>> data, boolean setNote){
		

		JSONArray arr = new JSONArray();
		
		HashSet<String> keyword_set = new HashSet<String>(); // 相同路徑重複的關鍵字
		HashMap<String, String> loc = new HashMap<String, String>(); // 權威詞讀入的順序
		for(ArrayList<String> row : data){
							
			// get path. ['水部', '地水類', '流水'] -> 本草綱目/水部/天水類/雨水/
			String path = title + "/";
			int i, x = setNote ? 2 : 1;
			for(i = 0; i < row.size()-x; ++i){
				if(!row.get(i).equals(""))
					path += row.get(i) + "/";
			}
			
			

			// 過濾同路徑相同的權威詞
			// 不重複: 本草綱目/水部/天水類/雨水/雨水 & 本草綱目/水部/天水類/立春雨水/雨水
			String keyword = row.get(i++);
			if(keyword_set.contains(path + keyword)){
				System.out.println("duplicate: " + row);
			}
			
			// 如果沒設定註解 i==row.size()，設為"無"
			// 如果有設定註解，直接給值，但是空字串的話，設為"無"
			String note = i==row.size() ? "無" : (row.get(i).equals("") ? "無" : row.get(i)); 
			
			System.out.println(path + keyword);
			// 過濾空字串和重複的關鍵字
			if(!keyword.equals("") && !keyword_set.contains(path + keyword)){
				if(!loc.containsKey(keyword)){
					loc.put(keyword, String.valueOf(loc.size()));
				}
				JSONObject json = new JSONObject();  // solr document的型式
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
	
	public JSONArray csvToSolrDoc(String filePath, String fileName, String title, String authorityId, boolean setNote)throws AuthorityFileUploadException{
		
		String sep = ",";
		ArrayList<JSONObject> data = readTextUploadFile(filePath, fileName, sep, setNote);
		return toSolrDoc2(title, authorityId, data);
	}
	
	public JSONArray tsvToSolrDoc(String filePath, String fileName, String title, String authorityId, boolean setNote) throws AuthorityFileUploadException{
		
		String sep = "\t";
		ArrayList<JSONObject> data = readTextUploadFile(filePath, fileName, sep, setNote);
		return toSolrDoc2(title, authorityId, data);
	}
	
	public JSONArray txtToSolrDoc(String filePath, String fileName, String title, String authorityId, boolean setNote) throws AuthorityFileUploadException{
	
		String sep = "\n";
		ArrayList<JSONObject> data = readTextUploadFile(filePath, fileName, sep, setNote);
		return toSolrDoc2(title, authorityId, data);
	}
	
	
	
//	@SuppressWarnings("deprecation")
//	public JSONArray xlsxToSolrDoc(String filePath, String fileName, String title, String authorityId, boolean setNote) throws AuthorityFileUploadException{
//		
//		ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
//		
//		try {
//			// Excel -> XSSFWorkbook -> XSSFSheet -> XSSFRow -> XSSFCell
//			InputStream excelToRead = new FileInputStream(filePath + fileName);	// 讀Excel		
//			XSSFWorkbook workbook = new XSSFWorkbook(excelToRead); // poi 用 XSSFWorkbook 操作 Excel
//			XSSFSheet sheet = workbook.getSheetAt(0); // 取得第一個試算表			
//			Iterator<Row> rows = sheet.rowIterator(); // 取得試算表的 row iterator
//			
//			ArrayList<String> header = UploadAuthority.XSSFRowToHeader((XSSFRow) rows.next()); // 第一行當作 header
//			this.authorityFileUploadException.setHeader(header);
//			validateHeader(header, setNote); // 驗證 header格式
//			
//			int limit = header.size(), num = 1; 
//			while(rows.hasNext()){
//				
//				XSSFRow row = (XSSFRow) rows.next();
//				if(!isEmptyXSSFRow(row)){
//					ArrayList<String> row_data = UploadAuthority.XSSFRowToArrayList(row, limit);
//					// 驗證 row data格式
//					if(validate(row_data, setNote, num)){
//						ArrayList<ArrayList<String>> multikeyword = parseMultikeyword(row_data, setNote); // 解析同一欄位多個關鍵字
//						for(int i = 0; i < multikeyword.size(); ++i){
//							data.add(multikeyword.get(i));
//						}
//					}
//					++num;
//				}
//			}
//			workbook.close();
//		}  catch (IOException | JSONException e) {
//			e.printStackTrace();
//		}
//
//		if(this.authorityFileUploadException.getHasException()){
//			throw this.authorityFileUploadException;
//		}
//		return toSolrDoc(filePath, fileName, title, authorityId, data, setNote);
//	}
	
	/**
	 * 逐行讀取權威檔資料成 ArrayList<ArrayList<String>>，再轉換成 solr document型式的 JSONArray
	 * @param filePath       權威檔路徑   ex: "data/xlsx/"
	 * @param fileName       權威檔名        ex: "本草綱目.xlsx"
	 * @param title          檔威檔標題   ex: "本草綱目"
	 * @param authorityId    檔威檔id   ex: "330"
	 * @param setNote        是否有註解   ex: true
	 * */
	public JSONArray xlsxToSolrDoc(String filePath, String fileName, String title, String authorityId, boolean setNote) throws AuthorityFileUploadException{
		
		ArrayList<JSONObject> data = new ArrayList<JSONObject>();
		ArrayList<String> header = null;
		try {
			// Excel -> XSSFWorkbook -> XSSFSheet -> XSSFRow -> XSSFCell
			InputStream excelToRead = new FileInputStream(filePath + fileName);	// 讀Excel		
			XSSFWorkbook workbook = new XSSFWorkbook(excelToRead); // poi 用 XSSFWorkbook 操作 Excel
			XSSFSheet sheet = workbook.getSheetAt(0); // 取得第一個試算表			
			Iterator<Row> rows = sheet.rowIterator(); // 取得試算表的 row iterator
			
			header = UploadAuthority.XSSFRowToHeader((XSSFRow) rows.next()); // 第一行當作 header
			this.authorityFileUploadException.setHeader(header);
			validateHeader(header, setNote); // 驗證 header格式
			
			int limit = header.size();
			int num = 1; 
			while(rows.hasNext()){
				
				XSSFRow row = (XSSFRow) rows.next();
				if(!isEmptyXSSFRow(row)){
					JSONObject json_data = UploadAuthority.XSSFRowToJSONObject(row, header);
					// 驗證 row data格式
					if(validate2(json_data, num)){
						ArrayList<JSONObject> synonym = parseSynonym(json_data, header, setNote);
						System.out.println("synonym: " + synonym);
						data.addAll(synonym);
					}
					++num;
				}
				
			}
			workbook.close();
		}  catch (IOException | JSONException e) {
			e.printStackTrace();
		}

		if(this.authorityFileUploadException.getHasException()){
			throw this.authorityFileUploadException;
		}
		return toSolrDoc2(title, authorityId, data);
	}
	
	public JSONArray toSolrDoc2(String title, String authorityId, ArrayList<JSONObject> data){
		

		JSONArray arr = new JSONArray();
		ArrayList<String> header = this.authorityFileUploadException.getHeader();
		HashSet<String> keyword_set = new HashSet<String>(); // 相同路徑重複的關鍵字
		HashMap<String, String> loc = new HashMap<String, String>(); // 權威詞讀入的順序
		for(JSONObject json : data){
							
			String keyword = json.getString("名");
			String note    = json.getString("註解").equals("") ? "無" : json.getString("註解");
			// get path. ['水部', '地水類', '流水'] -> 本草綱目/水部/天水類/雨水/
			String path = title + "/";
			for(String key : header){
				String value = json.getString(key);
				if(!value.equals("") && !key.equals("名") && !key.equals("註解")){
					path += value + "/";
				}
			}
			if(json.has("extraPath")){
				path += json.getString("extraPath") + "/";
			}
			
			

			// 過濾同路徑相同的權威詞
			// 不重複: 本草綱目/水部/天水類/雨水/雨水 & 本草綱目/水部/天水類/立春雨水/雨水
			if(keyword_set.contains(path + keyword)){
				System.out.println("duplicate: " + json.toString());
			}
			
//			System.out.println(path + keyword);
			// 過濾空字串和重複的關鍵字
			if(!keyword.equals("") && !keyword_set.contains(path + keyword)){
				if(!loc.containsKey(keyword)){
					loc.put(keyword, String.valueOf(loc.size()));
				}
				JSONObject jsonObject = new JSONObject();  // solr document的型式
				jsonObject.put("authorityId", authorityId);
				jsonObject.put("loc", loc.get(keyword));
				jsonObject.put("path", path);
				jsonObject.put("text", keyword.trim());
				jsonObject.put("note", note);
				jsonObject.put("hidden", "false");
				
				arr.put(jsonObject);
				keyword_set.add(path + keyword);
			}
		}

		return arr;
	}
	
	public static JSONObject XSSFRowToJSONObject(XSSFRow row, ArrayList<String> header){
		
		JSONObject json = new JSONObject();
		
		for(int i = 0; i < header.size(); ++i){
			XSSFCell cell = row.getCell(i);
			String value = "";
			if(cell != null){
				value = cell.getStringCellValue();
			} 
			json.put(header.get(i), value.trim());
		}
		json.put("extraPath", "");
		
		return json;
	}
	
	public static JSONObject convertToJSONObject(ArrayList<String> arr, ArrayList<String> header){
		
		JSONObject json = new JSONObject();
		for(int i = 0; i < header.size() && i < arr.size(); ++i){
			json.put(header.get(i), arr.get(i));
		}
//		System.out.println(json);
		json.put("extraPath", "");
		return json;
	}

	
	private boolean isEmptyXSSFRow(XSSFRow row) {
	    
		if (row == null) {
	        return true;
	    }
	    for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); ++cellNum) {
	        XSSFCell cell = row.getCell(cellNum);
	        if (cell != null && cell.getCellTypeEnum() != CellType.BLANK && StringUtils.isNotBlank(cell.toString())) {
	            return false;
	        }
	    }
	    return true;
	}
//	
	
	
	/**
	 * 解析同義詞
	 * */
	public ArrayList<JSONObject> parseSynonym(JSONObject json_data, ArrayList<String> header, boolean setNote){
		
		ArrayList<ArrayList<String>> synonymPath  = new ArrayList<ArrayList<String>>();
		for(String key : header){
			if(key.equals("名")){
				break;
			}
			ArrayList<String> arr = new ArrayList<String>();
			String str = json_data.getString(key).replace("(", this.synonymSeparatar).replace(")", "");
			arr.addAll(new ArrayList<String>(Arrays.asList(str.split(this.synonymSeparatar))));
			synonymPath.add(arr);
		}
		
//		System.out.println(synonymPath);
		
		ArrayList<JSONObject> res = buildSynonymPath(synonymPath, new JSONObject(), header, 0);
		
		String keyword = json_data.getString("名").replace("(", this.synonymSeparatar).replace(")"	, "");
		String note    = json_data.has("註解") ? json_data.getString("註解") : "無";
		ArrayList<String> multiKeyword = new ArrayList<String>(Arrays.asList(keyword.split(this.synonymSeparatar)));
		ArrayList<JSONObject> synonym = new ArrayList<JSONObject>();
		for(int i = 0; i < res.size(); ++i){
			
			for(int j = 0; j < multiKeyword.size(); ++j){
				JSONObject json = copyFrom(res.get(i));
				if(multiKeyword.size() > 1){
					json.put("extraPath", multiKeyword.get(0));
				}
				json.put("名", multiKeyword.get(j));
				json.put("註解", note);
				synonym.add(json);
			}
		}
		return synonym;
	}
	
	/**
	 * 產生同義的資料
	 * */
	private ArrayList<JSONObject> buildSynonymPath(ArrayList<ArrayList<String>> synonym, JSONObject json, ArrayList<String> header, int i){
		
		if(synonym.size() == i){
			return null;
		}

		ArrayList<JSONObject> new_data = new ArrayList<JSONObject>();
		ArrayList<String> syn = synonym.get(i);
		for(int j = 0; j < syn.size(); ++j){
			JSONObject new_json = copyFrom(json);
			new_json.put(header.get(i), syn.get(j));
			ArrayList<JSONObject> res = buildSynonymPath(synonym, new_json, header, i+1);
			if(res != null){
				new_data.addAll(res);
			} else{
				new_data.add(new_json);
			}
		}
		return new_data;
	}
	
	private static JSONObject copyFrom(JSONObject json){
		
		JSONObject copy = new JSONObject();
		for(String key : json.keySet()){
			copy.put(key, json.getString(key));
		}
		return copy;
	}
	
	
	/**
	 * 驗證標頭欄位是否正確，若錯誤的話，直接丟出例外情況並中止程式。
	 * 權威檔內容的資料格式是以標頭為基準
	 * @param header  標頭欄位 ex: [部, 類, 種, 名, 註解], [部, 類, 種, 名]
	 * @param setNote 是否設定註解
	 * */
	public void validateHeader(ArrayList<String> header, boolean setNote) throws AuthorityFileUploadException{
		
		
//		Map<String, String> header_json = new LinkedHashMap<String, String>();
		JSONObject header_json = new JSONObject();
		int x = setNote ? 2 : 1;
		int i;
		
		// 取出資料
		for(i = 0; i < header.size() - x; ++i)
			header_json.put("第" + (i+1) + "層", header.get(i).equals("") ? "\" \"" : header.get(i));
		header_json.put("名", header.get(i++).equals("") ? "\" \"" : header.get(i-1));
		
		// 檢查「名」
		if(!header.get(i-1).equals("名")){
			this.authorityFileUploadException.appendErrorMessage("標頭缺少「名」欄位"); // 記錄錯誤訊息
			this.authorityFileUploadException.appendErrorData(header_json); // 記錄錯誤資料
			this.authorityFileUploadException.setHasException(true);
			throw this.authorityFileUploadException;
		}
		
		// 檢查「註解」
		if(setNote){
			if(i < header.size()){
				header_json.put("註解", header.get(i).equals("") ? "\" \"" : header.get(i));
			} else{
				this.authorityFileUploadException.appendErrorMessage("標頭缺少「註解」欄位"); // 記錄錯誤訊息
				this.authorityFileUploadException.appendErrorData(header_json); // 記錄錯誤資料
				this.authorityFileUploadException.setHasException(true);
				throw this.authorityFileUploadException;
			}
		} 
		
		// 檢查「欄位名稱」
		for(String s : header){
			if(s.equals("")){
				this.authorityFileUploadException.appendErrorMessage("欄位名稱不可空白"); // 記錄錯誤訊息
				this.authorityFileUploadException.appendErrorData(header_json); // 記錄錯誤資料
				this.authorityFileUploadException.setHasException(true);
				throw this.authorityFileUploadException;
			}
		}
		if(hasSolrSpecialChar(header)){
			this.authorityFileUploadException.appendErrorMessage("欄位不可包含特殊符號"); // 記錄錯誤訊息
			this.authorityFileUploadException.appendErrorData(header_json); // 記錄錯誤資料
			this.authorityFileUploadException.setHasException(true);
			throw this.authorityFileUploadException;
		}
	}
	
	/**
	 * 驗證每一行資料格式是否正確，若錯誤的話，紀錄該筆資料，最後一次顯示所有錯誤的資料
	 * @param row_data  標頭欄位 ex: [部, 類, 種, 名, 註解], [部, 類, 種, 名]
	 * @param setNote 是否設定註解
	 * @param index   第幾筆資料
	 * */
	public boolean validate2(JSONObject json, int index) {
		
		ArrayList<String> header = this.authorityFileUploadException.getHeader();
		boolean isValidate = true;
		int n = json.length() - 1; // for extraPath
		int m = header.size();
		
		if(n != m){ // +1 for extraPath
			this.authorityFileUploadException.appendErrorMessage("第" + index + "筆資料：欄位數量錯誤"); // 記錄錯誤訊息
			this.authorityFileUploadException.appendErrorData(json); // 記錄錯誤資料
			this.authorityFileUploadException.setHasException(true);
			isValidate = false;
		}
		if(!json.has("名")){ // 有註解，倒數第二個是權威詞。無註解，最後一個是權威詞
			this.authorityFileUploadException.appendErrorMessage("第" + index + "筆資料：權威詞不可空白"); // 記錄錯誤訊息
			this.authorityFileUploadException.appendErrorData(json); // 記錄錯誤資料
			this.authorityFileUploadException.setHasException(true);
			isValidate = false;
		}
		
		boolean setNote = header.indexOf("註解") != -1;
		int offset = setNote ? 3 : 2;
		if(n > offset){
		
			String key = header.get(n-offset);
			boolean hasValue = !json.getString(key).equals("");
			for(int j = n - offset - 1; j >= 0; --j){
				key = header.get(j);
				if(hasValue && json.getString(key).equals("")){
					this.authorityFileUploadException.appendErrorMessage("第" + index + "筆資料：階層格式錯誤"); // 記錄錯誤訊息
					this.authorityFileUploadException.appendErrorData(json); // 記錄錯誤資料
					this.authorityFileUploadException.setHasException(true);
					isValidate = false;
				}
			}
		}
		
		if(hasSolrSpecialChar(json)){
			this.authorityFileUploadException.appendErrorMessage("第" + index + "筆資料：不可包含特殊符號"); // 記錄錯誤訊息
			this.authorityFileUploadException.appendErrorData(json); // 記錄錯誤資料
			this.authorityFileUploadException.setHasException(true);
			isValidate = false;
		}
		if(!validSynonym(json)){
			this.authorityFileUploadException.appendErrorMessage("第" + index + "筆資料：同義詞格式錯誤"); // 記錄錯誤訊息
			this.authorityFileUploadException.appendErrorData(json); // 記錄錯誤資料
			this.authorityFileUploadException.setHasException(true);
			isValidate = false;
		}
		if(!validParenthesis(json)){
			this.authorityFileUploadException.appendErrorMessage("第" + index + "筆資料：括號必須成對"); // 記錄錯誤訊息
			this.authorityFileUploadException.appendErrorData(json); // 記錄錯誤資料
			this.authorityFileUploadException.setHasException(true);
			isValidate = false;
		}
				
		return isValidate;
	}
	
	public boolean hasSolrSpecialChar(ArrayList<String> row_data){
		
		for(String data : row_data){
			for(int i = 0; i < solrSpecialChar.length(); ++i){
				if(data.indexOf(solrSpecialChar.charAt(i)) != -1){
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean hasSolrSpecialChar(JSONObject json){
		
		for(String key : json.keySet()){
			if(key.equals("註解")){
				continue;
			}
			for(int i = 0; i < solrSpecialChar.length(); ++i){
				String value = json.getString(key);
				if(value.indexOf(solrSpecialChar.charAt(i)) != -1){
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * 以( )表示一個同義詞，( )必須成對。
	 * */
	public boolean validParenthesis(JSONObject json){
		
		Pattern leftParen  = Pattern.compile("[(]");
		Pattern rightParen = Pattern.compile("[)]");
		for(String key : json.keySet()){
			if(key.equals("註解")){
				continue;
			}
			String value = json.getString(key);
			Matcher leftParenMatcher = leftParen.matcher(value);
			Matcher rightParenMatcher = rightParen.matcher(value);
			int lCount = 0, rCount = 0;
			while(leftParenMatcher.find()){
				++lCount;
			}
			while(rightParenMatcher.find()){
				++rCount;
			}
			
			// 成對
			if(lCount != rCount){
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * 只能包含一種同義詞格式。
	 * 以逗號分隔多個同義詞。 ex: 台灣,臺灣,中華民國
	 * 以( )表示一個同義詞，不可多個。 ex: 台灣(臺灣)。 error: 台灣(臺灣)(中華民國)
	 * */
	public boolean validSynonym(JSONObject json){
				
		Pattern paren = Pattern.compile("[(][.]*[)]");
		for(String key : json.keySet()){
			if(key.equals("註解")){
				continue;
			}
			String value = json.getString(key);
			
			// 不能同時出現 , ()
			if(value.contains(this.synonymSeparatar) && (value.contains("(") || value.contains(")"))){
				return false;
			}
			
			// 不同有多對 ()
			Matcher parenMatcher = paren.matcher(value);
			for(int i = 0; parenMatcher.find(); ++i){
				if(i > 0){
					return false;
				}
			}
		}
		return true;
	}
	
	public static void main(String[] args) {
		
		String filePath = "data/csv/";		
		String title = "本草綱目";
		
//		String fileName = "藥名.txt";		
		String fileName = "本草綱目.csv";
//		String fileName = "本草綱目.xlsx";
//		String fileName = "太平廣記卷96權威詞.xlsx";
//		String fileName = "太平廣記卷96權威詞-synonym.xlsx";
		String authorityId = "0";
		boolean setNote = true;
		
		UploadAuthority uploadAuthority = new UploadAuthority();
		try {
			long start = new Date().getTime();
//			JSONArray arr = uploadAuthority.txtToSolrDoc(filePath, fileName, title, authorityId, setNote);
			JSONArray arr = uploadAuthority.csvToSolrDoc(filePath, fileName, title, authorityId, setNote);
//			JSONArray arr = uploadAuthority.tsvToSolrDoc(filePath, fileName, title, authorityId, setNote);
			
//			JSONArray arr = uploadAuthority.xlsxToSolrDoc(filePath, fileName, title, authorityId, setNote);
//			long time = new Date().getTime();
			
//			JSONObject obj = uploadAuthority.txtToJstreeJSON(title);
			JSONObject obj = uploadAuthority.csvToJstreeJSON(filePath, fileName, title, setNote);
//			JSONObject obj = uploadAuthority.tsvToJstreeJSON(filePath, fileName, title);
			
//			JSONObject obj = uploadAuthority.xlsxToJstreeJSON(filePath, fileName, title, setNote);
//			long end = new Date().getTime();
//			System.out.println(end - time);
			System.out.println(arr);
			System.out.println(obj);
			
		} catch (AuthorityFileUploadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			String message = e.getMessage();
			JSONArray error_data = e.getErrorData();
			ArrayList<String> error_msg = e.getErrorMessage();
			for(int i = 0; i < error_msg.size(); ++i){
				System.out.println(error_msg.get(i));
				System.out.println(error_data.get(i));
				System.out.println("");
			}
		}

			 
	}

}
