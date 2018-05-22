package ascdc.sinica.dhtext.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

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

public class AuthoritySolrOperate {
	
	private final int BIGINTEGER = 1000000;  
	private SolrClient solrClient = null;
	private String solrServerURL = null;
	private String corename = null;
	private String authorityId = null;
	
	public AuthoritySolrOperate(String solrServerURL, String corename, String authorityId){
		this.solrServerURL = solrServerURL;
		this.corename = corename;
		this.authorityId = authorityId;
		this.solrClient = new HttpSolrClient(this.solrServerURL + this.corename);
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
		return docArrList;
	}
	
	/**
	  * 回傳所有的 solr document
	  * @param hidden ["false", "true", "*"]
	  */
	public ArrayList<SolrInputDocument> getSolrDoc(String hidden, String fl, String sort){
		
		long start = new Date().getTime();
		
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.set("q", "authorityId:" + this.authorityId);
		solrQuery.set("fl", fl);
		solrQuery.set("sort", sort);
		solrQuery.set("rows", this.BIGINTEGER);
		solrQuery.addFilterQuery("hidden:" + hidden);
		
		ArrayList<SolrInputDocument> res = this.getSolrQueryResponse(solrQuery);
		System.out.println("getSolrDoc: " + String.valueOf(new Date().getTime() - start));
		return res;
	}
	
	/**
	  * 回傳指定 text 的 solr document
	  * @param hidden ["false", "true", "*"]
	  * @param keyword
	  */
	public ArrayList<SolrInputDocument> getSolrDocByText(String hidden, String fl, ArrayList<String> keyword){
		
		long start = new Date().getTime();
		
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.set("q", "authorityId:" + this.authorityId);
		solrQuery.set("fl", fl);
		solrQuery.set("rows", this.BIGINTEGER);
		solrQuery.addFilterQuery("hidden:" + hidden);
		
		if(keyword.size() > 0){
			String filterQuery = "{!terms f=text}" + keyword.toString().replaceAll("[\\[\\] ]", ""); // match '[', ']', ' ' 
			solrQuery.addFilterQuery(filterQuery);
//			String filterQuery = "text:" + keyword.get(0);
//			for(int i = 1; i < keyword.size(); ++i){
//				filterQuery += " || text:" + keyword.get(i);
//			}
//			solrQuery.addFilterQuery(filterQuery);
		}
		
		ArrayList<SolrInputDocument> res = this.getSolrQueryResponse(solrQuery);
		System.out.println("getSolrDocByText: " + String.valueOf(new Date().getTime() - start));
		
		return res;
	}
	
	/**
	  * 回傳指定 id 的 solr document，使用者點選的權威詞，所以一定是 hidden:false
	  * @param idList  
	  */
	public ArrayList<SolrInputDocument> getSolrDocById(List<String> idList){
		
		long start_ = new Date().getTime();
		
		ArrayList<SolrInputDocument> res = new ArrayList<SolrInputDocument>();
		int batchSize = 50;
		int size = idList.size();
		int iteration = (int) Math.ceil((double)size / batchSize);//Math.floorDiv(size, batchSize);
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.set("q", "authorityId:" + this.authorityId);
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
		
		System.out.println("getSolrDocById: " + String.valueOf(new Date().getTime() - start_));
		return res;
	}
	

	/**
	  * 回傳指定 path 的 solr document
	  * @param path  
	  * @param hidden ["*", "true", "false"]
	  */
	public ArrayList<SolrInputDocument> getSolrDocByPath(String path, String hidden){
		
		long start = new Date().getTime();
		
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.set("q", "authorityId:" + this.authorityId);
		solrQuery.set("rows", this.BIGINTEGER);
		solrQuery.addFilterQuery("path:" + path);
		solrQuery.addFilterQuery("hidden:" + hidden);
	
		ArrayList<SolrInputDocument> res = this.getSolrQueryResponse(solrQuery);
		System.out.println("getSolrDocByPath: " + String.valueOf(new Date().getTime() - start));
		
		return res;
	}
	
	/**
	  * 回傳指定 path 的 solr document 且 keyword.contains(text)
	  * @param path  
	  * @param hidden ["*", "true", "false"]
	  * @param keyword
	  */
	public ArrayList<SolrInputDocument> getSolrDocByPath(String path, String hidden, ArrayList<String> keyword){
		
		long start = new Date().getTime();
		
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.set("q", "authorityId:" + this.authorityId);
		solrQuery.set("rows", this.BIGINTEGER);
		solrQuery.addFilterQuery("path:" + path);// 包含子層
		solrQuery.addFilterQuery("hidden:" + hidden);
		
		if(keyword.size() > 0){
			String filterQuery = "text:" + keyword.get(0);
			for(int i = 1; i < keyword.size(); ++i){
				filterQuery += " || text:" + keyword.get(i);
			}
			solrQuery.addFilterQuery(filterQuery);
		}
	
		ArrayList<SolrInputDocument> res = this.getSolrQueryResponse(solrQuery);
		System.out.println("getSolrDocByPathWithKeyword: " + String.valueOf(new Date().getTime() - start));
		
		return res;
	}
	
	/**
	  * 將 docArrList 更新到solr
	  * @param docArrList
	  */
	public void update(ArrayList<SolrInputDocument> solrDocList){
		
		long start = new Date().getTime();
		
		try {
			if(solrDocList.size() > 0){
				UpdateResponse updateResponse = solrClient.add(solrDocList);
				solrClient.commit();
			}
		} catch (SolrServerException | IOException e) {
			e.printStackTrace();
		}
		System.out.println("update: " + String.valueOf(new Date().getTime() - start));
	}
	 
	/**
	  * 刪除指定 id 的 solr document，將 hidden 設為 true
	  * @param idToDel
	  */
	public void deleteById(List<String> idToDel){
		
		long start = new Date().getTime();
		
		ArrayList<SolrInputDocument> docArrList = this.getSolrDocById(idToDel);
		for(int i = 0; i < docArrList.size(); ++i){
			docArrList.get(i).setField("hidden", "true");
		}
		this.update(docArrList);
		
		System.out.println("deleteById: " + String.valueOf(new Date().getTime() - start));
	}
	
	
	/**
	  * 刪除路徑下所有的 solr document，將 hidden 設為 true
	  * @param path
	  */
	public void deleteByPath(String path){
		
		long start = new Date().getTime();
		
		ArrayList<SolrInputDocument> docArrList = this.getSolrDocByPath(path + "*", "false");
		for(int i = 0; i < docArrList.size(); ++i){
			docArrList.get(i).setField("hidden", "true");
		}
		this.update(docArrList);
		
		System.out.println("deleteByPath: " + String.valueOf(new Date().getTime() - start));
	}
	
	/**
	  * 將oldPath下所有solr document的path改成 newPath
	  * @param oldPath
	  * @param newPath
	  */
	public void renamePath(String oldPath, String newPath){
		
		long start = new Date().getTime();
		
		ArrayList<SolrInputDocument> docArrList = this.getSolrDocByPath(oldPath + "*", "*");
		for(int i = 0; i < docArrList.size(); ++i){
			String path = docArrList.get(i).getFieldValue("path").toString();
			docArrList.get(i).setField("path", path.replace(oldPath, newPath));
		}
		this.update(docArrList);
		
		System.out.println("renamePath: " + String.valueOf(new Date().getTime() - start));

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
	        for(int i = 1; i <= nHier; ++i){
	        	header.createCell(i-1).setCellValue("第" + i + "層");
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
	

}
