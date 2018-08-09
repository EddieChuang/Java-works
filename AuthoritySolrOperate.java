package ascdc.sinica.dhtext.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;
import java.sql.Connection;

import ascdc.sinica.dhtext.tool.solr.UploadAuthority;
import ascdc.sinica.dhtext.tool.tree.JstreeNode;
import ascdc.sinica.dhtext.util.io.JSONOperate;
import ascdc.sinica.dhtext.util.sys.Command;
import dao.DictDAO;

public class AuthoritySolrOperate {
	
	private final int BIGINTEGER = 1000000;  
	private SolrClient solrClient = null;
	private String solrServerURL = Command.solrServerURL;
	private String corename = "keyword";
	private String authorityId = null;
	private ArrayList<String> header = new ArrayList<String>();
	
	
	public AuthoritySolrOperate(String solrServerURL, String corename, String authorityId){
		this.solrServerURL = solrServerURL;
		this.corename = corename;
		this.authorityId = authorityId;
		this.solrClient = new HttpSolrClient(this.solrServerURL + this.corename);
	}
	
	// chiamin
	public AuthoritySolrOperate(String solrServerURL, String corename, String authorityId, Connection conn){
		this.solrServerURL = solrServerURL;
		this.corename = corename;
		this.authorityId = authorityId;
		this.solrClient = new HttpSolrClient(this.solrServerURL + this.corename);
		
		if(conn != null){
			DictDAO dictDAO = new DictDAO(conn);
			this.header = dictDAO.getHeader(Long.valueOf(authorityId));
		}
	}
	
	public ArrayList<String> hasDuplicateKeyword(ArrayList<String> keyword, String path){
		
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.set("q", "authorityId:" + this.authorityId);
		solrQuery.set("fl", "text");
		solrQuery.addFilterQuery("hidden:false");
		solrQuery.addFilterQuery("path:" + path);
		
		
		if(keyword.size() > 0){
			String filterQuery = "{!terms f=text}" + keyword.toString().replaceAll("[\\[\\] ]", "");
			solrQuery.addFilterQuery(filterQuery);
		}
		SolrInputDocument solrInputDoc = new SolrInputDocument();
		solrInputDoc.addField("price", new JSONObject("{\"set\": 10}"));
		ArrayList<SolrInputDocument> solrDocList = this.getSolrQueryResponse(solrQuery);
		ArrayList<String> duplicateText = new ArrayList<String>();
		for(int i = 0; i < solrDocList.size(); ++i){
			duplicateText.add(solrDocList.get(i).getFieldValue("text").toString());
			
		}
		
		return duplicateText;
	}
	
	/**
	  * 回傳路徑下相同名稱的權威詞
	  * @param arr 
	  */
	public ArrayList<String> hasDuplicateKeyword(JSONArray arr){

		SolrQuery solrQuery = new SolrQuery();
		solrQuery.set("q", "authorityId:" + this.authorityId);
		solrQuery.set("fl", "text");
		solrQuery.addFilterQuery("hidden:false");
		solrQuery.addFilterQuery("path:" + arr.getJSONObject(0).getString("path"));
		
		String filterQuery = "{!terms f=text}" + arr.getJSONObject(0).getString("text");
		
//		String fq = "text:" + arr.getJSONObject(0).getString("text");
		for(int i = 1; i < arr.length(); ++i){
			filterQuery += "," + arr.getJSONObject(i).getString("text");
//			fq += " || text:" + arr.getJSONObject(i).getString("text");
		}
		solrQuery.addFilterQuery(filterQuery);
		
		SolrDocumentList duplicateSolrDoc = null;
		try {
			QueryResponse rsp = this.solrClient.query(solrQuery);
			duplicateSolrDoc = rsp.getResults();
		} catch (SolrServerException | IOException e) {
			e.printStackTrace();
		}
		
		// 取出重複的權威詞
		ArrayList<String> duplicateText = new ArrayList<String>();
		for(int i = 0; i < duplicateSolrDoc.size(); ++i){
			SolrDocument solrDoc = duplicateSolrDoc.get(i);
			duplicateText.add(solrDoc.getFieldValue("text").toString());
		}
		
		return duplicateText;
	}
	
