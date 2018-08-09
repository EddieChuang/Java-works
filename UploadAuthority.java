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
	
//	private String solrSpecialChar = "+-&|!{}[]^\"~*?:/\\]"; 
//	private String solrSpecialChar = "";
	private static String synonymSeparatar = "=";
	
	//上傳檔案格式錯誤時，throw authorityFileUploadException
	private AuthorityFileUploadException authorityFileUploadException = null;
	
	public UploadAuthority(){
		authorityFileUploadException = new AuthorityFileUploadException();
	}
	
	public ArrayList<String> getHeader(){
		return this.authorityFileUploadException.getHeader();
	}
	
	public static boolean isEmptyContent(ArrayList<String> list){
		for(String value : list){
			if(!value.equals("")){
				return false;
			}
		}
		return true;
	}
	
	public static ArrayList<String> toArrayList(JSONObject json, ArrayList<String> header){
	
		ArrayList<String> list = new ArrayList<String>();
		for(String key : header){
			if(json.has(key))
				list.add(json.getString(key));
		}
		if(json.has("extraPath")){
			list.add(json.getString("extraPath"));
		}
//		System.out.println(list);
		return list;
		
	}
	
	public static JSONObject toJstreeJSON(String title, ArrayList<String> header, ArrayList<JSONObject> data, boolean setNote, String userName){
		
		JSONObject init_data   = new JSONObject();
		init_data.put("remove", false);
		init_data.put("rename", false);
		
		
		JstreeNode jstree = new JstreeNode("root", "default");  // chiamin add
		jstree.setData(init_data);  // 根節點不能刪除和更名
		
		ArrayList<String> rowDefault = new ArrayList<String>();
		rowDefault.add("自訂標記");

		//default 自訂標記
		JstreeNode defaultNode = JstreeNode.toJstreeNode(rowDefault, 0);
		defaultNode.setData(init_data); // 自訂標記不能刪除和更名
		
		//自訂標記加名字節點
		JstreeNode child = new JstreeNode(userName, "subDir", false, false);
		ArrayList<JstreeNode> alChildren = new ArrayList<JstreeNode>();
		alChildren.add(child);
		defaultNode.setChildren(alChildren);
		jstree.append(defaultNode);
		
		for(JSONObject node : data){
			jstree.append(toArrayList(node, header)); // 新增節點
		}
	
		return jstree.toJSON();
	}
	
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
			ArrayList<String> header = new ArrayList<String>();
			int limit = 1, num = 1; // 第 num 行
			
			if(!isTxt){ // .txt不需要驗證資料格式
				header = new ArrayList<String>(Arrays.asList(scanner.nextLine().split(sep)));
				int noteIndex = header.indexOf("註解");
				if(noteIndex == -1 && !setNote){
					// 但是檔案沒有註解且沒勾選，標頭補上註解欄位
					header.add("註解");
				}
				this.authorityFileUploadException.setHeader(header);
				validateHeader(header, setNote); // 驗證 header 格式
				limit = header.size(); // 以 header 的欄位數為基準
			} else {
				header.add("名");
				header.add("註解");
			}
			