	public static ArrayList<SolrInputDocument> jsonArrToSolrDocArr(JSONArray arr){
		ArrayList<SolrInputDocument> docArrList = new ArrayList<SolrInputDocument>();
		for(int i = 0; i < arr.length(); ++i){
			SolrInputDocument document = new SolrInputDocument();
			JSONObject json = arr.getJSONObject(i);
			for(String key : json.keySet()){
				document.setField(key, json.getString(key));
			}
			docArrList.add(document);
		}
		return docArrList;
	}
	
	private ArrayList<SolrInputDocument> getSolrQueryResponse(SolrQuery solrQuery){
		
		//long start = new Date().getTime();
		
		ArrayList<SolrInputDocument> docArrList = new ArrayList<SolrInputDocument>();
		try{
			QueryResponse rsp = this.solrClient.query(solrQuery);
			SolrDocumentList docList = rsp.getResults();
			for(int i = 0; i < docList.size(); ++i){
				SolrDocument doc = docList.get(i);
				SolrInputDocument inputDoc = new SolrInputDocument();
				for(String key : doc.keySet()){
					inputDoc.setField(key,  doc.getFieldValue(key));
				}
				docArrList.add(inputDoc);
			}
			
		} catch(SolrServerException | IOException e){
			e.printStackTrace();
		}
		//System.out.println("getSolrQueryResponse: " + String.valueOf(new Date().getTime() - start));
		return docArrList;
	}
	
	/**
	  * 回傳所有的 solr document
	  * @param hidden ["false", "true", "*"]
	  */
	public ArrayList<SolrInputDocument> getSolrDoc(String hidden, String fl, String sort){
		
//		long start = new Date().getTime();
		
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.set("q", "authorityId:" + this.authorityId);
		solrQuery.set("fl", fl);
		solrQuery.set("sort", sort);
		solrQuery.set("rows", this.BIGINTEGER);
		solrQuery.addFilterQuery("hidden:" + hidden);
		
		ArrayList<SolrInputDocument> res = this.getSolrQueryResponse(solrQuery);
//		System.out.println("getSolrDoc: " + String.valueOf(new Date().getTime() - start));
		return res;
	}
	
	/**
	  * 回傳指定 text 的 solr document
	  * @param hidden ["false", "true", "*"]
	  * @param keyword
	  */
	public ArrayList<SolrInputDocument> getSolrDocByText(String hidden, String fl, ArrayList<String> keyword){
		
//		long begin = new Date().getTime();
		
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.set("q", "authorityId:" + this.authorityId);
		solrQuery.set("fl", fl);
		solrQuery.set("rows", this.BIGINTEGER);
		solrQuery.addFilterQuery("hidden:" + hidden);
		
		if(keyword.size() > 0){
			String filterQuery = "{!terms f=text}" + keyword.toString().replaceAll("[\\[\\] ]", ""); // match '[', ']', ' ' 
			solrQuery.addFilterQuery(filterQuery);
		}
		
		ArrayList<SolrInputDocument> res = this.getSolrQueryResponse(solrQuery);
//		System.out.println("getSolrDocByText: " + String.valueOf(new Date().getTime() - begin));
		
		return res;
	}
	
	/**
	  * 回傳指定 id 的 solr document，使用者點選的權威詞，所以一定是 hidden:false
	  * @param idList  
	  */
	public ArrayList<SolrInputDocument> getSolrDocById(List<String> idList, String fl){
		
//		long begin = new Date().getTime();
		
		ArrayList<SolrInputDocument> res = new ArrayList<SolrInputDocument>();
		int batchSize = 50;
		int size = idList.size();
		int iteration = (int) Math.ceil((double)size / batchSize);
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.set("q", "authorityId:" + this.authorityId);
		solrQuery.set("fl", fl);
		solrQuery.set("rows", this.BIGINTEGER);
		for(int i = 0; i < iteration; ++i){
				
			int start = i * batchSize;
			int end   = start + batchSize < size ? start + batchSize : size;
			
			ArrayList<String> idSubList = new ArrayList<String>(idList.subList(start, end));
			if(idSubList.size() > 0){
				String filterQuery = "{!terms f=id}" + idSubList.toString().replaceAll("[\\[\\] ]", ""); // match '[', ']', ' ' 
				solrQuery.setFilterQueries(filterQuery);
			}
			
			ArrayList<SolrInputDocument> solrDocList = this.getSolrQueryResponse(solrQuery);
			res.addAll(solrDocList);
		}
		
//		System.out.println("getSolrDocById: " + String.valueOf(new Date().getTime() - begin));
		return res;
	}
	

	/**
	  * 回傳指定 path 的 solr document
	  * @param path  
	  * @param hidden ["*", "true", "false"]
	  */
	public ArrayList<SolrInputDocument> getSolrDocByPath(String path, String hidden, String fl){
		
//		long begin = new Date().getTime();
		
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.set("q", "authorityId:" + this.authorityId);
		solrQuery.set("fl",  fl);
		solrQuery.set("rows", this.BIGINTEGER);
		solrQuery.addFilterQuery("path:" + path);
		solrQuery.addFilterQuery("hidden:" + hidden);
	
		ArrayList<SolrInputDocument> res = this.getSolrQueryResponse(solrQuery);
//		System.out.println("getSolrDocByPath: " + String.valueOf(new Date().getTime() - begin));
		
		return res;
	}
	
	/**
	  * 回傳指定 path 的 solr document 且 keyword.contains(text)
	  * @param path  
	  * @param hidden ["*", "true", "false"]
	  * @param keyword
	  */
	public ArrayList<SolrInputDocument> getSolrDocByPath(String path, String hidden, String fl, ArrayList<String> keyword){
		
//		long begin = new Date().getTime();
		
		ArrayList<SolrInputDocument> res = new ArrayList<SolrInputDocument>();
		int batchSize = 50;
		int size = keyword.size();
		int iteration = (int) Math.ceil((double)size / batchSize);
		for(int i = 0; i < iteration; ++i){
			
			SolrQuery solrQuery = new SolrQuery();
			solrQuery.set("q", "authorityId:" + this.authorityId);
			solrQuery.set("fl", fl);
			solrQuery.set("rows", this.BIGINTEGER);
			solrQuery.addFilterQuery("path:" + path);// 包含子層
			solrQuery.addFilterQuery("hidden:" + hidden);
			
			int start = i * batchSize;
			int end   = start + batchSize < size ? start + batchSize : size;
			
			ArrayList<String> subList = new ArrayList<String>(keyword.subList(start, end));
			if(subList.size() > 0){
				String filterQuery = "{!terms f=text}" + subList.toString().replaceAll("[\\[\\] ]", ""); // match '[', ']', ' ' 
				solrQuery.addFilterQuery(filterQuery);
			}
			
			ArrayList<SolrInputDocument> solrDocList = this.getSolrQueryResponse(solrQuery);
			res.addAll(solrDocList);
		}
	
//		System.out.println("getSolrDocByPathWithKeyword: " + String.valueOf(new Date().getTime() - begin));
		return res;
	}
	
	/**
	  * 將 docArrList 更新到solr
	  * @param docArrList
	  */
	public void update(ArrayList<SolrInputDocument> solrDocList){
		
//		long start = new Date().getTime();
		
		try {
			if(solrDocList.size() > 0){
			
				UpdateResponse updateResponse = solrClient.add(solrDocList);
				solrClient.commit();
			}
		} catch (SolrServerException | IOException e) {
			e.printStackTrace();
		}
//		System.out.println("update: " + String.valueOf(new Date().getTime() - start));
	}
	 