//			System.out.println("header: " + header);
			// 逐行讀取資料
			while(scanner.hasNextLine()){
				String[] texts = scanner.nextLine().split(sep, limit);
				JSONObject json = UploadAuthority.convertToJSONObject(new ArrayList<String>(Arrays.asList(texts)), header);
				if(isTxt){ // .txt不需要驗證資料格式，也不會有多個關鍵字在同一欄位
					json.put("註解", "");
					data.add(json);
				} else{
					if(validate(json, num)){
						ArrayList<JSONObject> synonym = parseSynonym(json, header);
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
			if(cell == null) {
				header.add("");
			} else if(cell.getCellTypeEnum() == CellType.STRING){
				header.add(cell.getStringCellValue().trim());
			} else if(cell.getCellTypeEnum() == CellType.NUMERIC){
				header.add(String.valueOf(cell.getNumericCellValue()).trim());
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
//		System.out.println("header: " + header);
		
		return header;
	}
	
	
	/**
	 * 將 XSSFRow 轉換成 ArrayList<String>，長度為 limit (header.size())，太長截斷，太短補空字串
	 * @param row 
	 * @param limit 
	 * */
	public static ArrayList<String> XSSFRowToArrayList(XSSFRow row, int limit){
		
		ArrayList<String> list = new ArrayList<String>();
		
		for(int i = 0; i < limit; ++i){
			XSSFCell cell = row.getCell(i);
			String value = "";
			if(cell == null){
				value = "";
			} else if(cell.getCellTypeEnum() == CellType.STRING){
				value = cell.getStringCellValue().trim();
			} else if(cell.getCellTypeEnum() == CellType.NUMERIC){
				value = String.valueOf(cell.getNumericCellValue()).trim();
			}
			list.add(value);
		}
		return list;
	}

	/**
	 * 逐行讀取權威檔資料成 ArrayList<ArrayList<String>>，再轉換成 jstree的樹狀目錄，不包含權威詞，只有目錄結構
	 * @param filePath 權威檔路徑   ex: "data/xlsx/"
	 * @param fileName 權威檔名        ex: "本草綱目.xlsx"
	 * @param title    檔威檔標題   ex: "本草綱目"
	 * */
	@SuppressWarnings("deprecation")
	public static JSONObject xlsxToJstreeJSON(String filePath, String fileName, String title, boolean setNote, String userName){
		
//		System.out.println("xlsxToJstreeJSON");
		ArrayList<JSONObject> data = new ArrayList<JSONObject>();
		ArrayList<String> header = null;
		try {
			// Excel -> XSSFWorkbook -> XSSFSheet -> XSSFRow -> XSSFCell
			InputStream excelToRead = new FileInputStream(filePath + fileName); // 讀Excel
			XSSFWorkbook workbook = new XSSFWorkbook(excelToRead); // poi 用 XSSFWorkbook 操作 Excel
			XSSFSheet sheet = workbook.getSheetAt(0); // 取得第一個試算表
			Iterator<Row> rows = sheet.rowIterator(); // 取得試算表的 row iterator
			header = UploadAuthority.XSSFRowToHeader((XSSFRow) rows.next()); // 第一行當作 header
			header.remove("註解");
			HashSet<String> set = new HashSet<String>();
			
			// 讀每一行的資料
			while(rows.hasNext()){
				
				XSSFRow row = (XSSFRow) rows.next();
				if(isEmptyXSSFRow(row)) 
					continue;
//				ArrayList<String> list = XSSFRowToArrayList(row, header.size());
				JSONObject json = XSSFRowToJSONObject(row, header);//convertToJSONObject(list, header);
				ArrayList<JSONObject> synonym = parseSynonym(json, header);
				data.addAll(synonym);
			}
			
			
			// 按照 path asc 排序，以符合取出 solr document 的順序
			Collections.sort(data, pathComparator);
			workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		header.remove(header.size() - 1); // remove "名"
		
		// 轉換成 jstree json 的格式，讓前端jstree library可以直接讀取
		return toJstreeJSON(title, header, data, setNote, userName);
	}
	
	/**
	 * txt的權威檔沒有階層
	 * @param title    檔威檔標題   ex: "本草綱目"
	 * */
	public static JSONObject txtToJstreeJSON(String title, boolean setNote, String userName){
	
		return toJstreeJSON(title, new ArrayList<String>(), new ArrayList<JSONObject>(), setNote, userName);
	}
	
	/**
	 * 逐行讀取權威檔資料成 ArrayList<ArrayList<String>>，再轉換成 jstree的樹狀目錄，不包含權威詞，只有目錄結構
	 * @param filePath 權威檔路徑   ex: "data/csv/"
	 * @param fileName 權威檔名        ex: "本草綱目.csv"
	 * @param title    檔威檔標題   ex: "本草綱目"
	 * */
	public JSONObject csvToJstreeJSON(String filePath, String fileName, String title, boolean setNote, String userName){
		
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
			header.remove("註解");
			while(scanner.hasNextLine()){
				
				// "a,b,c,d".split(sep, col)
				// 如果該筆資料的欄位數小於 col不影響結果。col=5, ["a", "b", "c", "d"]
				// 如果該筆資料的欄位數大於 col會截斷字串。col=3, ["a", "b", "c,d"]

				ArrayList<String> list = new ArrayList<String>(Arrays.asList(scanner.nextLine().split(sep, col)));
				if(isEmptyContent(list)){
					continue;
				}
				JSONObject json = convertToJSONObject(list, header);
				ArrayList<JSONObject> synonym = parseSynonym(json, header);
				data.addAll(synonym);
			}
			Collections.sort(data, pathComparator);
			fileInputStream.close();
			scanner.close();
		} catch(IOException e){
			e.printStackTrace();
		} 
		
		header.remove("名");
		return toJstreeJSON(title, header, data, setNote, userName);
	}
	
	public static Comparator<JSONObject> pathComparator = new Comparator<JSONObject>() {

		public int compare(JSONObject json1, JSONObject json2) {
			String path1 = "", path2 = "";
			for(String key : json1.keySet()){
				path1 += json1.getString(key);
			}
			for(String key : json2.keySet()){
				path2 += json2.getString(key);
			}

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
	public JSONObject tsvToJstreeJSON(String filePath, String fileName, String title, boolean setNote, String userName){
		
		String sep = "\t";
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
			
			header.remove("註解");
			
			while(scanner.hasNextLine()){
				
				// "a,b,c,d".split(sep, col)
				// 如果該筆資料的欄位數小於 col不影響結果。col=5, ["a", "b", "c", "d"]
				// 如果該筆資料的欄位數大於 col會截斷字串。col=3, ["a", "b", "c,d"]
				ArrayList<String> list = new ArrayList<String>(Arrays.asList(scanner.nextLine().split(sep, col)));
				if(isEmptyContent(list)){
					continue;
				}
				JSONObject json = convertToJSONObject(list, header);

				ArrayList<JSONObject> synonym = parseSynonym(json, header);
				data.addAll(synonym);
			}
			Collections.sort(data, pathComparator);
			fileInputStream.close();
			scanner.close();
		} catch(IOException e){
			e.printStackTrace();
		} 
		header.remove("名");
		return toJstreeJSON(title, header, data, setNote, userName);
	}
	
	public JSONArray csvToSolrDoc(String filePath, String fileName, String title, String authorityId, boolean setNote)throws AuthorityFileUploadException{
		
		String sep = ",";
		ArrayList<JSONObject> data = readTextUploadFile(filePath, fileName, sep, setNote);
		return toSolrDoc(authorityId, data);
	}
	
	public JSONArray tsvToSolrDoc(String filePath, String fileName, String title, String authorityId, boolean setNote) throws AuthorityFileUploadException{
		
		String sep = "\t";
		ArrayList<JSONObject> data = readTextUploadFile(filePath, fileName, sep, setNote);
		return toSolrDoc(authorityId, data);
	}
	
	public JSONArray txtToSolrDoc(String filePath, String fileName, String title, String authorityId, boolean setNote) throws AuthorityFileUploadException{
	
		String sep = "\n";
		ArrayList<JSONObject> data = readTextUploadFile(filePath, fileName, sep, setNote);
		return toSolrDoc(authorityId, data);
	}
	
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
			int notePos = header.indexOf("註解");
			if(notePos == -1  && !setNote){
				// 檔案沒有註解，標頭補上註解欄位
				header.add("註解");
			} 
			this.authorityFileUploadException.setHeader(header);
			validateHeader(header, setNote); // 驗證 header格式
			
			int limit = header.size();
			int num = 1; 
			while(rows.hasNext()){
				
				XSSFRow row = (XSSFRow) rows.next();
				if(!isEmptyXSSFRow(row)){
					JSONObject json_data = UploadAuthority.XSSFRowToJSONObject(row, header);
					// 驗證 row data格式
					if(validate(json_data, num)){
						ArrayList<JSONObject> synonym = parseSynonym(json_data, header);
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
		System.out.println(header);
		return toSolrDoc(authorityId, data);
	}
	/** 轉換成 solr document格式的JSON
	 * */
	public JSONArray toSolrDoc(String authorityId, ArrayList<JSONObject> data){
		

		JSONArray arr = new JSONArray();
		ArrayList<String> header = this.authorityFileUploadException.getHeader();
		HashSet<String> keyword_set = new HashSet<String>(); // 相同路徑重複的關鍵字
		HashMap<String, String> loc = new HashMap<String, String>(); // 權威詞讀入的順序
		for(JSONObject json : data){
			String keyword = json.getString("名");
			String note    = json.getString("註解").equals("") ? "無" : json.getString("註解");
			String path = "";
			for(String key : header){
				String value = json.getString(key);
				if(!value.equals("") && !key.equals("名") && !key.equals("註解")){
					path += value + "/";
				}
			}
			// 如果"名"有同義詞的話，會多一層extraPath
			if(json.has("extraPath") && !json.getString("extraPath").equals("")){
				path += json.getString("extraPath") + "/";
			}
			
			
			// 過濾同路徑相同的權威詞
			// 不重複: 本草綱目/水部/天水類/雨水/雨水 & 本草綱目/水部/天水類/立春雨水/雨水
			if(keyword_set.contains(path + keyword)){
//				System.out.println("duplicate: " + json.toString());
			}
			
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
			if(cell == null){
				value = "";
			} else if(cell.getCellTypeEnum() == CellType.STRING){
				value = cell.getStringCellValue().trim();
			} else if(cell.getCellTypeEnum() == CellType.NUMERIC){
				value = String.valueOf(cell.getNumericCellValue()).trim();
			}
			json.put(header.get(i), value);
		}
		json.put("extraPath", "");
		return json;
	}
	
	public static JSONObject convertToJSONObject(ArrayList<String> arr, ArrayList<String> header){
		
		JSONObject json = new JSONObject();
		for(int i = 0; i < header.size(); ++i){
			String value = i < arr.size() ? arr.get(i) : "";
			json.put(header.get(i), value);
		}
		json.put("extraPath", "");
		return json;
	}

	
	private static boolean isEmptyXSSFRow(XSSFRow row) {
	    
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
	public static ArrayList<JSONObject> parseSynonym(JSONObject json_data, ArrayList<String> header){
		
		ArrayList<ArrayList<String>> synonymPath  = new ArrayList<ArrayList<String>>();
		for(String key : header){
			if(key.equals("名")){
				break;
			}
			ArrayList<String> arr = new ArrayList<String>();
			String str = json_data.getString(key).replace("(", synonymSeparatar).replace(")", "");
			arr.addAll(new ArrayList<String>(Arrays.asList(str.split(synonymSeparatar))));
			synonymPath.add(arr);
		}
				
		ArrayList<JSONObject> res = buildSynonymPath(synonymPath, new JSONObject(), header, 0);

		ArrayList<JSONObject> synonym = new ArrayList<JSONObject>();
		if(json_data.has("名")){ 
			String keyword = json_data.getString("名").replace("(", synonymSeparatar).replace(")", "");
			String note    = json_data.has("註解") ? json_data.getString("註解") : "無";
			ArrayList<String> synonymKeyword = new ArrayList<String>(Arrays.asList(keyword.split(synonymSeparatar)));
			for(int i = 0; i < res.size() || i == 0 ; ++i){
				JSONObject tmp = res.size() == 0 ? new JSONObject() : res.get(i);
				for(int j = 0; j < synonymKeyword.size(); ++j){
					JSONObject json = copyFrom(tmp);
					if(synonymKeyword.size() > 1){
						json.put("extraPath", synonymKeyword.get(0));
					}
					json.put("名", synonymKeyword.get(j));
					json.put("註解", note);
					synonym.add(json);
					
				}
			}
		} else { // for jstree json
			synonym = res;
		}
		return synonym;
	}
	
	/**
	 * 產生同義的資料
	 * */
	private static ArrayList<JSONObject> buildSynonymPath(ArrayList<ArrayList<String>> synonym, JSONObject json, ArrayList<String> header, int i){
		
		if(synonym.size() == i){
			return new ArrayList<JSONObject>(); // return empty ArrayList
		}

		ArrayList<JSONObject> new_data = new ArrayList<JSONObject>();
		ArrayList<String> syn = synonym.get(i);
		for(int j = 0; j < syn.size(); ++j){
			JSONObject new_json = copyFrom(json);
			new_json.put(header.get(i), syn.get(j));
			ArrayList<JSONObject> res = buildSynonymPath(synonym, new_json, header, i+1);
			if(res.size() > 0){
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
		
		
		// 取出資料
		JSONObject json = new JSONObject();
		for(int i = 0; i < header.size(); ++i)
			json.put("欄位 " + (i+1), header.get(i).equals("") ? "\" \"" : header.get(i));
		
		// 檢查「註解」
		// 檔案有「註解」，但是不在最後一欄，throw exception
		int noteIndex = header.indexOf("註解");
		if(noteIndex == -1 && setNote){
			this.authorityFileUploadException.appendErrorMessage("標頭缺少「註解」欄位"); // 記錄錯誤訊息
			this.authorityFileUploadException.appendErrorData(json); // 記錄錯誤資料
			this.authorityFileUploadException.setHasException(true);
			throw this.authorityFileUploadException;
		}
		if(noteIndex != header.size()-1) {
			this.authorityFileUploadException.appendErrorMessage("「註解」欄位必須在最後一欄"); // 記錄錯誤訊息
			this.authorityFileUploadException.appendErrorData(json); // 記錄錯誤資料
			this.authorityFileUploadException.setHasException(true);
			throw this.authorityFileUploadException;
		}
	
		// 檢查「名」
		int keywordIndex = header.indexOf("名");
		if(keywordIndex == -1){
			this.authorityFileUploadException.appendErrorMessage("標頭缺少「名」欄位"); // 記錄錯誤訊息
			this.authorityFileUploadException.appendErrorData(json); // 記錄錯誤資料
			this.authorityFileUploadException.setHasException(true);
			throw this.authorityFileUploadException;
		} else if(keywordIndex != header.size()-2){
			this.authorityFileUploadException.appendErrorMessage("「名」欄位必須在倒數第二欄"); // 記錄錯誤訊息
			this.authorityFileUploadException.appendErrorData(json); // 記錄錯誤資料
			this.authorityFileUploadException.setHasException(true);
			throw this.authorityFileUploadException;
		}
		
		// 檢查「欄位名稱」
		for(String s : header){
			if(s.equals("")){
				this.authorityFileUploadException.appendErrorMessage("欄位名稱不可空白"); // 記錄錯誤訊息
				this.authorityFileUploadException.appendErrorData(json); // 記錄錯誤資料
				this.authorityFileUploadException.setHasException(true);
				throw this.authorityFileUploadException;
			}
		}
//		if(hasSolrSpecialChar(header)){
//			this.authorityFileUploadException.appendErrorMessage("欄位不可包含特殊符號"); // 記錄錯誤訊息
//			this.authorityFileUploadException.appendErrorData(json); // 記錄錯誤資料
//			this.authorityFileUploadException.setHasException(true);
//			throw this.authorityFileUploadException;
//		}
	}
	
	/**
	 * 驗證每一行資料格式是否正確，若錯誤的話，紀錄該筆資料，最後一次顯示所有錯誤的資料
	 * @param row_data  標頭欄位 ex: [部, 類, 種, 名, 註解], [部, 類, 種, 名]
	 * @param setNote 是否設定註解
	 * @param index   第幾筆資料
	 * */
	public boolean validate(JSONObject _json, int index) {
		
		ArrayList<String> header = this.authorityFileUploadException.getHeader();
		JSONObject json = new JSONObject(_json.toString());
		json.remove("extraPath");
		boolean isValidate = true;
		int n = json.length();
		
//		if(n != m){
//			this.authorityFileUploadException.appendErrorMessage("第" + index + "筆資料：欄位數量錯誤"); // 記錄錯誤訊息
//			this.authorityFileUploadException.appendErrorData(json); // 記錄錯誤資料
//			this.authorityFileUploadException.setHasException(true);
//			isValidate = false;
//		}
		if(json.getString("名").equals("")){
			this.authorityFileUploadException.appendErrorMessage("第" + index + "筆資料：權威詞不可空白"); // 記錄錯誤訊息
			this.authorityFileUploadException.appendErrorData(json); // 記錄錯誤資料
			this.authorityFileUploadException.setHasException(true);
			isValidate = false;
		}
		
		int offset = 3;
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
				hasValue = !json.getString(key).equals("");	
			}
		}
		
//		if(hasSolrSpecialChar(json)){
//			this.authorityFileUploadException.appendErrorMessage("第" + index + "筆資料：不可包含特殊符號"); // 記錄錯誤訊息
//			this.authorityFileUploadException.appendErrorData(json); // 記錄錯誤資料
//			this.authorityFileUploadException.setHasException(true);
//			isValidate = false;
//		}

		// 括號是否成對
		if(!validParenthesis(json)){
			this.authorityFileUploadException.appendErrorMessage("第" + index + "筆資料：括號必須成對"); // 記錄錯誤訊息
			this.authorityFileUploadException.appendErrorData(json); // 記錄錯誤資料
			this.authorityFileUploadException.setHasException(true);
			isValidate = false;
		}
				
		return isValidate;
	}
	
//	public boolean hasSolrSpecialChar(ArrayList<String> row_data){
//		
//		for(String data : row_data){
//			for(int i = 0; i < solrSpecialChar.length(); ++i){
//				if(data.indexOf(solrSpecialChar.charAt(i)) != -1){
//					return true;
//				}
//			}
//		}
//		return false;
//	}
//	
//	public boolean hasSolrSpecialChar(JSONObject json){
//		
//		for(String key : json.keySet()){
//			if(key.equals("註解")){
//				continue;
//			}
//			for(int i = 0; i < solrSpecialChar.length(); ++i){
//				String value = json.getString(key);
//				if(value.indexOf(solrSpecialChar.charAt(i)) != -1){
//					return true;
//				}
//			}
//		}
//		return false;
//	}
	
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
	 * 以等號分隔多個同義詞。 ex: 台灣=臺灣=中華民國
	 * 以( )表示一個同義詞，不可多個。 ex: 台灣(臺灣)。 error: 台灣(臺灣)(中華民國)
	 * */
//	public boolean validSynonym(JSONObject json){
//				
//		Pattern paren = Pattern.compile("[(][.]*[)]");
//		for(String key : json.keySet()){
//			if(key.equals("註解")){
//				continue;
//			}
//			String value = json.getString(key);
//			
//			 不能同時出現 'synonymSeparatar' ()
//			if(value.contains(this.synonymSeparatar) && (value.contains("(") || value.contains(")"))){
//				return false;
//			}
//			
//			 不同有多對 ()
//			Matcher parenMatcher = paren.matcher(value);
//			for(int i = 0; parenMatcher.find(); ++i){
//				if(i > 0){
//					return false;
//				}
//			}
//		}
//		return true;
//	}
	
	public static void main(String[] args) {
		
		
//		String filePath = "data/xlsx/";		
//		String title = "本草綱目";
//		
////		String fileName = "藥名.txt";		
////		String fileName = "本草綱目.csv";
////		String fileName = "本草綱目.xlsx";
//////		String fileName = "太平廣記卷96權威詞.xlsx";
//		String fileName = "太平廣記卷96權威詞-synonym.xlsx";
////		String fileName = "法鼓山地名2.xlsx";
//		String authorityId = "0";
//		String username = "chiamin";
//		boolean setNote = true;
//		
//		UploadAuthority uploadAuthority = new UploadAuthority();
//		try {
//			long start = new Date().getTime();
////			JSONArray arr = uploadAuthority.txtToSolrDoc(filePath, fileName, title, authorityId, setNote);
////			JSONArray arr = uploadAuthority.csvToSolrDoc(filePath, fileName, title, authorityId, setNote);
////			JSONArray arr = uploadAuthority.tsvToSolrDoc(filePath, fileName, title, authorityId, setNote);
//			
//			JSONArray arr = uploadAuthority.xlsxToSolrDoc(filePath, fileName, title, authorityId, setNote);
////			long time = new Date().getTime();
//			
////			JSONObject obj = uploadAuthority.txtToJstreeJSON(title, setNote, username);
////			JSONObject obj = uploadAuthority.csvToJstreeJSON(filePath, fileName, title, setNote, username);
////			JSONObject obj = uploadAuthority.tsvToJstreeJSON(filePath, fileName, title, setNote, username);
//			
//			JSONObject obj = uploadAuthority.xlsxToJstreeJSON(filePath, fileName, title, setNote, username);
////			long end = new Date().getTime();
////			System.out.println(end - time);
//			System.out.println("solr document list\n" + arr);
//			System.out.println("jstree json\n" + obj);
//			
//		} catch (AuthorityFileUploadException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			String message = e.getMessage();
//			JSONArray error_data = e.getErrorData();
//			
//			ArrayList<String> error_msg = e.getErrorMessage();
//			for(int i = 0; i < error_msg.size(); ++i){
//				System.out.println(error_msg.get(i));
//				System.out.println(error_data.get(i));
//				System.out.println("");
//			}
//		}	 
	}

}