	/**
	  * 刪除指定 id 的 solr document，將 hidden 設為 true
	  * @param idToDel
	  */
	public void deleteById(List<String> idToDel){
		
//		long begin = new Date().getTime();
		
		String fl = "*";
		ArrayList<SolrInputDocument> docArrList = this.getSolrDocById(idToDel, fl);
		for(int i = 0; i < docArrList.size(); ++i){
			docArrList.get(i).setField("hidden", "true");
		}
		this.update(docArrList);
		
//		System.out.println("deleteById: " + String.valueOf(new Date().getTime() - begin));
	}
	
	
	/**
	  * 刪除路徑下所有的 solr document，將 hidden 設為 true
	  * @param path
	  */
	public void deleteByPath(String path){
		
//		long begin = new Date().getTime();
		
		String hidden = "false";
		String fl = "*";
		ArrayList<SolrInputDocument> docArrList = this.getSolrDocByPath(path, hidden, fl);

		for(int i = 0; i < docArrList.size(); ++i){
			docArrList.get(i).setField("hidden", "true");
		}
		this.update(docArrList);
		
//		System.out.println("deleteByPath: " + String.valueOf(new Date().getTime() - begin));
	}
	
	/**
	  * 將oldPath下所有solr document的path改成 newPath
	  * @param oldPath
	  * @param newPath
	  */
	public void renamePath(String oldPath, String newPath){
		
//		long begin = new Date().getTime();
		
		String hidden = "*";
		String fl = "*";
		ArrayList<SolrInputDocument> docArrList = this.getSolrDocByPath(oldPath + "*", hidden, fl);
		for(int i = 0; i < docArrList.size(); ++i){
			String path = docArrList.get(i).getFieldValue("path").toString();
			docArrList.get(i).setField("path", path.replace(oldPath, newPath));
		}
		this.update(docArrList);
		
//		System.out.println("renamePath: " + String.valueOf(new Date().getTime() - begin));

	}
	
	/**
	  * 從 solr 匯出權威檔成XLSX
	  */
	public ByteArrayOutputStream exportXLSX(){
		
		System.out.println("\nexportXLSX");
		
		long start = new Date().getTime();
		
		
		// 從solr讀取權威檔，解析階層，計算階層數。 solrDocList -> JSONArray 
		int nHier = 0; // 階層數。總欄位數 = 階層數 + 名 + 註解
		String hidden = "false";
		String fl = "path,text,note";
		String sort = "path asc";
		ArrayList<SolrInputDocument> solrDocList = this.getSolrDoc(hidden, fl, sort);
		JSONArray data = new JSONArray();  // 權威檔資料
		ArrayList<ArrayList<String>> allHier = new ArrayList<ArrayList<String>>();
		
		long time1 = new Date().getTime();
		for(int i = 0; i < solrDocList.size(); ++i){
			SolrInputDocument solrInputDoc = solrDocList.get(i);
			String path = solrInputDoc.getFieldValue("path").toString();
			String text = solrInputDoc.getFieldValue("text").toString();
			String note = solrInputDoc.getFieldValue("note").toString();
			
			ArrayList<String> hier = new ArrayList<String>(Arrays.asList(path.split("/"))); // 解析階層
			hier.remove(0);
			allHier.add(hier);
			nHier = Math.max(nHier, hier.size());
			
			JSONObject row = new JSONObject();
			row.put("text", text);
			row.put("note", note);
			data.put(row);
		}
		long time2 = new Date().getTime();
		System.out.println("parsing solrDocList: " + String.valueOf(time2 - time1));
		
		ByteArrayOutputStream baos = null;
       try{ 
		    XSSFWorkbook wb = new XSSFWorkbook(); 
		    XSSFSheet sheet = wb.createSheet(); 
		    
		    // 寫入標頭
	        XSSFRow header = sheet.createRow(0);
	        for(int i = 0; i < nHier; ++i){
	        	
	        	// chiamin
	        	if(i < this.header.size() - 2){
	        		header.createCell(i).setCellValue(this.header.get(i));
	        	} else {
	        		header.createCell(i).setCellValue("extra path");
	        	}
	        }
	        header.createCell(nHier).setCellValue("名");
	        header.createCell(nHier+1).setCellValue("註解");
	        
	        
	        // 寫入每筆資料
	        for(int i = 0; i < data.length(); ++i){
	        	XSSFRow row = sheet.createRow(i+1);
	        	JSONObject obj = data.getJSONObject(i);
	        	ArrayList<String> hier = allHier.get(i);
	        	
	        	// 寫入階層
		        int j;
	        	for(j = 0; j < nHier; ++j){ 
	        		if(j < hier.size()){
	        			row.createCell(j).setCellValue(hier.get(j));
	        		} else{
	        			row.createCell(j).setCellValue("");
	        		}
		        } 
		        // 寫入權威詞、註解
		        row.createCell(j++).setCellValue(obj.getString("text"));
		        row.createCell(j).setCellValue(obj.getString("note"));
	        }
	        long time3 = new Date().getTime();
	        System.out.println("write to XSSFWorkbook: " + String.valueOf(time3 - time2));
	        
	        // 寫檔 XSSFWorkbook -> ByteArrayOutputStream -> FileOutputStream
		    baos = new ByteArrayOutputStream(); 
	        wb.write(baos); 

	        
	        long end = new Date().getTime();
	        System.out.println("write to xlsx: " + String.valueOf(end - start));
	        
	        baos.close(); 
	        wb.close();
	        
       }catch(IOException e){ 
         e.printStackTrace(); 
       }
       
       return baos;
	}

	
	/**
	  * authorityId的權威檔是否存在
	  */
	public boolean duplicateAuthorityId(String authorityId){
		
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.set("q", "authorityId:" + authorityId);
		
		try{
			QueryResponse rsp = this.solrClient.query(solrQuery);
			return rsp.getResults().size() > 0;
		}catch (SolrServerException | IOException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	/**
	  * 複製新的權威檔
	  */
	public void clone(String newAuthorityId, String newTitle){
		
		if(this.duplicateAuthorityId(newAuthorityId)){
			System.out.println("重複的權威檔 ID: " + newAuthorityId);
			return;
		}
		
		String hidden = "*";
		String sort = "";
		String fl = "path,text,note,loc,hidden";
		ArrayList<SolrInputDocument> solrDocList = this.getSolrDoc(hidden, fl, sort);
		
		for(int i = 0; i < solrDocList.size(); ++i){
			SolrInputDocument SolrDoc = solrDocList.get(i); 
			String path = SolrDoc.getFieldValue("path").toString();
			int    pos  = path.indexOf("/");
			String newPath = newTitle + path.substring(pos);
			
			SolrDoc.setField("authorityId", newAuthorityId);
			SolrDoc.setField("path", newPath);
		}
		this.update(solrDocList);
	}
	
	public JstreeNode getJstree(){
		
		String hidden = "false";
		String fl = "path";
		String sort = "path asc";
		ArrayList<SolrInputDocument> solrDocList = getSolrDoc(hidden, fl, sort);
		String title = "";
		
		String type = "default";
		JstreeNode jstree = new JstreeNode(type);
		for(int i = 0; i < solrDocList.size(); ++i){
			SolrInputDocument solrDoc = solrDocList.get(i);
			ArrayList<String> path = new ArrayList<String>(Arrays.asList(solrDoc.getFieldValue("path").toString().split("/")));
			title = path.get(0);
			path.remove(0);
			JstreeNode node = JstreeNode.toJstreeNode(path, 0);
			jstree.append(node);
		}
		jstree.setText(title);
		return jstree;
	}


	
	public static void main(String[] args) {
		
		
		Connection conn = null;
		try{
			//String mysqlUrl = "jdbc:mysql://172.16.10.64:3306/dhtext";
			String mysqlUrl = "jdbc:mysql://127.0.0.1:3306/dhtext";
			String mysqlUser = "dhtext";
			String mysqlPassword = "dhtext";
			Properties mysqlProps = new Properties();
	        mysqlProps.put("user", mysqlUser);
	        mysqlProps.put("password", mysqlPassword);
	        mysqlProps.put("SetBigStringTryClob", "true");
			Class.forName("org.mariadb.jdbc.Driver");
	        conn = DriverManager.getConnection(mysqlUrl, mysqlProps);
		}catch(Exception e){
			e.printStackTrace();
			//out.println("<br>"+e+"  Cannot access The Database<br>");
		}
		
//		JSONArray solrArr = new JSONArray();
//		JSONObject obj = new JSONObject();
//		obj.put("path", "本草綱目/火部/");
//		obj.put("note", "無");
//		obj.put("loc", "[12]");
//		obj.put("authorityId", "212");
//		obj.put("hidden", "false");
//		obj.put("text", "淫雨");
//		solrArr.put(obj);
//		solrArr.put(0, obj);
//		System.out.println(solrArr);
		
//		HashMap<String, String> map = new HashMap<String, String>();
//		map.put("a", "1");
//		map.put("b", "3");
//		System.out.println(map.toString());
		
		
		
		// solr server config
		String solrServerURL = "http://127.0.0.1:8983/solr/";      
		String corename = "keyword";
//		
//		
//		String filePath = "export/";
//		String fileName = "本草綱目.xlsx";
//		String fileName = "本草綱目.txt";

		String authorityId = "345";
		String hidden = "*";
		String fl = "*";
		AuthoritySolrOperate aso = new AuthoritySolrOperate(solrServerURL, corename, authorityId, conn); //不需要連接mysql就給null
		JstreeNode jstree  = aso.getJstree();

		System.out.println(jstree.toJSON());
//		ArrayList<SolrInputDocument> solrDocList = aso.getSolrDoc(hidden, fl, "");
//		ArrayList<String> path = new ArrayList<String>();
//		for(int i = 0; i < solrDocList.size(); ++i){
//			path.add(solrDocList.get(i).getFieldValue("path").toString());
//		}
//		System.out.println(path);
//		Collections.sort(path);
//		System.out.println(path);
		
//		authoritySolrOperate.exportXLSX(filePath, fileName);
		
//		fileName = "本草綱目_onlyText.xlsx";
//		authoritySolrOperate.exportXLSXOnlyText(filePath, fileName);
//		
//		fileName = "本草綱目.txt";
//		authoritySolrOperate.exportTXT(filePath, fileName);
		
//		List<String> idToDel = new ArrayList<String>();
//		idToDel.add("8ee2c424-2da9-42c4-8c9e-02527fd650b0");
//		idToDel.add("0f233e18-7997-4d0f-9210-35f66c800f17");
//		idToDel.add("ab041474-6c19-4961-b333-bbee367ef937");
//		authoritySolrOperate.deleteById(idToDel);
		
//		System.out.println(Math.floorDiv(1, 50));
//		System.out.println(Math.ceil(1 / (float)50));
		
//		SolrInputDocument solrDoc = new SolrInputDocument();
//		solrDoc.addField("text", "value");
//		solrDoc.removeField("text");
//		System.out.println(solrDoc);
		
//		ArrayList<ArrayList<String>> arr = new ArrayList<ArrayList<String>>();
//		ArrayList<String> arr2 = new ArrayList<String>();
//		arr.add(arr2);
//		JSONObject obj = new JSONObject();
//		obj.put("arr", new Gson().toJson(arr));
//		String str = obj.getString("arr");
//		ArrayList<ArrayList<String>> list = new Gson().fromJson(str, ArrayList.class);
//		System.out.println(list);
		
		
		
		
		
	}

}
